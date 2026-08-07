extends Control
## Store/IAP Screen — remove ads, premium category packs, pro bundle,
## restore purchases.
##
## Google Play Billing isn't wired up yet (build order item 9), so the
## purchase buttons here are dev-only stand-ins: pressing one flips the
## matching SaveManager entitlement directly instead of going through a
## real purchase flow. This is enough to test that unlocks propagate
## correctly (Home's category grid, ad visibility) while the actual
## Play Billing plugin/product IDs get set up. Every dev-purchase button
## says so on the label so it's never mistaken for a real transaction.

const PREMIUM_CATEGORY_IDS := ["nigerian_history", "sports", "pidgin_proverbs"]

var status_label: Label
var remove_ads_button: Button
var pro_bundle_button: Button
var pack_buttons: Dictionary = {} # category_id -> Button


func _ready() -> void:
	AdManager.hide_banner()

	set_anchors_preset(Control.PRESET_FULL_RECT)
	UIHelpers.add_background(self)

	var vbox := UIHelpers.add_screen_vbox(self, 16)

	UIHelpers.add_title(vbox, "Store", 36)
	UIHelpers.add_label(vbox, "Google Play Billing isn't connected yet — these buttons apply the unlock locally for testing.")

	status_label = UIHelpers.add_label(vbox)
	status_label.modulate = Color(1, 0.85, 0.3)

	UIHelpers.add_spacer(vbox, 8)

	remove_ads_button = UIHelpers.add_button(vbox, "", 64)
	remove_ads_button.pressed.connect(_on_remove_ads_pressed)

	for category_id in PREMIUM_CATEGORY_IDS:
		var button := UIHelpers.add_button(vbox, "", 64)
		button.pressed.connect(_on_pack_pressed.bind(category_id))
		pack_buttons[category_id] = button

	pro_bundle_button = UIHelpers.add_button(vbox, "", 64)
	pro_bundle_button.pressed.connect(_on_pro_bundle_pressed)

	UIHelpers.add_spacer(vbox, 8)

	var restore_button := UIHelpers.add_button(vbox, "Restore Purchases", 56)
	restore_button.pressed.connect(_on_restore_pressed)

	var back_button := UIHelpers.add_button(vbox, "Back", 56)
	back_button.pressed.connect(func(): SceneManager.go_back())

	_refresh()


func _refresh() -> void:
	var ads_removed := SaveManager.has_ads_removed()
	remove_ads_button.text = "Remove Ads — ✓ Owned" if ads_removed else "Remove Ads — ₦500-₦1,000 (dev unlock)"
	remove_ads_button.disabled = ads_removed

	var pro_owned: bool = SaveManager.data.get("purchases", {}).get("pro_bundle", false)
	pro_bundle_button.text = "Pro Bundle (all categories, no ads) — ✓ Owned" if pro_owned else "Pro Bundle — ₦1,500 (dev unlock)"
	pro_bundle_button.disabled = pro_owned

	for category_id in PREMIUM_CATEGORY_IDS:
		var button: Button = pack_buttons[category_id]
		var display_name := QuestionBank.get_category_display_name(category_id)
		var owned := SaveManager.is_category_unlocked(category_id)
		button.text = "%s Pack — ✓ Owned" % display_name if owned else "%s Pack — ₦300-₦500 (dev unlock)" % display_name
		button.disabled = owned


func _on_remove_ads_pressed() -> void:
	SaveManager.set_ads_removed(true)
	status_label.text = "Ads removed (dev unlock applied)."
	_refresh()


func _on_pack_pressed(category_id: String) -> void:
	SaveManager.unlock_category_pack(category_id)
	status_label.text = "%s unlocked (dev unlock applied)." % QuestionBank.get_category_display_name(category_id)
	_refresh()


func _on_pro_bundle_pressed() -> void:
	SaveManager.set_pro_bundle_owned(true)
	status_label.text = "Pro Bundle unlocked (dev unlock applied)."
	_refresh()


func _on_restore_pressed() -> void:
	# Nothing to restore yet without a real billing backend — this just
	# confirms the current entitlement state so the button isn't dead.
	status_label.text = "Purchases are stored locally for now — nothing to restore from Google Play yet."
