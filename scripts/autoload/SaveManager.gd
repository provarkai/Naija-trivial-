extends Node
## SaveManager (autoload singleton)
##
## Local persistence for everything that needs to survive an app restart:
## unlocked categories, high scores, daily-challenge streak, settings, and
## purchased IAP entitlements. Backed by a single JSON file in the user's
## save directory (res://user://) so it works before any leaderboard/cloud
## save backend is wired up.

const SAVE_PATH := "user://savegame.json"
const SAVE_VERSION := 1

const FREE_CATEGORIES := ["naija_music", "nollywood", "general_knowledge"]

## Lifetime points per player level. A typical round earns roughly
## 500-1500 points depending on difficulty/streak/speed bonuses (see
## GameStateManager._score_for_answer), so this is tuned for "a level or
## so per session," not per round — see categories.json's per-category
## `unlock_level` for how this gates content.
const POINTS_PER_LEVEL := 1000

var data: Dictionary = {}

signal save_loaded
signal save_updated
signal leveled_up(new_level: int)


func _ready() -> void:
	# Godot's global RNG (randi()/randf()) starts from a fixed seed unless
	# explicitly randomized — get_player_id() below needs real entropy.
	randomize()
	load_game()


# ---------------------------------------------------------------------------
# Load / Save
# ---------------------------------------------------------------------------

func load_game() -> void:
	data = _default_data()

	if FileAccess.file_exists(SAVE_PATH):
		var file := FileAccess.open(SAVE_PATH, FileAccess.READ)
		if file != null:
			var parsed = JSON.parse_string(file.get_as_text())
			if parsed is Dictionary:
				data.merge(parsed, true)

	save_loaded.emit()


func save_game() -> void:
	var file := FileAccess.open(SAVE_PATH, FileAccess.WRITE)
	if file == null:
		push_error("SaveManager: could not open %s for writing" % SAVE_PATH)
		return
	file.store_string(JSON.stringify(data, "\t"))
	save_updated.emit()


func _default_data() -> Dictionary:
	return {
		"version": SAVE_VERSION,
		"unlocked_categories": FREE_CATEGORIES.duplicate(),
		"high_scores": {}, # category_id -> int
		"daily_challenge": {
			"last_played_date": "",
			"current_streak_days": 0,
			"longest_streak_days": 0,
			"completed_dates": [],
		},
		"purchases": {
			"remove_ads": false,
			"pro_bundle": false,
			"category_packs": [], # category_ids unlocked via single-pack purchase
		},
		"settings": {
			"sound_enabled": true,
			"music_enabled": true,
		},
		"progression": {
			"lifetime_score": 0, # sum of every round's score, ever — drives get_player_level()
			"best_score": 0, # best single-round score across any category — feeds the regional leaderboard
		},
		"player": {
			"id": "", # generated on first read via get_player_id(), stable thereafter
			"display_name": "",
			"region": "", # free-text state/city, entered in Settings — leaderboard grouping
		},
	}


# ---------------------------------------------------------------------------
# Categories
# ---------------------------------------------------------------------------

## True if a category is playable right now, via *any* path: the default
## free set, an IAP category-pack purchase, Pro Bundle, or — per the
## brief's progression design — having leveled up past that category's
## `unlock_level` in categories.json. Leveling and buying unlock the same
## door; there's no separate "level-only" tier.
func is_category_unlocked(category_id: String) -> bool:
	if data["purchases"].get("pro_bundle", false):
		return true
	if data["unlocked_categories"].has(category_id):
		return true
	if data["purchases"]["category_packs"].has(category_id):
		return true
	return get_player_level() >= _category_unlock_level(category_id)


func unlock_category(category_id: String) -> void:
	if not data["unlocked_categories"].has(category_id):
		data["unlocked_categories"].append(category_id)
	save_game()


## Looks up a category's level-gate from categories.json (via
## QuestionBank, loaded before SaveManager per project.godot's autoload
## order). Categories without an explicit `unlock_level` unlock at level
## 1 — i.e. by whichever *other* path already applies (free list or IAP);
## leveling isn't what's gating them.
func _category_unlock_level(category_id: String) -> int:
	for category in QuestionBank.categories:
		if category.get("id", "") == category_id:
			return category.get("unlock_level", 1)
	return 1


# ---------------------------------------------------------------------------
# Progression (lifetime score -> player level)
# ---------------------------------------------------------------------------

func get_lifetime_score() -> int:
	return data.get("progression", {}).get("lifetime_score", 0)


func get_player_level() -> int:
	return int(get_lifetime_score() / POINTS_PER_LEVEL) + 1


## Adds `points` to the lifetime total that drives get_player_level(),
## emitting leveled_up if this crosses a level boundary. Call once per
## completed round with that round's score (Results._apply_save_updates).
func add_lifetime_score(points: int) -> void:
	if points <= 0:
		return
	var before := get_player_level()
	data["progression"]["lifetime_score"] = get_lifetime_score() + points
	save_game()
	var after := get_player_level()
	if after > before:
		leveled_up.emit(after)


func get_overall_best_score() -> int:
	return data.get("progression", {}).get("best_score", 0)


