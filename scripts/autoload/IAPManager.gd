extends Node
## IAPManager (autoload singleton)
##
## Thin wrapper around a native Google Play Billing plugin, mirroring how
## AdManager wraps AdMob: the rest of the game calls purchase(product_id),
## restore_purchases(), and get_price_string(product_id), and never touches
## the plugin or SaveManager's purchase fields directly.
##
## WHICH PLUGIN THIS TARGETS
## This wraps the "GodotGooglePlayBilling" native singleton exposed by the
## official Godot Play Billing plugin
## (https://github.com/godotengine/godot-play-billing). It is NOT
## installed by this scaffold: install it yourself (AssetLib or manual
## addon), enable it under Project Settings > Plugins, and configure your
## Android export preset per its README (Gradle build enabled, Play
## Billing export plugin enabled). See docs/PLAY_BILLING_SETUP.md.
##
## As with AdManager, every plugin call/signal connection goes through
## NativePluginBridge, which checks has_method()/has_signal() first and
## logs + no-ops instead of crashing when something doesn't match — the
## exact API below is this plugin's documented shape as of writing and
## needs re-checking against whatever version you actually install.
##
## SaveManager remains the single source of truth for what the player
## owns (is_category_unlocked(), has_ads_removed(), etc.) — IAPManager's
## only job is turning a successful purchase into the right SaveManager
## call, never tracking ownership itself. When the plugin isn't present,
## purchase() simulates a successful purchase after a short delay so the
## Store screen is fully testable from the editor without a device.

signal purchase_completed(product_id: String)
signal purchase_failed(product_id: String, message: String)
signal restore_completed(found_purchases: bool)
signal prices_updated

const PLUGIN_SINGLETON_NAME := "GodotGooglePlayBilling"
const PURCHASE_TYPE_INAPP := "inapp"

const PRODUCT_REMOVE_ADS := "remove_ads"
const PRODUCT_PRO_BUNDLE := "pro_bundle"
const CATEGORY_PACK_PREFIX := "pack_"

## product_id -> { type, [category_id], fallback_price }. fallback_price is
## shown until the plugin's queried SKU details give a real localized
## price, and is all the Store screen ever has without a plugin installed.
## Product IDs here must match what you create in Play Console exactly —
## see docs/PLAY_BILLING_SETUP.md.
var PRODUCT_CATALOG: Dictionary = {
	PRODUCT_REMOVE_ADS: {"type": "remove_ads", "fallback_price": "₦500–₦1,000"},
	PRODUCT_PRO_BUNDLE: {"type": "pro_bundle", "fallback_price": "₦1,500"},
}

const SIMULATE_PURCHASES_WITHOUT_PLUGIN := true
const SIMULATED_PURCHASE_DELAY := 1.0

var _plugin: Object = null
var _connected := false
var _sku_prices: Dictionary = {} # product_id -> localized price string
var _last_requested_product := ""


func _ready() -> void:
	# Category packs are data-driven off categories.json rather than
	# hardcoded here, so a new premium category automatically gets a
	# matching IAP product without touching this file.
	for category in QuestionBank.categories:
		if category.get("premium", false):
			var category_id: String = category.get("id", "")
			PRODUCT_CATALOG[category_pack_product_id(category_id)] = {
				"type": "category_pack",
				"category_id": category_id,
				"fallback_price": "₦300–₦500",
			}

	if Engine.has_singleton(PLUGIN_SINGLETON_NAME):
		_plugin = Engine.get_singleton(PLUGIN_SINGLETON_NAME)
		_connect_plugin_signals()
		_call_plugin("startConnection", [])
	else:
		print("IAPManager: '%s' plugin singleton not found — purchases are simulated locally (expected outside an Android export with the plugin installed)." % PLUGIN_SINGLETON_NAME)


func is_plugin_available() -> bool:
	return _plugin != null


func category_pack_product_id(category_id: String) -> String:
	return CATEGORY_PACK_PREFIX + category_id


## Localized price if the plugin has reported one, otherwise the brief's
## Naira price-range as a placeholder.
func get_price_string(product_id: String) -> String:
	if _sku_prices.has(product_id):
		return _sku_prices[product_id]
	return PRODUCT_CATALOG.get(product_id, {}).get("fallback_price", "")


# ---------------------------------------------------------------------------
# Purchase / restore
# ---------------------------------------------------------------------------

