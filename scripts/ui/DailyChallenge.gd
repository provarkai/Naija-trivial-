extends Control
## Daily Challenge Screen — same 10 questions for every player on a given
## day (QuestionBank.get_daily_challenge_questions() seeds off the date),
## with streak info pulled from SaveManager.
##
## Leaderboard entry (per the brief's screen list) depends on a
## local/regional ranking backend that doesn't exist yet — this screen
## only shows the player's own streak for now.

var streak_label: Label
var status_label: Label
var play_button: Button


func _ready() -> void:
	AdManager.hide_banner()

	set_anchors_preset(Control.PRESET_FULL_RECT)
	UIHelpers.add_background(self)

	var vbox := UIHelpers.add_screen_vbox(self, 20)

	UIHelpers.add_title(vbox, "Daily Challenge", 36)
	UIHelpers.add_label(vbox, "10 questions, same for everyone today.")

	streak_label = UIHelpers.add_label(vbox)

	status_label = UIHelpers.add_label(vbox)
	status_label.modulate = Color(1, 0.85, 0.3)

	UIHelpers.add_spacer(vbox)

	play_button = UIHelpers.add_button(vbox, "Play Today's Challenge", 72)
	play_button.pressed.connect(_on_play_pressed)

	var back_button := UIHelpers.add_button(vbox, "Back Home", 56)
	back_button.pressed.connect(func(): SceneManager.goto_scene(SceneManager.HOME))

	_refresh()


func _refresh() -> void:
	var dc: Dictionary = SaveManager.data.get("daily_challenge", {})
	var current: int = dc.get("current_streak_days", 0)
	var longest: int = dc.get("longest_streak_days", 0)
	streak_label.text = "Current streak: %d days   •   Best: %d days" % [current, longest]

	if SaveManager.has_played_daily_today():
		status_label.text = "You've already played today — playing again won't change your streak, but you can still beat your score."
	else:
		status_label.text = ""


func _on_play_pressed() -> void:
	var round_questions := QuestionBank.get_daily_challenge_questions(GameStateManager.QUESTIONS_PER_ROUND)
	if round_questions.is_empty():
		status_label.text = "No questions available for today's challenge yet."
		return

	GameStateManager.start_round("Daily Challenge", round_questions, {"daily_challenge": true})
	SceneManager.goto_scene(SceneManager.GAMEPLAY)