## True if `score` beats the player's all-time best single-round score
## across any category — kept separate from per-category high scores
## (report_score) since this is what feeds the regional leaderboard,
## which ranks players against each other, not each player against their
## own per-category history.
func report_overall_best_score(score: int) -> bool:
	if score > get_overall_best_score():
		data["progression"]["best_score"] = score
		save_game()
		return true
	return false


# ---------------------------------------------------------------------------
# Player identity (for the regional leaderboard — see LeaderboardManager)
# ---------------------------------------------------------------------------

## A stable anonymous ID for this install, generated once on first call
## and persisted thereafter. Good enough to key leaderboard documents
## without a full auth system; swap for a real Firebase Auth UID if/when
## one gets wired in (see docs/LEADERBOARD_SETUP.md).
func get_player_id() -> String:
	var id: String = data["player"].get("id", "")
	if id.is_empty():
		id = (str(Time.get_unix_time_from_system()) + str(randi())).sha256_text().substr(0, 20)
		data["player"]["id"] = id
		save_game()
	return id


func get_display_name() -> String:
	return data["player"].get("display_name", "")


func set_display_name(value: String) -> void:
	data["player"]["display_name"] = value
	save_game()


func get_region() -> String:
	return data["player"].get("region", "")


func set_region(value: String) -> void:
	data["player"]["region"] = value
	save_game()


# ---------------------------------------------------------------------------
# High scores
# ---------------------------------------------------------------------------

func get_high_score(category_id: String) -> int:
	return data["high_scores"].get(category_id, 0)


## Returns true if this was a new high score.
func report_score(category_id: String, score: int) -> bool:
	var current: int = get_high_score(category_id)
	if score > current:
		data["high_scores"][category_id] = score
		save_game()
		return true
	return false


# ---------------------------------------------------------------------------
# Daily challenge streak
# ---------------------------------------------------------------------------

func has_played_daily_today(date_string: String = "") -> bool:
	return data["daily_challenge"]["last_played_date"] == _today_or(date_string)


## The streak as of *today*, not as of the last time it was written.
## current_streak_days only gets corrected to 1 the next time the player
## completes a Daily Challenge — read directly, it can go on showing a
## stale streak for days after it's actually lapsed. Loss-aversion (the
## brief's core retention lever) only works if a broken streak reads as
## broken the moment it breaks, so every screen should call this instead
## of reading current_streak_days off `data` directly.
func get_effective_daily_streak(date_string: String = "") -> int:
	var dc: Dictionary = data.get("daily_challenge", {})
	var last_played: String = dc.get("last_played_date", "")
	if last_played.is_empty():
		return 0

	var today := _today_or(date_string)
	if last_played == today or last_played == _shift_date(today, -1):
		return dc.get("current_streak_days", 0)
	return 0 # last played before yesterday — the streak has already lapsed


## Marks today's daily challenge as complete and updates the streak. Streak
## continues if the previous completed date was exactly yesterday, resets
## to 1 otherwise.
func record_daily_challenge_completion(date_string: String = "") -> void:
	var today := _today_or(date_string)
	var dc: Dictionary = data["daily_challenge"]

	if dc["last_played_date"] == today:
		return # already recorded today

	var yesterday := _shift_date(today, -1)
	if dc["last_played_date"] == yesterday:
		dc["current_streak_days"] += 1
	else:
		dc["current_streak_days"] = 1

	dc["longest_streak_days"] = max(dc["longest_streak_days"], dc["current_streak_days"])
	dc["last_played_date"] = today
	if not dc["completed_dates"].has(today):
		dc["completed_dates"].append(today)

	save_game()


func _today_or(date_string: String) -> String:
	if not date_string.is_empty():
		return date_string
	var d := Time.get_date_dict_from_system(true)
	return "%04d-%02d-%02d" % [d.year, d.month, d.day]


func _shift_date(date_string: String, day_offset: int) -> String:
	var parts := date_string.split("-")
	var unix_time := Time.get_unix_time_from_datetime_dict({
		"year": int(parts[0]), "month": int(parts[1]), "day": int(parts[2]),
		"hour": 0, "minute": 0, "second": 0,
	})
	unix_time += day_offset * 86400
	var d := Time.get_date_dict_from_unix_time(unix_time)
	return "%04d-%02d-%02d" % [d.year, d.month, d.day]


# ---------------------------------------------------------------------------
# Purchases (Google Play Billing hooks land here once wired up)
# ---------------------------------------------------------------------------

func set_ads_removed(value: bool = true) -> void:
	data["purchases"]["remove_ads"] = value
	save_game()


func has_ads_removed() -> bool:
	return data["purchases"].get("remove_ads", false) or data["purchases"].get("pro_bundle", false)


func set_pro_bundle_owned(value: bool = true) -> void:
	data["purchases"]["pro_bundle"] = value
	save_game()


func unlock_category_pack(category_id: String) -> void:
	if not data["purchases"]["category_packs"].has(category_id):
		data["purchases"]["category_packs"].append(category_id)
	save_game()


# ---------------------------------------------------------------------------
# Settings
# ---------------------------------------------------------------------------

func set_sound_enabled(value: bool) -> void:
	data["settings"]["sound_enabled"] = value
	save_game()


func set_music_enabled(value: bool) -> void:
	data["settings"]["music_enabled"] = value
	save_game()
