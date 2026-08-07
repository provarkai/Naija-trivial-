extends Node
## GameStateManager (autoload singleton)
##
## Owns the state of "the round the player is currently playing":
## which category, which questions, which question we're on, the score,
## the current answer streak, and the per-question countdown.
##
## Scenes (Gameplay, Results, Daily Challenge) read from this singleton and
## call its methods rather than tracking score/streak locally, so the same
## round state survives scene changes (e.g. "watch ad to continue").

# ---------------------------------------------------------------------------
# Tuning
# ---------------------------------------------------------------------------

const QUESTIONS_PER_ROUND := 10
const DEFAULT_TIME_PER_QUESTION := 15.0 # seconds

const BASE_POINTS := 100
const STREAK_BONUS_PER_STEP := 10 # extra points per streak level, before cap
const STREAK_BONUS_CAP := 100
const TIME_BONUS_MAX := 50 # awarded when answered instantly

const DIFFICULTY_MULTIPLIER := {
	"easy": 1.0,
	"medium": 1.25,
	"hard": 1.5,
}

# ---------------------------------------------------------------------------
# Signals
# ---------------------------------------------------------------------------

signal round_started(category: String, total_questions: int)
signal question_changed(question: Dictionary, index: int, total: int)
signal timer_tick(time_remaining: float, time_total: float)
signal answer_submitted(result: Dictionary)
signal streak_changed(streak: int, longest_streak: int)
signal score_changed(score: int)
signal round_completed(summary: Dictionary)

# ---------------------------------------------------------------------------
# Round state
# ---------------------------------------------------------------------------

var current_category: String = ""
var is_daily_challenge: bool = false

var questions: Array = []
var current_question_index: int = -1
var answers_given: Array = [] # one entry per question: {selected_index, correct, points}

var score: int = 0
var streak: int = 0
var longest_streak: int = 0
var correct_count: int = 0

## Snapshot of the summary dict from the most recent round_completed emission.
## The Results screen reads this directly on _ready() rather than relying on
## having been connected in time to catch the live signal (it usually
## wasn't — the round finishes on the Gameplay scene, which then changes to
## Results, and by the time Results exists the signal has already fired).
var last_round_summary: Dictionary = {}

var time_per_question: float = DEFAULT_TIME_PER_QUESTION
var time_remaining: float = 0.0
var _round_active: bool = false
var _question_locked: bool = false # true once an answer has been submitted for this question

var _timer: Timer


func _ready() -> void:
	_timer = Timer.new()
	_timer.one_shot = false
	_timer.wait_time = 0.1
	add_child(_timer)
	_timer.timeout.connect(_on_timer_tick)


# ---------------------------------------------------------------------------
# Round lifecycle
# ---------------------------------------------------------------------------

## Starts a new round with a pre-selected list of question dictionaries.
## `questions` should already be trimmed/shuffled to QUESTIONS_PER_ROUND by
## QuestionBank (or fewer, for short/testing rounds).
func start_round(category: String, round_questions: Array, opts: Dictionary = {}) -> void:
	current_category = category
	questions = round_questions.duplicate()
	is_daily_challenge = opts.get("daily_challenge", false)
	time_per_question = opts.get("time_per_question", DEFAULT_TIME_PER_QUESTION)

	current_question_index = -1
	answers_given.clear()
	score = 0
	streak = 0
	longest_streak = 0
	correct_count = 0
	_round_active = true

	score_changed.emit(score)
	streak_changed.emit(streak, longest_streak)
	round_started.emit(current_category, questions.size())

	_advance_question()


func get_current_question() -> Dictionary:
	if current_question_index < 0 or current_question_index >= questions.size():
		return {}
	return questions[current_question_index]


