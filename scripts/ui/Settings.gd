extends Control
## Settings — sound toggle, restore purchases, privacy policy link.

var sound_toggle: CheckButton
var music_toggle: CheckButton
var status_label: Label


func _ready() -> void:
	AdManager.hide_banner()

	set_anchors_preset(Control.PRESET_FULL_RECT)
	UIHelpers.add_background(self)

	var vbox := UIHelpers.add_screen_vbox(self, 20)

	UIHelpers.add_title(vbox, "Settings", 36)

	sound_toggle = CheckButton.new()
	sound_toggle.text = "Sound effects"
	sound_toggle.button_pressed = SaveManager.data.get("settings", {}).get("sound_enabled", true)
	sound_toggle.toggled.connect(func(pressed): SaveManager.set_sound_enabled(pressed))
	vbox.add_child(sound_toggle)

	music_toggle = CheckButton.new()
	music_toggle.text = "Music"
	music_toggle.button_pressed = SaveManager.data.get("settings", {}).get("music_enabled", true)
	music_toggle.toggled.connect(func(pressed): SaveManager.set_music_enabled(pressed))
	vbox.add_child(music_toggle)

	UIHelpers.add_spacer(vbox)

	status_label = UIHelpers.add_label(vbox)
	status_label.modulate = Color(1, 0.85, 0.3)

	var restore_button := UIHelpers.add_button(vbox, "Restore Purchases", 56)
	restore_button.pressed.connect(_on_restore_pressed)

	var privacy_button := UIHelpers.add_button(vbox, "Privacy Policy", 56)
	privacy_button.pressed.connect(_on_privacy_pressed)

	var back_button := UIHelpers.add_button(vbox, "Back", 56)
	back_button.pressed.connect(func(): SceneManager.go_back())


func _on_restore_pressed() -> void:
	status_label.text = "Purchases are stored locally for now — nothing to restore from Google Play yet."


func _on_privacy_pressed() -> void:
	# TODO: point this at the published privacy policy URL once the app is
	# listed on Play Console (required for the store listing).
	status_label.text = "Privacy policy link isn't configured yet."
