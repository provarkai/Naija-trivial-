extends Control
## Round Results Screen — score summary, streak achieved, and options to
## play again or head back to categories.
##
## "Watch ad to continue" isn't wired up here yet — it depends on the
## AdMob rewarded-video integration (build order item 8). GameStateManager
## already exposes the hook it would call (grant_extra_time/revive); this
## screen just doesn't have a rewarded-ad button to trigger it from yet.
## "Share score" is likewise deferred to the UI-polish pass.

var title_label: Label
var score_label: Label
var streak_label: Label
var extra_label: Label

var _summary: Dictionary


func _ready() -> void:
	# Duplicated rather than referenced directly — this screen annotates the
	# summary with a local-only "_is_new_high_score" flag below, and
	# GameStateManager's cached copy shouldn't pick up UI-layer fields.
	_summary = GameStateManager.last_round_summary.duplicate()
	if _summary.is_empty():
		SceneManager.goto_scene(SceneManager.HOME)
		return

	_apply_save_updates()

	set_anchors_preset(Control.PRESET_FULL_RECT)
	UIHelpers.add_background(self)

	var vbox := UIHelpers.add_screen_vbox(self, 20)

	title_label = UIHelpers.add_title(vbox, "Round Complete!", 36)

	var category_id: String = _summary.get("category", "")
	var category_name := "Daily Challenge" if _summary.get("daily_challenge", false) else QuestionBank.get_category_display_name(category_id)
	UIHelpers.add_label(vbox, category_name)

	score_label = UIHelpers.add_label(vbox)
	score_label.text = "%d / %d correct — %d points" % [
		_summary.get("correct_count", 0),
		_summary.get("total_questions", 0),
		_summary.get("score", 0),
	]

	streak_label = UIHelpers.add_label(vbox)
	streak_label.text = "Best streak: %d" % _summary.get("longest_streak", 0)

	extra_label = UIHelpers.add_label(vbox)
	extra_label.modulate = Color(1, 0.85, 0.3)
	extra_label.text = "New high score!" if _summary.get("_is_new_high_score", false) else ""

	UIHelpers.add_spacer(vbox)

	var play_again := UIHelpers.add_button(vbox, "Play Again", 64)
	play_again.pressed.connect(_on_play_again_pressed)

	var back_home := UIHelpers.add_button(vbox, "Back to Categories", 64)
	back_home.pressed.connect(func(): SceneManager.goto_scene(SceneManager.HOME))


## Persists the score/streak-day side effects of finishing a round. Done
## once, here, right as the round's summary is consumed — Gameplay only
## emits/forwards the summary, it doesn't know what to do with it.
func _apply_save_updates() -> void:
	var category_id: String = _summary.get("category", "")
	var score: int = _summary.get("score", 0)
	_summary["_is_new_high_score"] = SaveManager.report_score(category_id, score)

	if _summary.get("daily_challenge", false):
		SaveManager.record_daily_challenge_completion()


func _on_play_again_pressed() -> void:
	var category_id: String = _summary.get("category", "")
	var is_daily: bool = _summary.get("daily_challenge", false)

	var round_questions: Array
	if is_daily:
		round_questions = QuestionBank.get_daily_challenge_questions(GameStateManager.QUESTIONS_PER_ROUND)
	else:
		round_questions = QuestionBank.get_round_questions(category_id, GameStateManager.QUESTIONS_PER_ROUND)

	if round_questions.is_empty():
		SceneManager.goto_scene(SceneManager.HOME)
		return

	GameStateManager.start_round(category_id, round_questions, {"daily_challenge": is_daily})
	SceneManager.goto_scene(SceneManager.GAMEPLAY)