## Records the player's answer for the current question, scores it, and
## returns a result dictionary the Gameplay scene can use to render feedback.
func submit_answer(selected_index: int) -> Dictionary:
	if not _round_active or _question_locked:
		return {}

	_question_locked = true
	_timer.stop()

	var question: Dictionary = get_current_question()
	var correct_index: int = question.get("correct_index", -1)
	var correct: bool = selected_index == correct_index

	var streak_before_miss := streak
	var points := 0
	if correct:
		points = _score_for_answer(question)
		streak += 1
		longest_streak = max(longest_streak, streak)
		correct_count += 1
	else:
		streak = 0

	score += points

	var result := {
		"correct": correct,
		"correct_index": correct_index,
		"selected_index": selected_index,
		"points_earned": points,
		"score": score,
		"streak": streak,
		"streak_before_miss": streak_before_miss,
		"time_remaining": time_remaining,
		"timed_out": selected_index == -1,
	}
	answers_given.append(result)

	answer_submitted.emit(result)
	streak_changed.emit(streak, longest_streak)
	score_changed.emit(score)

	return result


## Called by the Gameplay scene when the countdown bar visually finishes,
## or automatically by the internal timer if nothing else is driving it.
func time_up() -> Dictionary:
	if _question_locked:
		return {}
	return submit_answer(-1)


func next_question() -> void:
	if not _round_active:
		return
	if current_question_index + 1 >= questions.size():
		end_round()
		return
	_advance_question()


func end_round() -> void:
	if not _round_active:
		return
	_round_active = false
	_timer.stop()

	var summary := {
		"category": current_category,
		"daily_challenge": is_daily_challenge,
		"score": score,
		"correct_count": correct_count,
		"total_questions": questions.size(),
		"longest_streak": longest_streak,
		"answers": answers_given.duplicate(),
	}
	last_round_summary = summary
	round_completed.emit(summary)


# ---------------------------------------------------------------------------
# Rewarded-ad hooks (called by the AdMob layer once it lands)
# ---------------------------------------------------------------------------

## "Watch ad for +5 seconds" — extends the current question's countdown.
func grant_extra_time(seconds: float = 5.0) -> void:
	if not _round_active or _question_locked:
		return
	time_remaining = min(time_remaining + seconds, time_per_question + seconds)
	timer_tick.emit(time_remaining, time_per_question)


## "Watch ad to revive" — undoes the loss of streak from the last wrong
## answer and lets the round continue instead of ending. Only meaningful
## right after a wrong/timed-out answer, before advancing.
func revive() -> void:
	if answers_given.is_empty():
		return
	var last: Dictionary = answers_given.back()
	if last.get("correct", false):
		return
	streak = last.get("streak_before_miss", streak)


# ---------------------------------------------------------------------------
# Internal
# ---------------------------------------------------------------------------

func _advance_question() -> void:
	current_question_index += 1
	_question_locked = false
	time_remaining = time_per_question

	var question := get_current_question()
	question_changed.emit(question, current_question_index, questions.size())
	timer_tick.emit(time_remaining, time_per_question)
	_timer.start()


func _on_timer_tick() -> void:
	if _question_locked or not _round_active:
		return
	time_remaining = max(0.0, time_remaining - _timer.wait_time)
	timer_tick.emit(time_remaining, time_per_question)
	if time_remaining <= 0.0:
		time_up()


func _score_for_answer(question: Dictionary) -> int:
	var difficulty: String = question.get("difficulty", "easy")
	var multiplier: float = DIFFICULTY_MULTIPLIER.get(difficulty, 1.0)

	var streak_bonus: int = min(streak * STREAK_BONUS_PER_STEP, STREAK_BONUS_CAP)

	var time_ratio: float = 0.0
	if time_per_question > 0.0:
		time_ratio = clampf(time_remaining / time_per_question, 0.0, 1.0)
	var time_bonus: int = int(round(TIME_BONUS_MAX * time_ratio))

	var total: float = (BASE_POINTS + streak_bonus + time_bonus) * multiplier
	return int(round(total))
