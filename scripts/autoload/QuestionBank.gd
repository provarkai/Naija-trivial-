extends Node
## QuestionBank (autoload singleton)
##
## Loads every category's question JSON file from res://data/questions/,
## validates it against the schema documented in data/questions/SCHEMA.md,
## and hands out shuffled/filtered question sets to the gameplay screens.
##
## Keeping questions in plain JSON (rather than baked into scenes/scripts)
## means content can be added or edited without touching game logic.

const QUESTIONS_DIR := "res://data/questions/"
const CATEGORIES_PATH := "res://data/categories.json"
const REQUIRED_DIFFICULTIES := ["easy", "medium", "hard"]

## category_id -> Array[Dictionary] of validated questions
var _questions_by_category: Dictionary = {}
## Ordered list of category metadata dictionaries loaded from categories.json
var categories: Array = []

var _rng := RandomNumberGenerator.new()


func _ready() -> void:
	_rng.randomize()
	_load_categories()
	_load_all_questions()


# ---------------------------------------------------------------------------
# Loading
# ---------------------------------------------------------------------------

func _load_categories() -> void:
	categories.clear()
	var data = _read_json(CATEGORIES_PATH)
	if data is Array:
		categories = data
	else:
		push_error("QuestionBank: could not load %s" % CATEGORIES_PATH)


func _load_all_questions() -> void:
	_questions_by_category.clear()

	var dir := DirAccess.open(QUESTIONS_DIR)
	if dir == null:
		push_error("QuestionBank: could not open %s" % QUESTIONS_DIR)
		return

	dir.list_dir_begin()
	var file_name := dir.get_next()
	while file_name != "":
		if not dir.current_is_dir() and file_name.get_extension() == "json":
			_load_question_file(QUESTIONS_DIR + file_name)
		file_name = dir.get_next()
	dir.list_dir_end()


func _load_question_file(path: String) -> void:
	var data = _read_json(path)
	if not (data is Dictionary):
		push_error("QuestionBank: %s does not contain a JSON object" % path)
		return

	var category: String = data.get("category", "")
	var raw_questions = data.get("questions", [])
	if category.is_empty() or not (raw_questions is Array):
		push_error("QuestionBank: %s missing 'category' or 'questions'" % path)
		return

	var valid_questions: Array = []
	for entry in raw_questions:
		if _validate_question(entry, path):
			valid_questions.append(entry)

	if _questions_by_category.has(category):
		_questions_by_category[category].append_array(valid_questions)
	else:
		_questions_by_category[category] = valid_questions


func _validate_question(entry, path: String) -> bool:
	if not (entry is Dictionary):
		push_warning("QuestionBank: skipping non-object question in %s" % path)
		return false

	if not entry.has("question") or not (entry["question"] is String) or entry["question"].is_empty():
		push_warning("QuestionBank: skipping question with missing text in %s" % path)
		return false

	var options = entry.get("options", null)
	if not (options is Array) or options.size() != 4:
		push_warning("QuestionBank: skipping '%s' — needs exactly 4 options" % entry.get("question", "?"))
		return false

	# JSON has no distinct int type, so Godot's JSON parser hands numbers back
	# as float — accept both and normalize to int for downstream comparisons.
	var correct_index = entry.get("correct_index", null)
	if not (typeof(correct_index) == TYPE_INT or typeof(correct_index) == TYPE_FLOAT):
		push_warning("QuestionBank: skipping '%s' — correct_index must be 0-3" % entry.get("question", "?"))
		return false
	var correct_index_int: int = int(correct_index)
	if correct_index_int < 0 or correct_index_int > 3:
		push_warning("QuestionBank: skipping '%s' — correct_index must be 0-3" % entry.get("question", "?"))
		return false
	entry["correct_index"] = correct_index_int

	var difficulty = entry.get("difficulty", "easy")
	if not REQUIRED_DIFFICULTIES.has(difficulty):
		push_warning("QuestionBank: '%s' has unknown difficulty '%s', defaulting to easy" % [entry.get("question", "?"), difficulty])
		entry["difficulty"] = "easy"

	return true


func _read_json(path: String):
	if not FileAccess.file_exists(path):
		return null
	var file := FileAccess.open(path, FileAccess.READ)
	if file == null:
		return null
	var text := file.get_as_text()
	var result: Variant = JSON.parse_string(text)
	return result


# ---------------------------------------------------------------------------
# Queries
# ---------------------------------------------------------------------------

func get_category_ids() -> Array:
	return _questions_by_category.keys()


## Looks up the human-readable name for a category id from categories.json.
## Falls back to the id itself (e.g. for the synthetic "Daily Challenge"
## pseudo-category) so callers never have to null-check this.
func get_category_display_name(category_id: String) -> String:
	for category in categories:
		if category.get("id", "") == category_id:
			return category.get("display_name", category_id)
	return category_id


func get_question_count(category: String) -> int:
	return _questions_by_category.get(category, []).size()


## Returns up to `count` shuffled questions for a category. If a `difficulty`
## filter is given, only that difficulty is used; falls back to the full
## pool if the filtered pool is too small.
func get_round_questions(category: String, count: int = 10, difficulty: String = "") -> Array:
	var pool: Array = _questions_by_category.get(category, []).duplicate()

	if not difficulty.is_empty():
		var filtered: Array = pool.filter(func(q): return q.get("difficulty", "easy") == difficulty)
		if filtered.size() >= count:
			pool = filtered

	pool.shuffle()
	return pool.slice(0, min(count, pool.size()))


## Deterministic question set for the Daily Challenge: every player gets the
## same questions on the same calendar date, drawn from every category so
## the daily round isn't dominated by whichever category has the most
## content. `date_string` defaults to today (YYYY-MM-DD, UTC).
func get_daily_challenge_questions(count: int = 10, date_string: String = "") -> Array:
	if date_string.is_empty():
		var d := Time.get_date_dict_from_system(true)
		date_string = "%04d-%02d-%02d" % [d.year, d.month, d.day]

	var seed_value := date_string.hash()
	var daily_rng := RandomNumberGenerator.new()
	daily_rng.seed = seed_value

	# Pull the full pool of all categories, sorted by a stable key first so
	# the shuffle below is reproducible across platforms/runs.
	var all_questions: Array = []
	var category_ids := get_category_ids()
	category_ids.sort()
	for category in category_ids:
		all_questions.append_array(_questions_by_category[category])

	all_questions.sort_custom(func(a, b): return a.get("question", "") < b.get("question", ""))

	# Fisher-Yates shuffle using the seeded RNG for determinism.
	for i in range(all_questions.size() - 1, 0, -1):
		var j := daily_rng.randi_range(0, i)
		var tmp = all_questions[i]
		all_questions[i] = all_questions[j]
		all_questions[j] = tmp

	return all_questions.slice(0, min(count, all_questions.size()))
