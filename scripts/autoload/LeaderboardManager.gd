extends Node
## LeaderboardManager (autoload singleton)
##
## Regional + Daily Challenge leaderboards, backed by Firebase Firestore
## via the GodotFirebase addon (https://github.com/GodotNuts/GodotFirebase)
## — NOT installed by this scaffold. See docs/LEADERBOARD_SETUP.md.
##
## WHY THIS LOOKS DIFFERENT FROM AdManager / IAPManager
## AdMob and Play Billing are native Android singletons, checkable at
## runtime via Engine.has_singleton("SomeName") without that name needing
## to exist at parse time — the check is just a string. GodotFirebase is
## the opposite: a GDScript/GDExtension addon that talks to Firebase's
## REST API directly (so, unlike AdMob/Billing, it — and this manager —
## can run in the editor on desktop too, not just an Android export), but
## it installs itself as ordinary project autoloads (Firebase,
## FirebaseFirestore, ...). Referencing those names directly in code that
## must also run without the addon installed would be a parse-time error
## in GDScript, not a runtime no-op. So this looks the autoload up
## dynamically by node path (get_node_or_null("/root/...")) instead of a
## static identifier — different detection mechanism, same safety
## outcome: every call still goes through NativePluginBridge's
## has_method()-guarded call_plugin(), same as AdManager/IAPManager.
##
## CONFIDENCE LEVEL: lower than AdManager/IAPManager. AdMob and Play
## Billing are singular, canonical, well-documented plugin targets;
## GodotFirebase's Firestore query-builder API (collection/document/
## where/order/limit chaining, and what signals results come back on) is
## something this file cannot verify against a real install from here.
## Treat the method names below as a structural sketch of what needs
## to happen, not a verified API surface — see docs/LEADERBOARD_SETUP.md
## for what to check first.
##
## Also unlike AdManager/IAPManager, there's no meaningful "simulate
## locally" fallback: a leaderboard is inherently about comparing against
## other real players, so without Firestore connected, submit/fetch calls
## just no-op and report failure/empty rather than fake teammates.
##
## SECURITY NOTE: as sketched, the client writes leaderboard entries
## directly to Firestore, which means a modified client could write a
## fake score — there's nothing here validating a submitted score against
## an actual completed round server-side. Fine for an early build; if
## the leaderboard starts to matter competitively, move score writes
## behind a Cloud Function that validates before accepting them, rather
## than trusting direct client writes.

signal scores_fetched(scope: String, entries: Array)
signal score_submitted(scope: String, success: bool)

const FIRESTORE_NODE_PATH := "/root/FirebaseFirestore"

## Documents keyed "<date>_<player_id>" so each player has one entry per
## day; latest write for that key wins.
const COLLECTION_DAILY := "daily_leaderboard"
## Documents keyed "<region>_<player_id>" — one entry per player per
## region, meant to hold their personal best (see submit_regional_best).
const COLLECTION_REGIONAL := "regional_leaderboard"

const DEFAULT_LIMIT := 20

var _firestore: Object = null


func _ready() -> void:
	_firestore = get_node_or_null(FIRESTORE_NODE_PATH)
	if _firestore == null:
		print("LeaderboardManager: no autoload at %s — leaderboard disabled until the GodotFirebase addon is installed and configured. See docs/LEADERBOARD_SETUP.md." % FIRESTORE_NODE_PATH)


func is_available() -> bool:
	return _firestore != null


# ---------------------------------------------------------------------------
# Submitting
# ---------------------------------------------------------------------------

## Upserts today's Daily Challenge score. Called from Results whenever a
## Daily Challenge round finishes, win or lose — the daily leaderboard is
## about participation + score, not just personal bests.
func submit_daily_score(score: int, correct_count: int, date_string: String = "") -> void:
	if not is_available():
		score_submitted.emit(COLLECTION_DAILY, false)
		return

	var date := date_string if not date_string.is_empty() else _today()
	var doc_id := "%s_%s" % [date, SaveManager.get_player_id()]
	_upsert_document(COLLECTION_DAILY, doc_id, {
		"player_id": SaveManager.get_player_id(),
		"display_name": _display_name_or_fallback(),
		"region": SaveManager.get_region(),
		"date": date,
		"score": score,
		"correct_count": correct_count,
	})


