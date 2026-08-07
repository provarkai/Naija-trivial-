extends Control
## Leaderboard Screen — today's Daily Challenge rankings and the player's
## regional rankings, backed by LeaderboardManager (Firestore). Until a
## Firebase project is set up (docs/LEADERBOARD_SETUP.md), this screen
## just reports that the leaderboard isn't connected — there's no local
## fallback for a feature that's inherently about other real players.

var status_label: Label
var list_container: VBoxContainer
var daily_tab_button: Button
var regional_tab_button: Button

var _mode := "daily" # "daily" | "regional"


func _ready() -> void:
	AdManager.hide_banner()

	set_anchors_preset(Control.PRESET_FULL_RECT)
	UIHelpers.add_background(self)

	var vbox := UIHelpers.add_screen_vbox(self, 16)

	UIHelpers.add_title(vbox, "Leaderboard", 36)

	status_label = UIHelpers.add_label(vbox)
	status_label.modulate = Color(1, 0.85, 0.3)

	var tabs := HBoxContainer.new()
	tabs.add_theme_constant_override("separation", 12)
	vbox.add_child(tabs)

	daily_tab_button = UIHelpers.add_button(tabs, "Today", 48)
	daily_tab_button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	daily_tab_button.pressed.connect(_load_daily)

	regional_tab_button = UIHelpers.add_button(tabs, "My Region", 48)
	regional_tab_button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	regional_tab_button.pressed.connect(_load_regional)

	var scroll := ScrollContainer.new()
	scroll.size_flags_vertical = Control.SIZE_EXPAND_FILL
	vbox.add_child(scroll)

	list_container = VBoxContainer.new()
	list_container.add_theme_constant_override("separation", 6)
	list_container.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	scroll.add_child(list_container)

	var back_button := UIHelpers.add_button(vbox, "Back", 56)
	back_button.pressed.connect(func(): SceneManager.go_back())

	LeaderboardManager.scores_fetched.connect(_on_scores_fetched)

	_load_daily()


func _load_daily() -> void:
	_mode = "daily"
	_set_loading("today's Daily Challenge")
	LeaderboardManager.get_daily_leaderboard()


func _load_regional() -> void:
	var region := SaveManager.get_region()
	if region.is_empty():
		_clear_list()
		status_label.text = "Set your region in Settings to see regional rankings."
		return
	_mode = "regional"
	_set_loading(region)
	LeaderboardManager.get_regional_leaderboard(region)


func _set_loading(what: String) -> void:
	_clear_list()
	if not LeaderboardManager.is_available():
		status_label.text = "Leaderboard isn't connected yet — see docs/LEADERBOARD_SETUP.md."
		return
	status_label.text = "Loading %s…" % what


func _clear_list() -> void:
	for child in list_container.get_children():
		child.queue_free()


func _on_scores_fetched(scope: String, entries: Array) -> void:
	var expected_scope := LeaderboardManager.COLLECTION_DAILY if _mode == "daily" else LeaderboardManager.COLLECTION_REGIONAL
	if scope != expected_scope:
		return # a stale response for the tab we've since switched away from

	_clear_list()

	if entries.is_empty():
		if LeaderboardManager.is_available():
			status_label.text = "No scores yet — be the first!"
		# else: leave the "not connected" message from _set_loading() as-is
		return

	status_label.text = ""
	for i in range(entries.size()):
		var entry: Dictionary = entries[i]
		var row := Label.new()
		var player_name: String = entry.get("display_name", "Anonymous Player")
		var score: int = entry.get("score", 0)
		row.text = "%d. %s — %d" % [i + 1, player_name, score]
		list_container.add_child(row)
