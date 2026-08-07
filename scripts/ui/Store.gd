extends Control
## Store/IAP Screen — remove ads, premium category packs, pro bundle,
## restore purchases.
##
## Every purchase button goes through IAPManager, which either launches a
## real Google Play Billing flow or — if the plugin isn't installed —
## simulates a successful purchase after a short delay, so this screen is
## fully testable from the editor without a device. See
## docs/PLAY_BILLING_SETUP.md.

var status_label: Label
var remove_ads_button: Button
var pro_bundle_button: Button
var pack_buttons: Dictionary = {} # category_id -> Button
var _purchase_buttons: Array = []
var _premium_category_ids: Array = []


func _ready() -> void:
	AdManager.hide_banner()

	_premium_category_ids = QuestionBank.categories \
		.filter(func(c): return c.get("premium", false)) \
		.map(func(c): return c.get("id", ""))

	set_anchors_preset(Control.PRESET_FULL_RECT)
	UIHelpers.add_background(self)

	var vbox := UIHelpers.add_screen_vbox(self, 16)

	UIHelpers.add_title(vbox, "Store", 36)

	status_label = UIHelpers.add_label(vbox)
	status_label.modulate = Color(1, 0.85, 0.3)
	if not IAPManager.is_plugin_available():
		status_label.text = "Google Play Billing isn't connected — purchases apply locally for testing."

	UIHelpers.add_spacer(vbox, 8)

	remove_ads_button = UIHelpers.add_button(vbox, "", 64)
	remove_ads_button.pressed.connect(_on_purchase_pressed.bind(IAPManager.PRODUCT_REMOVE_ADS))
	_purchase_buttons.append(remove_ads_button)

	for category_id in _premium_category_ids:
		var button := UIHelpers.add_button(vbox, "", 64)
		var product_id := IAPManager.category_pack_product_id(category_id)
		button.icon = UIHelpers.load_category_icon(_category_by_id(category_id))
		button.pressed.connect(_on_purchase_pressed.bind(product_id))
		pack_buttons[category_id] = button
		_purchase_buttons.append(button)

	pro_bundle_button = UIHelpers.add_button(vbox, "", 64)
	pro_bundle_button.pressed.connect(_on_purchase_pressed.bind(IAPManager.PRODUCT_PRO_BUNDLE))
	_purchase_buttons.append(pro_bundle_button)

	UIHelpers.add_spacer(vbox, 8)

	var restore_button := UIHelpers.add_button(vbox, "Restore Purchases", 56)
	restore_button.pressed.connect(_on_restore_pressed)

	var back_button := UIHelpers.add_button(vbox, "Back", 56)
	back_button.pressed.connect(func(): SceneManager.go_back())

	IAPManager.purchase_completed.connect(_on_purchase_completed)
	IAPManager.purchase_failed.connect(_on_purchase_failed)
	IAPManager.restore_completed.connect(_on_restore_completed)
	IAPManager.prices_updated.connect(_refresh)

	_refresh()


func _category_by_id(category_id: String) -> Dictionary:
	for category in QuestionBank.categories:
		if category.get("id", "") == category_id:
			return category
	return {}


func _refresh() -> void:
	var ads_removed := SaveManager.has_ads_removed()
	remove_ads_button.text = "Remove Ads — ✓ Owned" if ads_removed else "Remove Ads — %s" % IAPManager.get_price_string(IAPManager.PRODUCT_REMOVE_ADS)
	remove_ads_button.disabled = ads_removed

	var pro_owned: bool = SaveManager.data.get("purchases", {}).get("pro_bundle", false)
	pro_bundle_button.text = "Pro Bundle (all categories, no ads) — ✓ Owned" if pro_owned else "Pro Bundle — %s" % IAPManager.get_price_string(IAPManager.PRODUCT_PRO_BUNDLE)
	pro_bundle_button.disabled = pro_owned

	for category_id in _premium_category_ids:
		var button: Button = pack_buttons[category_id]
		var display_name := QuestionBank.get_category_display_name(category_id)
		var owned := SaveManager.is_category_unlocked(category_id)
		var price := IAPManager.get_price_string(IAPManager.category_pack_product_id(category_id))
		button.text = "%s Pack — ✓ Owned" % display_name if owned else "%s Pack — %s" % [display_name, price]
		button.disabled = owned


func _on_purchase_pressed(product_id: String) -> void:
	_set_purchase_buttons_disabled(true)
	status_label.text = "Processing purchase…"
	IAPManager.purchase(product_id)


func _set_purchase_buttons_disabled(disabled: bool) -> void:
	for button in _purchase_buttons:
		button.disabled = disabled


func _on_purchase_completed(product_id: String) -> void:
	status_label.text = "%s unlocked!" % _product_label(product_id)
	_refresh() # re-derives each button's disabled state from ownership


func _on_purchase_failed(_product_id: String, message: String) -> void:
	status_label.text = "Purchase failed: %s" % message
	_refresh()


func _product_label(product_id: String) -> String:
	if product_id == IAPManager.PRODUCT_REMOVE_ADS:
		return "Remove Ads"
	if product_id == IAPManager.PRODUCT_PRO_BUNDLE:
		return "Pro Bundle"
	for category_id in _premium_category_ids:
		if IAPManager.category_pack_product_id(category_id) == product_id:
			return "%s Pack" % QuestionBank.get_category_display_name(category_id)
	return product_id


func _on_restore_pressed() -> void:
	status_label.text = "Restoring purchases…"
	IAPManager.restore_purchases()


func _on_restore_completed(found_purchases: bool) -> void:
	if IAPManager.is_plugin_available():
		status_label.text = "Purchases restored." if found_purchases else "No previous purchases found."
	else:
		status_label.text = "Purchases are stored locally for now — nothing to restore from Google Play without Billing connected."
	_refresh()
