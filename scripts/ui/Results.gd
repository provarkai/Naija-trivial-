extends Control
## Round Results Screen — score summary, streak achieved, and options to
## share the score, play again, or head back to categories.
##
## "Watch ad to continue" isn't wired up here — GameStateManager already
## exposes the hook it would call (grant_extra_time/revive), but there's
## no natural point to offer it once a round is already fully over
## (revive fits mid-round, right after a wrong answer — see Gameplay.gd,
## which is where it's actually wired). Kept as a documented gap rather
## than a half-fit button here.

var title_label: Label
var score_label: Label
var streak_label: Label
var extra_label: Label
var share_button: Button
var share_status_label: Label

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

	SFXManager.play(SFXManager.Sound.LEVEL_UP if _summary.get("_leveled_up_to", 0) > 0 else SFXManager.Sound.ROUND_COMPLETE)

	AdManager.hide_banner()
	# Round is fully over and no question is on screen — the right, and
	# only, moment for an interstitial per the "never mid-question" rule.
	AdManager.notify_round_completed()

	set_anchors_preset(Control.PRESET_FULL_RECT)
	UIHelpers.add_background(self)

	var vbox := UIHelpers.add_screen_vbox(self, 20)

	title_label = UIHelpers.add_title(vbox, "Round Complete!", 36)

	var category_id: String = _summary.get("category", "")
	var category_name := "Daily Challenge" if _summary.get("daily_challenge", false) else QuestionBank.get_category_display_name(category_id)
	UIHelpers.add_label(vbox, category_name)

	score_label = UIHelpers.add_label(vbox)
	_animate_score_label()

	streak_label = UIHelpers.add_label(vbox)
	streak_label.text = "Best streak: %d" % _summary.get("longest_streak", 0)

	extra_label = UIHelpers.add_label(vbox)
	extra_label.modulate = Color(1, 0.85, 0.3)
	extra_label.text = _build_extra_message()

	UIHelpers.add_spacer(vbox)

	share_button = UIHelpers.add_button(vbox, "📤 Share Score to WhatsApp", 64)
	share_button.pressed.connect(_on_share_pressed)

	share_status_label = UIHelpers.add_label(vbox)
	share_status_label.modulate = Color(1, 0.85, 0.3)

	var play_again := UIHelpers.add_button(vbox, "Play Again", 64)
	play_again.pressed.connect(_on_play_again_pressed)

	var back_home := UIHelpers.add_button(vbox, "Back to Categories", 64)
	back_home.pressed.connect(func(): SceneManager.goto_scene(SceneManager.HOME))

	ShareManager.share_opened.connect(_on_share_opened)


## Counts the points total up from 0 rather than just printing the final
## number — "correct / total" is shown immediately since there's nothing
## to build suspense about there, only the points value animates.
func _animate_score_label() -> void:
	var correct_count: int = _summary.get("correct_count", 0)
	var total_questions: int = _summary.get("total_questions", 0)
	var final_score: int = _summary.get("score", 0)

	score_label.text = "%d / %d correct — 0 points" % [correct_count, total_questions]

	var tween := create_tween()
	tween.set_trans(Tween.TRANS_CUBIC).set_ease(Tween.EASE_OUT)
	tween.tween_method(
		# Untyped on purpose — Tween may hand this a float mid-interpolation
		# even though `from`/`to` below are ints; int(...) it explicitly
		# rather than relying on an implicit conversion into a typed param.
		func(current_score): score_label.text = "%d / %d correct — %d points" % [correct_count, total_questions, int(current_score)],
		0, final_score, 0.6
	)


## Persists the score/streak-day/progression side effects of finishing a
## round, and forwards the score to the leaderboard where relevant. Done
## once, here, right as the round's summary is consumed — Gameplay only
## emits/forwards the summary, it doesn't know what to do with it.
func _apply_save_updates() -> void:
	var category_id: String = _summary.get("category", "")
	var score: int = _summary.get("score", 0)
	var is_daily: bool = _summary.get("daily_challenge", false)

	_summary["_is_new_high_score"] = SaveManager.report_score(category_id, score)

	var level_before := SaveManager.get_player_level()
	SaveManager.add_lifetime_score(score)
	if SaveManager.get_player_level() > level_before:
		_summary["_leveled_up_to"] = SaveManager.get_player_level()

	if is_daily:
		SaveManager.record_daily_challenge_completion()
		LeaderboardManager.submit_daily_score(score, _summary.get("correct_count", 0))

	if SaveManager.report_overall_best_score(score):
		LeaderboardManager.submit_regional_best(score)


## Combines high-score and level-up feedback into the one line under the
## score summary — most rounds trigger neither, some rounds trigger both.
func _build_extra_message() -> String:
	var parts: Array = []
	if _summary.get("_is_new_high_score", false):
		parts.append("New high score!")

	var leveled_up_to: int = _summary.get("_leveled_up_to", 0)
	if leveled_up_to > 0:
		parts.append("🎉 Level up! You're now level %d." % leveled_up_to)
		var unlocked := _category_unlocked_at_level(leveled_up_to)
		if not unlocked.is_empty():
			parts.append("%s unlocked!" % unlocked)

	return "  ".join(parts)


func _category_unlocked_at_level(level: int) -> String:
	for category in QuestionBank.categories:
		if category.get("unlock_level", 0) == level:
			return category.get("display_name", category.get("id", ""))
	return ""


func _on_share_pressed() -> void:
	share_button.disabled = true
	share_status_label.text = "Preparing your score card…"
	ShareManager.share_score(_summary)


func _on_share_opened(method: String) -> void:
	share_button.disabled = false
	match method:
		"plugin", "whatsapp_text":
			share_status_label.text = "Shared! 🎉"
		"clipboard_fallback":
			share_status_label.text = "WhatsApp isn't available here — copied your score to the clipboard instead."
		_:
			share_status_label.text = ""


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
