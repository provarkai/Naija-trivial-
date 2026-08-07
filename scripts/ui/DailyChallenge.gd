extends Control
## Daily Challenge Screen — same 10 questions for every player on a given
## day (QuestionBank.get_daily_challenge_questions() seeds off the date),
## with streak info pulled from SaveManager and a link to today's
## leaderboard (LeaderboardManager — see docs/LEADERBOARD_SETUP.md for
## what's needed to actually populate it).

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

	var leaderboard_button := UIHelpers.add_button(vbox, "View Today's Leaderboard", 56)
	leaderboard_button.pressed.connect(func(): SceneManager.goto_scene(SceneManager.LEADERBOARD, true))

	var back_button := UIHelpers.add_button(vbox, "Back Home", 56)
	back_button.pressed.connect(func(): SceneManager.goto_scene(SceneManager.HOME))

	_refresh()


func _refresh() -> void:
	var dc: Dictionary = SaveManager.data.get("daily_challenge", {})
	var effective_streak := SaveManager.get_effective_daily_streak()
	var longest: int = dc.get("longest_streak_days", 0)
	streak_label.text = "Current streak: %d days   •   Best: %d days" % [effective_streak, longest]

	if SaveManager.has_played_daily_today():
		status_label.text = "You've already played today — playing again won't change your streak, but you can still beat your score."
	elif effective_streak > 0:
		status_label.text = "🔥 Play today to keep your %d-day streak alive!" % effective_streak
	elif dc.get("current_streak_days", 0) > 0:
		# current_streak_days is still stale (nonzero) but
		# get_effective_daily_streak() says it's already lapsed.
		status_label.text = "You lost your streak — start a new one today."
	else:
		status_label.text = ""


func _on_play_pressed() -> void:
	var round_questions := QuestionBank.get_daily_challenge_questions(GameStateManager.QUESTIONS_PER_ROUND)
	if round_questions.is_empty():
		status_label.text = "No questions available for today's challenge yet."
		return

	GameStateManager.start_round("Daily Challenge", round_questions, {"daily_challenge": true})
	SceneManager.goto_scene(SceneManager.GAMEPLAY)
