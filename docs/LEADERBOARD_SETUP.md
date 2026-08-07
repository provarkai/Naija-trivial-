# Leaderboard Setup (Firebase Firestore)

`scripts/autoload/LeaderboardManager.gd` wraps a Firebase Firestore
connection behind `submit_daily_score()`, `submit_regional_best()`,
`get_daily_leaderboard()`, and `get_regional_leaderboard()`. It contains
no Firebase SDK itself. This is the lowest-confidence of the four
plugin-wrapper docs in this repo (see the class comment at the top of
`LeaderboardManager.gd`) — read that before assuming any method name
below is correct as written.

## Why this is different from AdMob/Billing setup

AdMob and Play Billing are native Android singletons: `Engine.has_singleton("Name")`
is a runtime string check that works whether or not the plugin is
installed, so `AdManager`/`IAPManager` can reference them defensively
without a compile error. Firebase (via the community **GodotFirebase**
addon, https://github.com/GodotNuts/GodotFirebase) is a GDScript/GDExtension
addon that talks to Firebase's REST API directly — which means, unlike
AdMob/Billing, it (and a real leaderboard) would actually work in the
**editor on desktop**, not just an Android export. But it installs itself
as ordinary project autoloads (`Firebase`, `FirebaseFirestore`, ...)
rather than an engine singleton, and referencing those names directly
before the addon is installed would be a parse-time error in GDScript.
`LeaderboardManager` works around this by looking the autoload up
dynamically via `get_node_or_null("/root/FirebaseFirestore")` instead of
a static identifier.

## 1. Create a Firebase project

- Create a project at https://console.firebase.google.com.
- Enable **Firestore** (Native mode).
- Register an Android app in the Firebase project (package name must
  match this project's Android export preset) to get the config values
  GodotFirebase needs.

## 2. Install the GodotFirebase addon

- AssetLib (search "Firebase") or manually from the GitHub repo above.
- Enable it under Project Settings > Plugins — this is what registers
  the `Firebase`/`FirebaseFirestore`/etc. autoloads `LeaderboardManager`
  looks for at `/root/FirebaseFirestore`. **Verify that node name** —
  older/newer versions of the addon may name it differently; update
  `LeaderboardManager.FIRESTORE_NODE_PATH` if so.
- Fill in the addon's Firebase config (API key, project ID, etc.) per
  its own README, from the Firebase project created in step 1.

## 3. Verify the Firestore API shape against LeaderboardManager.gd

This is the step most likely to need real work. `LeaderboardManager`
assumes a chainable query builder roughly like:

```
Firestore.collection(name).add(fields, doc_id)      # upsert a document
Firestore.collection(name).query()
    .where(field, "EQUAL", value)
    .order("score", "DESCENDING")
    .limit(n)
    .get()                                            # -> a Task-like object
```

with results/completion arriving via a signal (`task_finished` is what's
wired up in `_upsert_document`/`_query_top`). **Check this against your
installed version's actual API** — collection/document/query method
names, how a query result comes back (an Array of document objects? Does
each document expose `.get_fields()`? Or does the whole thing come back
as one dictionary already?), and what signal(s) fire on completion vs.
error. Update `LeaderboardManager._upsert_document()` and
`_query_top()`/`_on_query_result()` to match — the rest of the game only
ever calls the small public API on `LeaderboardManager`
(`submit_daily_score`, `submit_regional_best`, `get_daily_leaderboard`,
`get_regional_leaderboard`), so nothing else needs to change once this
is correct.

## 4. Firestore data model

Two collections, both flat documents (no subcollections):

**`daily_leaderboard`** — one document per player per day, document ID
`"<date>_<player_id>"` (e.g. `"2026-08-07_ab12cd34..."`):

| Field | Type | Notes |
|---|---|---|
| `player_id` | string | `SaveManager.get_player_id()` — a locally-generated anonymous ID, not a real auth UID (see step 6) |
| `display_name` | string | `SaveManager.get_display_name()`, falls back to "Anonymous Player" |
| `region` | string | `SaveManager.get_region()`, may be empty |
| `date` | string | `YYYY-MM-DD` |
| `score` | number | that day's Daily Challenge score |
| `correct_count` | number | out of 10 |

**`regional_leaderboard`** — one document per player per region,
document ID `"<region>_<player_id>"`, holding that player's **best
single-round score across any category/mode** (not just Daily
Challenge — see `SaveManager.get_overall_best_score()`):

| Field | Type | Notes |
|---|---|---|
| `player_id` | string | |
| `display_name` | string | |
| `region` | string | |
| `score` | number | personal best, overwritten only when beaten (client-side check — see security note) |

## 5. Firestore security rules

**As scaffolded, the client writes both collections directly** — there's
no server validating that a submitted score corresponds to an actual
completed round. Minimum viable rules to start with:

```
match /daily_leaderboard/{docId} {
  allow read: if true;
  allow write: if request.resource.data.player_id is string
            && request.resource.data.score is int
            && request.resource.data.score >= 0
            && request.resource.data.score <= 5000; // sanity ceiling, tune to your scoring formula
}
match /regional_leaderboard/{docId} {
  allow read: if true;
  allow write: if request.resource.data.player_id is string
            && request.resource.data.score is int
            && request.resource.data.score >= 0
            && request.resource.data.score <= 5000;
}
```

This blocks obviously-fake values but not a modified client submitting a
plausible-but-untrue score. If the leaderboard starts to matter
competitively, move writes behind a **Cloud Function** that validates
against server-side round state instead of trusting the client — out of
scope for this scaffold.

## 6. Player identity — anonymous for now

`SaveManager.get_player_id()` generates a random per-install ID on first
use (no Firebase Auth). This is enough to key leaderboard documents but
isn't a real identity — reinstalling the app or playing on a second
device creates a new ID and a fresh leaderboard history. If that matters,
add Firebase Anonymous Auth (or a real sign-in) and swap
`get_player_id()`'s generated string for the auth UID; nothing else in
`LeaderboardManager` needs to change since it only ever calls
`SaveManager.get_player_id()`, never generates the ID itself.

## What's already wired up in-game

- **Leaderboard screen** (`scenes/Leaderboard.tscn`) — "Today" (daily)
  and "My Region" tabs, reachable from Home and from Daily Challenge.
- **Settings** — "Leaderboard Profile" section for display name + region
  (free-text state/city — there's no fixed list, so leaderboard grouping
  is only as consistent as what players type).
- **Results** — submits to the daily leaderboard after every Daily
  Challenge round, and to the regional leaderboard whenever a round beats
  the player's all-time best score (any category/mode).
- Without Firestore connected, all of the above degrade to a "not
  connected yet" message — no local-only fallback, since a leaderboard
  is inherently about other real players.