## Upserts the player's regional-leaderboard entry. Call this only when
## the round is already known to be a new personal best (e.g. Results'
## `_is_new_high_score`/overall-best check) — this function itself does
## not compare against the existing remote value before writing.
func submit_regional_best(score: int) -> void:
	if not is_available():
		score_submitted.emit(COLLECTION_REGIONAL, false)
		return

	var region := SaveManager.get_region()
	if region.is_empty():
		print("LeaderboardManager: no region set (Settings > Region) — skipping regional submission.")
		score_submitted.emit(COLLECTION_REGIONAL, false)
		return

	var doc_id := "%s_%s" % [region, SaveManager.get_player_id()]
	_upsert_document(COLLECTION_REGIONAL, doc_id, {
		"player_id": SaveManager.get_player_id(),
		"display_name": _display_name_or_fallback(),
		"region": region,
		"score": score,
	})


func _display_name_or_fallback() -> String:
	var display_name := SaveManager.get_display_name()
	return display_name if not display_name.is_empty() else "Anonymous Player"


# ---------------------------------------------------------------------------
# Fetching
# ---------------------------------------------------------------------------

func get_daily_leaderboard(date_string: String = "", limit: int = DEFAULT_LIMIT) -> void:
	var date := date_string if not date_string.is_empty() else _today()
	_query_top(COLLECTION_DAILY, "date", date, limit)


func get_regional_leaderboard(region: String, limit: int = DEFAULT_LIMIT) -> void:
	_query_top(COLLECTION_REGIONAL, "region", region, limit)


func _today() -> String:
	var d := Time.get_date_dict_from_system(true)
	return "%04d-%02d-%02d" % [d.year, d.month, d.day]


# ---------------------------------------------------------------------------
# Firestore calls — structural sketch, verify against your installed
# GodotFirebase version. See docs/LEADERBOARD_SETUP.md.
# ---------------------------------------------------------------------------

func _upsert_document(collection: String, doc_id: String, fields: Dictionary) -> void:
	var collection_ref = NativePluginBridge.call_plugin(_firestore, "LeaderboardManager", "collection", [collection])
	if collection_ref == null:
		score_submitted.emit(collection, false)
		return

	var task = NativePluginBridge.call_plugin(collection_ref, "LeaderboardManager", "add", [fields, doc_id])
	if task == null:
		score_submitted.emit(collection, false)
		return

	if task is Object and task.has_signal("task_finished"):
		task.task_finished.connect(func(_result): score_submitted.emit(collection, true))
	else:
		# Can't confirm completion via a signal on whatever `task` actually
		# is — assume the write was issued and let a later fetch reconcile.
		score_submitted.emit(collection, true)


func _query_top(collection: String, field: String, value: String, limit: int) -> void:
	var collection_ref = NativePluginBridge.call_plugin(_firestore, "LeaderboardManager", "collection", [collection])
	if collection_ref == null:
		scores_fetched.emit(collection, [])
		return

	var query = NativePluginBridge.call_plugin(collection_ref, "LeaderboardManager", "query", [])
	if query == null:
		scores_fetched.emit(collection, [])
		return

	query = NativePluginBridge.call_plugin(query, "LeaderboardManager", "where", [field, "EQUAL", value])
	query = NativePluginBridge.call_plugin(query, "LeaderboardManager", "order", ["score", "DESCENDING"])
	query = NativePluginBridge.call_plugin(query, "LeaderboardManager", "limit", [limit])
	if query == null:
		scores_fetched.emit(collection, [])
		return

	var task = NativePluginBridge.call_plugin(query, "LeaderboardManager", "get", [])
	if task is Object and task.has_signal("task_finished"):
		task.task_finished.connect(func(result): _on_query_result(collection, result))
	else:
		scores_fetched.emit(collection, [])


func _on_query_result(collection: String, result) -> void:
	var entries: Array = []
	if result is Array:
		for doc in result:
			var fields = NativePluginBridge.call_plugin(doc, "LeaderboardManager", "get_fields", [])
			if fields is Dictionary:
				entries.append(fields)
	scores_fetched.emit(collection, entries)
