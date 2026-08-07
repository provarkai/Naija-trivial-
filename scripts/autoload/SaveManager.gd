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

var data: Dictionary = {}

signal save_loaded
signal save_updated


func _ready() -> void:
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
	}


# ---------------------------------------------------------------------------
# Categories
# ---------------------------------------------------------------------------

func is_category_unlocked(category_id: String) -> bool:
	if data["purchases"].get("pro_bundle", false):
		return true
	if data["unlocked_categories"].has(category_id):
		return true
	return data["purchases"]["category_packs"].has(category_id)


func unlock_category(category_id: String) -> void:
	if not data["unlocked_categories"].has(category_id):
		data["unlocked_categories"].append(category_id)
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
