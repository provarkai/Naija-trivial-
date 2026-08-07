extends Control
## Main — temporary all-in-one screen.
##
## This is a functional vertical slice (Home -> Gameplay -> Results) built
## entirely in code so the scaffold is playable/testable before dedicated
## scenes exist for each screen. As the project grows, split each `_build_*`
## section below into its own scene under scenes/ (Home.tscn,
## Gameplay.tscn, Results.tscn) driven by a scene-switching AutoLoad —
## the GameStateManager/QuestionBank/SaveManager calls stay the same either
## way, only where the UI lives changes.

const CORRECT_COLOR := Color(0.30, 0.75, 0.35)
const WRONG_COLOR := Color(0.85, 0.25, 0.25)
const NEUTRAL_COLOR := Color(1, 1, 1)
const NEXT_QUESTION_DELAY := 1.1

var home_screen: VBoxContainer
var gameplay_screen: VBoxContainer
var results_screen: VBoxContainer

var score_label: Label
var streak_label: Label
var progress_label: Label
var timer_bar: ProgressBar
var question_label: Label
var option_buttons: Array = []
var status_label: Label

var results_title_label: Label
var results_score_label: Label
var results_streak_label: Label
var results_extra_label: Label

var _current_category: String = ""
var _accepting_input := false


func _ready() -> void:
	set_anchors_preset(Control.PRESET_FULL_RECT)

	_build_background()
	_build_home_screen()
	_build_gameplay_screen()
	_build_results_screen()

	GameStateManager.question_changed.connect(_on_question_changed)
	GameStateManager.timer_tick.connect(_on_timer_tick)
	GameStateManager.answer_submitted.connect(_on_answer_submitted)
	GameStateManager.round_completed.connect(_on_round_completed)

	_show_home()


# ---------------------------------------------------------------------------
# Background
# ---------------------------------------------------------------------------

func _build_background() -> void:
	var bg := ColorRect.new()
	bg.set_anchors_preset(Control.PRESET_FULL_RECT)
	bg.color = Color(0.043, 0.180, 0.110) # deep green
	add_child(bg)


# ---------------------------------------------------------------------------
# Home screen
# ---------------------------------------------------------------------------

func _build_home_screen() -> void:
	home_screen = _full_rect_vbox(24)

	var title := Label.new()
	title.text = "Naija Trivia Blitz"
	title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	title.add_theme_font_size_override("font_size", 42)
	home_screen.add_child(title)

	var subtitle := Label.new()
	subtitle.text = "Pick a category to start a round"
	subtitle.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	home_screen.add_child(subtitle)

	status_label = Label.new()
	status_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	status_label.modulate = Color(1, 0.85, 0.3)
	home_screen.add_child(status_label)

	var grid := GridContainer.new()
	grid.columns = 2
	grid.add_theme_constant_override("h_separation", 16)
	grid.add_theme_constant_override("v_separation", 16)
	home_screen.add_child(grid)

	for category in QuestionBank.categories:
		var category_id: String = category.get("id", "")
		var button := Button.new()
		button.custom_minimum_size = Vector2(220, 80)
		button.text = _category_button_text(category)
		button.pressed.connect(_on_category_pressed.bind(category_id))
		grid.add_child(button)


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
	_current_category = category_id
	GameStateManager.start_round(category_id, round_questions)
	_show_gameplay()


# ---------------------------------------------------------------------------
# Gameplay screen
# ---------------------------------------------------------------------------

func _build_gameplay_screen() -> void:
	gameplay_screen = _full_rect_vbox(16)
	gameplay_screen.visible = false

	var hud := HBoxContainer.new()
	hud.add_theme_constant_override("separation", 24)
	gameplay_screen.add_child(hud)

	score_label = Label.new()
	hud.add_child(score_label)

	streak_label = Label.new()
	hud.add_child(streak_label)

	progress_label = Label.new()
	hud.add_child(progress_label)

	timer_bar = ProgressBar.new()
	timer_bar.custom_minimum_size = Vector2(0, 24)
	timer_bar.show_percentage = false
	gameplay_screen.add_child(timer_bar)

	question_label = Label.new()
	question_label.autowrap_mode = TextServer.AUTOWRAP_WORD
	question_label.add_theme_font_size_override("font_size", 28)
	question_label.custom_minimum_size = Vector2(0, 120)
	gameplay_screen.add_child(question_label)

	var options_container := VBoxContainer.new()
	options_container.add_theme_constant_override("separation", 12)
	gameplay_screen.add_child(options_container)

	option_buttons.clear()
	for i in range(4):
		var button := Button.new()
		button.custom_minimum_size = Vector2(0, 64)
		button.pressed.connect(_on_option_pressed.bind(i))
		options_container.add_child(button)
		option_buttons.append(button)