func purchase(product_id: String) -> void:
	if not PRODUCT_CATALOG.has(product_id):
		push_error("IAPManager: unknown product id '%s'" % product_id)
		purchase_failed.emit(product_id, "Unknown product")
		return

	if is_plugin_available() and _connected:
		_last_requested_product = product_id
		_call_plugin("purchase", [product_id])
		# The actual result arrives later via the purchases_updated /
		# purchase_error signals, not synchronously from this call.
		return

	if SIMULATE_PURCHASES_WITHOUT_PLUGIN:
		print("IAPManager: simulating purchase of '%s' (no plugin installed)." % product_id)
		await get_tree().create_timer(SIMULATED_PURCHASE_DELAY).timeout
		_grant_entitlement(product_id)
		return

	purchase_failed.emit(product_id, "Billing unavailable")


## Re-queries what the player already owns from Play and re-applies it to
## SaveManager. Without a plugin there's nothing external to query —
## SaveManager already holds whatever was granted locally.
func restore_purchases() -> void:
	if is_plugin_available() and _connected:
		_call_plugin("queryPurchases", [PURCHASE_TYPE_INAPP])
		return
	restore_completed.emit(false)


func _grant_entitlement(product_id: String) -> void:
	var entry: Dictionary = PRODUCT_CATALOG.get(product_id, {})
	if entry.is_empty():
		push_warning("IAPManager: purchase for unknown product id '%s' — nothing to grant." % product_id)
		return

	match entry.get("type", ""):
		"remove_ads":
			SaveManager.set_ads_removed(true)
		"pro_bundle":
			SaveManager.set_pro_bundle_owned(true)
		"category_pack":
			SaveManager.unlock_category_pack(entry.get("category_id", ""))
		_:
			push_warning("IAPManager: product '%s' has an unrecognized catalog type." % product_id)
			return

	purchase_completed.emit(product_id)


func _apply_purchase(purchase_data: Dictionary) -> void:
	# Play Billing v4+ purchases can cover multiple SKUs ("products");
	# older plugin versions report a single "sku" string. Handle both.
	var product_ids: Array = purchase_data.get("products", [])
	if product_ids.is_empty() and purchase_data.has("sku"):
		product_ids = [purchase_data["sku"]]

	for product_id in product_ids:
		_grant_entitlement(product_id)

	# Non-consumables must be acknowledged within 3 days or Play refunds
	# them automatically.
	if not purchase_data.get("is_acknowledged", true):
		var token: String = purchase_data.get("purchase_token", "")
		if not token.is_empty():
			_call_plugin("acknowledgePurchase", [token])


# ---------------------------------------------------------------------------
# Plugin signal wiring
# ---------------------------------------------------------------------------

func _connect_plugin_signals() -> void:
	_safe_connect("connected", _on_connected)
	_safe_connect("disconnected", _on_disconnected)
	_safe_connect("connect_error", _on_connect_error)
	_safe_connect("purchases_updated", _on_purchases_updated)
	_safe_connect("purchase_error", _on_purchase_error)
	_safe_connect("query_purchases_response", _on_query_purchases_response)
	_safe_connect("sku_details_query_completed", _on_sku_details_query_completed)
	_safe_connect("sku_details_query_error", _on_sku_details_query_error)


func _on_connected() -> void:
	_connected = true
	_call_plugin("queryPurchases", [PURCHASE_TYPE_INAPP])
	_call_plugin("querySkuDetails", [PackedStringArray(PRODUCT_CATALOG.keys()), PURCHASE_TYPE_INAPP])


func _on_disconnected() -> void:
	_connected = false


func _on_connect_error(_response_id = null, debug_message = "") -> void:
	_connected = false
	print("IAPManager: billing connection error — %s" % debug_message)


func _on_purchases_updated(purchases = []) -> void:
	for purchase_data in purchases:
		_apply_purchase(purchase_data)


func _on_purchase_error(_response_id = null, debug_message = "") -> void:
	print("IAPManager: purchase error — %s" % debug_message)
	purchase_failed.emit(_last_requested_product, str(debug_message))


func _on_query_purchases_response(purchases = []) -> void:
	for purchase_data in purchases:
		_apply_purchase(purchase_data)
	restore_completed.emit(true)


func _on_sku_details_query_completed(sku_details = []) -> void:
	for detail in sku_details:
		var product_id: String = detail.get("product_id", detail.get("sku", ""))
		var price: String = detail.get("price", "")
		if not product_id.is_empty() and not price.is_empty():
			_sku_prices[product_id] = price
	prices_updated.emit()


func _on_sku_details_query_error(_response_id = null, debug_message = "", _queried_skus = []) -> void:
	print("IAPManager: SKU details query error — %s" % debug_message)


func _safe_connect(signal_name: String, handler: Callable) -> void:
	NativePluginBridge.safe_connect(_plugin, "IAPManager", signal_name, handler)


func _call_plugin(method: String, args: Array):
	return NativePluginBridge.call_plugin(_plugin, "IAPManager", method, args)
