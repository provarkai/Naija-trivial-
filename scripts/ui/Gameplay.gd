extends Control
## Gameplay Screen — question text, 4 answer buttons, countdown timer bar,
## score/streak display, progress ("question 1 of 10").
##
## This screen never starts a round itself — GameStateManager.start_round()
## must already have been called by whoever navigated here (Home or Daily
## Challenge), so on _ready() this just renders whatever round is already
## in progress and reacts to GameStateManager's signals from then on.

const CORRECT_COLOR := Color(0.30, 0.75, 0.35)
const WRONG_COLOR := Color(0.85, 0.25, 0.25)
const NEUTRAL_COLOR := Color(1, 1, 1)
const NEXT_QUESTION_DELAY := 1.1

var score_label: Label
var streak_label: Label
var progress_label: Label
var timer_bar: ProgressBar
var question_label: Label
var option_buttons: Array = []

var _accepting_input := false


func _ready() -> void:
	if GameStateManager.get_current_question().is_empty():
		# Reached directly (e.g. scene run standalone in the editor) with no
		# round in progress — bail back to Home rather than showing a blank
		# gameplay screen.
		SceneManager.goto_scene(SceneManager.HOME)
		return

	set_anchors_preset(Control.PRESET_FULL_RECT)
	UIHelpers.add_background(self)

	var vbox := UIHelpers.add_screen_vbox(self, 16)

	var hud := HBoxContainer.new()
	hud.add_theme_constant_override("separation", 24)
	vbox.add_child(hud)
	score_label = UIHelpers.add_label(hud, "", false)
	streak_label = UIHelpers.add_label(hud, "", false)
	progress_label = UIHelpers.add_label(hud, "", false)

	timer_bar = ProgressBar.new()
	timer_bar.custom_minimum_size = Vector2(0, 24)
	timer_bar.show_percentage = false
	vbox.add_child(timer_bar)

	question_label = Label.new()
	question_label.autowrap_mode = TextServer.AUTOWRAP_WORD
	question_label.add_theme_font_size_override("font_size", 28)
	question_label.custom_minimum_size = Vector2(0, 120)
	vbox.add_child(question_label)

	var options_container := VBoxContainer.new()
	options_container.add_theme_constant_override("separation", 12)
	vbox.add_child(options_container)

	option_buttons.clear()
	for i in range(4):
		var button := UIHelpers.add_button(options_container, "", 64)
		button.pressed.connect(_on_option_pressed.bind(i))
		option_buttons.append(button)

	GameStateManager.question_changed.connect(_on_question_changed)
	GameStateManager.timer_tick.connect(_on_timer_tick)
	GameStateManager.answer_submitted.connect(_on_answer_submitted)
	GameStateManager.round_completed.connect(_on_round_completed)

	# The round (and its first question) already started before this scene
	# was loaded, so render the current question immediately instead of
	# waiting for a question_changed signal that already fired.
	_render_question(
		GameStateManager.get_current_question(),
		GameStateManager.current_question_index,
		GameStateManager.questions.size()
	)


func _on_question_changed(question: Dictionary, index: int, total: int) -> void:
	_render_question(question, index, total)


func _render_question(question: Dictionary, index: int, total: int) -> void:
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


func _on_round_completed(_summary: Dictionary) -> void:
	SceneManager.goto_scene(SceneManager.RESULTS)