func _on_question_changed(question: Dictionary, index: int, total: int) -> void:
	question_label.text = question.get("question", "")
	var options: Array = question.get("options", [])
	for i in range(option_buttons.size()):
		var button: Button = option_buttons[i]
		button.text = options[i] if i < options.size() else ""
		button.disabled = false
		button.modulate = NEUTRAL_COLOR

	progress_label.text = "Q %d / %d" % [index + 1, total]
	score_label.text = "Score: %d" % GameStateManager.score
	streak_label.text = "Streak: %d" % GameStateManager.streak
	_accepting_input = true


func _on_timer_tick(time_remaining: float, time_total: float) -> void:
	timer_bar.max_value = time_total
	timer_bar.value = time_remaining


func _on_option_pressed(index: int) -> void:
	if not _accepting_input:
		return
	_accepting_input = false
	GameStateManager.submit_answer(index)


func _on_answer_submitted(result: Dictionary) -> void:
	var selected: int = result.get("selected_index", -1)
	var correct_index: int = result.get("correct_index", -1)

	for i in range(option_buttons.size()):
		var button: Button = option_buttons[i]
		button.disabled = true
		if i == correct_index:
			button.modulate = CORRECT_COLOR
		elif i == selected:
			button.modulate = WRONG_COLOR

	score_label.text = "Score: %d" % result.get("score", GameStateManager.score)
	streak_label.text = "Streak: %d" % result.get("streak", GameStateManager.streak)

	await get_tree().create_timer(NEXT_QUESTION_DELAY).timeout
	GameStateManager.next_question()


# ---------------------------------------------------------------------------
# Results screen
# ---------------------------------------------------------------------------

func _build_results_screen() -> void:
	results_screen = _full_rect_vbox(20)
	results_screen.visible = false

	results_title_label = Label.new()
	results_title_label.text = "Round Complete!"
	results_title_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	results_title_label.add_theme_font_size_override("font_size", 36)
	results_screen.add_child(results_title_label)

	results_score_label = Label.new()
	results_score_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	results_screen.add_child(results_score_label)

	results_streak_label = Label.new()
	results_streak_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	results_screen.add_child(results_streak_label)

	results_extra_label = Label.new()
	results_extra_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	results_extra_label.modulate = Color(1, 0.85, 0.3)
	results_screen.add_child(results_extra_label)

	var play_again := Button.new()
	play_again.text = "Play Again"
	play_again.custom_minimum_size = Vector2(0, 64)
	play_again.pressed.connect(_on_play_again_pressed)
	results_screen.add_child(play_again)

	var back_home := Button.new()
	back_home.text = "Back to Categories"
	back_home.custom_minimum_size = Vector2(0, 64)
	back_home.pressed.connect(_show_home)
	results_screen.add_child(back_home)


func _on_round_completed(summary: Dictionary) -> void:
	var category: String = summary.get("category", "")
	var score: int = summary.get("score", 0)
	var is_new_high := SaveManager.report_score(category, score)

	if summary.get("daily_challenge", false):
		SaveManager.record_daily_challenge_completion()

	results_score_label.text = "Score: %d / %d correct" % [summary.get("correct_count", 0), summary.get("total_questions", 0)]
	results_streak_label.text = "Best streak: %d" % summary.get("longest_streak", 0)
	results_extra_label.text = "New high score!" if is_new_high else ""

	_show_results()


func _on_play_again_pressed() -> void:
	if _current_category.is_empty():
		_show_home()
		return
	_on_category_pressed(_current_category)


# ---------------------------------------------------------------------------
# Screen switching
# ---------------------------------------------------------------------------

func _show_home() -> void:
	_refresh_category_grid()
	home_screen.visible = true
	gameplay_screen.visible = false
	results_screen.visible = false


## Re-applies lock icons/labels in case a purchase or category unlock
## happened since the grid was built (e.g. returning from the Store).
func _refresh_category_grid() -> void:
	var grid := home_screen.get_child(3) as GridContainer
	var buttons := grid.get_children()
	for i in range(min(buttons.size(), QuestionBank.categories.size())):
		var button: Button = buttons[i]
		button.text = _category_button_text(QuestionBank.categories[i])


func _show_gameplay() -> void:
	home_screen.visible = false
	gameplay_screen.visible = true
	results_screen.visible = false


func _show_results() -> void:
	home_screen.visible = false
	gameplay_screen.visible = false
	results_screen.visible = true


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

func _full_rect_vbox(separation: int) -> VBoxContainer:
	var vbox := VBoxContainer.new()
	vbox.set_anchors_preset(Control.PRESET_FULL_RECT)
	vbox.add_theme_constant_override("separation", separation)
	vbox.offset_left = 40
	vbox.offset_right = -40
	vbox.offset_top = 80
	vbox.offset_bottom = -40
	vbox.alignment = BoxContainer.ALIGNMENT_BEGIN
	add_child(vbox)
	return vbox
