extends Control
## Home / Main Menu — category selection grid, daily challenge entry,
## settings, and the "remove ads" upsell banner.
##
## Every screen script in this project builds its Control tree in code
## rather than in a hand-authored .tscn: the project has no Godot editor
## in the loop yet to visually build/verify scenes, and a procedural tree
## via UIHelpers is much less error-prone to write and review by hand than
## a large block of raw .tscn node/anchor syntax. Swap any screen to a
## real editor-built scene later — nothing about GameStateManager /
## QuestionBank / SaveManager / SceneManager needs to change for that.

var status_label: Label
var daily_challenge_button: Button
var category_grid: GridContainer
var ads_upsell_button: Button


func _ready() -> void:
	AdManager.show_banner() # persistent on Home only, per the monetization brief

	set_anchors_preset(Control.PRESET_FULL_RECT)
	UIHelpers.add_background(self)

	var vbox := UIHelpers.add_screen_vbox(self, 20)

	UIHelpers.add_title(vbox, "Naija Trivia Blitz", 42)
	UIHelpers.add_label(vbox, "Pick a category to start a round")

	status_label = UIHelpers.add_label(vbox)
	status_label.modulate = Color(1, 0.85, 0.3)

	daily_challenge_button = UIHelpers.add_button(vbox, "Daily Challenge", 72)
	daily_challenge_button.pressed.connect(_on_daily_challenge_pressed)

	category_grid = GridContainer.new()
	category_grid.columns = 2
	category_grid.add_theme_constant_override("h_separation", 16)
	category_grid.add_theme_constant_override("v_separation", 16)
	vbox.add_child(category_grid)
	_build_category_buttons()

	UIHelpers.add_spacer(vbox)

	ads_upsell_button = UIHelpers.add_button(vbox, "Remove Ads — one-time purchase", 56)
	ads_upsell_button.pressed.connect(func(): SceneManager.goto_scene(SceneManager.STORE, true))

	var bottom_row := HBoxContainer.new()
	bottom_row.add_theme_constant_override("separation", 16)
	vbox.add_child(bottom_row)

	var store_button := UIHelpers.add_button(bottom_row, "Store", 56)
	store_button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	store_button.pressed.connect(func(): SceneManager.goto_scene(SceneManager.STORE, true))

	var settings_button := UIHelpers.add_button(bottom_row, "Settings", 56)
	settings_button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	settings_button.pressed.connect(func(): SceneManager.goto_scene(SceneManager.SETTINGS, true))

	# change_scene_to_file() rebuilds this scene from scratch every time the
	# player returns to it, so a plain _ready()-time refresh is enough to
	# pick up anything that changed elsewhere (a Store purchase, a new
	# streak day) — no visibility-change plumbing needed.
	_refresh()


## Re-applies lock icons/labels and the daily-streak label, and hides the
## ads banner if it's already been purchased.
func _refresh() -> void:
	ads_upsell_button.visible = not SaveManager.has_ads_removed()

	var streak: int = SaveManager.get_effective_daily_streak()
	if streak > 0:
		daily_challenge_button.text = "Daily Challenge 🔥 %d day streak" % streak
	else:
		daily_challenge_button.text = "Daily Challenge"

	for button in category_grid.get_children():
		var category_id: String = button.get_meta("category_id")
		button.text = _category_button_text(_category_by_id(category_id))


func _build_category_buttons() -> void:
	for category in QuestionBank.categories:
		var category_id: String = category.get("id", "")
		var button := UIHelpers.add_button(category_grid, _category_button_text(category), 80)
		button.custom_minimum_size = Vector2(220, 80)
		button.set_meta("category_id", category_id)
		button.pressed.connect(_on_category_pressed.bind(category_id))


func _category_by_id(category_id: String) -> Dictionary:
	for category in QuestionBank.categories:
		if category.get("id", "") == category_id:
			return category
	return {}


func _category_button_text(category: Dictionary) -> String:
	var label: String = category.get("display_name", category.get("id", "?"))
	var unlocked := SaveManager.is_category_unlocked(category.get("id", ""))
	return label if unlocked else "%s 🔒" % label


func _on_category_pressed(category_id: String) -> void:
	if not SaveManager.is_category_unlocked(category_id):
		status_label.text = "That category is a premium unlock — visit the Store."
		return

	var round_questions := QuestionBank.get_round_questions(category_id, GameStateManager.QUESTIONS_PER_ROUND)
	if round_questions.is_empty():
		status_label.text = "No questions loaded for this category yet."
		return

	status_label.text = ""
	GameStateManager.start_round(category_id, round_questions)
	SceneManager.goto_scene(SceneManager.GAMEPLAY)


func _on_daily_challenge_pressed() -> void:
	SceneManager.goto_scene(SceneManager.DAILY_CHALLENGE)
