# Naija Trivia Blitz

A Nigerian-themed mobile trivia game for Android, built in **Godot 4** (GDScript). Players answer timed multiple-choice questions across categories like Naija Music, Nollywood, Nigerian History, Sports, General Knowledge, and Pidgin & Proverbs — earning streak bonuses and competing on daily challenge scores.

## Getting started

1. Install [Godot 4.x](https://godotengine.org/download) (this project targets 4.3+).
2. Open Godot, choose **Import**, and select `project.godot` in this repo.
3. Press **F5** (or the Play button) to run — it boots into the Home screen: pick a category or Daily Challenge, play a 10-question round, see your results.

Android export (signed `.aab`) requires the Android build template + SDK/keystore, which are set up separately in the Godot editor's Export settings — see [Pre-Build Manual Steps](#pre-build-manual-steps) below.

## Project structure

```
scenes/              One .tscn per screen (Home, Gameplay, Results, DailyChallenge, Store, Settings)
scripts/
  autoload/           Singletons — GameStateManager, QuestionBank, SaveManager, SceneManager, AdManager, IAPManager
  ui/                  Screen scripts (one per scene) + UIHelpers, the shared procedural-UI builder
  systems/             Non-autoload shared systems — currently NativePluginBridge (AdManager/IAPManager's plugin-call guard)
data/
  categories.json      Category metadata (id, display name, icon, premium flag)
  questions/           One JSON file per category — see SCHEMA.md
assets/
  icons/ audio/ fonts/ images/   Art & sound (empty for now, .gitkeep placeholders)
docs/
  ADMOB_SETUP.md        Manual steps to wire a real AdMob plugin into AdManager
  PLAY_BILLING_SETUP.md Manual steps to wire a real Play Billing plugin into IAPManager
```

### Why screens are built in code, not the Godot editor

Every `scenes/*.tscn` file is a bare `Control` node with a script attached — the actual UI tree (labels, buttons, containers) is built procedurally in each script's `_ready()`, using the shared helpers in `scripts/ui/UIHelpers.gd`. There's no Godot editor in this workflow to visually lay out and verify `.tscn` node trees, and a procedural tree is far easier to write and review correctly by hand than raw `.tscn` anchor/layout syntax. Nothing about this is permanent: once the project is opened in the actual editor, any screen can be rebuilt visually as a normal editor-authored scene — the autoloads (`GameStateManager`, `QuestionBank`, `SaveManager`, `SceneManager`, `AdManager`, `IAPManager`) are what everything depends on, not how a given screen's tree was built.

## Core systems (current status)

| # | System | Status |
|---|--------|--------|
| 1 | Project scaffold | ✅ |
| 2 | Question data system (JSON schema + validation) | ✅ — see `data/questions/SCHEMA.md` |
| 3 | GameStateManager (score/streak/question index/timer) | ✅ `scripts/autoload/GameStateManager.gd` |
| 4 | Core gameplay loop (question → answer → score) | ✅ `scenes/Gameplay.tscn` |
| 5 | Round flow (10 questions → results) | ✅ `scenes/Results.tscn` |
| 6 | Save/load (unlocked categories, high scores, streak days) | ✅ `scripts/autoload/SaveManager.gd` |
| 7 | Daily challenge (deterministic date-seeded question set) | ✅ `scenes/DailyChallenge.tscn`, streak tracked via `SaveManager` |
| 8 | AdMob integration | ✅ scaffolded — `scripts/autoload/AdManager.gd` wraps a native AdMob plugin (not installed by this repo); banner/interstitial/rewarded all wired into the screens. See `docs/ADMOB_SETUP.md` |
| 9 | IAP integration (Google Play Billing) | ✅ scaffolded — `scripts/autoload/IAPManager.gd` wraps a native Play Billing plugin (not installed by this repo); Store + Settings wired up, product catalog built from `categories.json`'s premium flags. See `docs/PLAY_BILLING_SETUP.md` |
| 10 | UI polish (animations, SFX, category icons, editor-authored scenes) | ⬜ not started |

### Screens

| Screen | Scene | Notes |
|--------|-------|-------|
| Home / Main Menu | `scenes/Home.tscn` | Category grid, Daily Challenge entry, Remove Ads banner, Settings/Store links |
| Gameplay | `scenes/Gameplay.tscn` | Renders whatever round `GameStateManager` currently has in progress — doesn't start rounds itself |
| Round Results | `scenes/Results.tscn` | Score/streak summary, records high score + daily-streak, Play Again / Back Home |
| Daily Challenge | `scenes/DailyChallenge.tscn` | Streak display + entry point into the date-seeded question set |
| Store | `scenes/Store.tscn` | IAP tiers via `IAPManager`, localized price once the plugin's SKU query resolves |
| Settings | `scenes/Settings.tscn` | Sound/music toggles, restore purchases + privacy policy stubs |

Category Select and a Leaderboard screen from the original brief aren't separate scenes yet — Home's grid already covers category selection, and there's no ranking backend (local/regional) to back a leaderboard yet.

### Monetization (ads)

Wired per the brief's rules, all via `AdManager` (see `docs/ADMOB_SETUP.md` to connect a real plugin):

- **Banner** — Home only. Never shown during gameplay.
- **Interstitial** — every 2-3 completed rounds, triggered from Results after a round fully ends. Never mid-question.
- **Rewarded video** — in Gameplay: "Watch Ad for +5s" while a question is active, "Watch Ad to Revive Streak" for ~3.5s after a wrong answer.
- **Remove Ads** respected by banner + interstitial everywhere; rewarded video stays available regardless (it's opt-in).

### Monetization (IAP)

All via `IAPManager` (see `docs/PLAY_BILLING_SETUP.md` to connect a real plugin):

- **Remove Ads**, **Pro Bundle**, and one **category pack** per `"premium": true` category in `data/categories.json` — currently Nigerian History, Sports, and Pidgin & Proverbs.
- Every purchase button on the Store screen calls `IAPManager.purchase(product_id)`; without a real plugin installed, the purchase is simulated (granted after a short delay) so the whole flow — including unlocking a category on Home, hiding the ads banner — is testable from the editor.
- `SaveManager` stays the single source of truth for what's owned; `IAPManager` only ever turns a completed purchase into the matching `SaveManager` call, never tracks ownership itself.
- Restore Purchases is wired on both Store and Settings.

### Autoload singletons

- **`GameStateManager`** — the state of the round currently being played: current question index, score, streak, per-question countdown. Scenes call `start_round()`, `submit_answer()`, `next_question()` and listen to its signals (`question_changed`, `answer_submitted`, `round_completed`, etc.) rather than tracking this themselves. `last_round_summary` caches the most recent `round_completed` payload so the Results screen can read it directly even though it wasn't around to catch the live signal.
- **`QuestionBank`** — loads and validates every `data/questions/*.json` file at startup, and hands out shuffled round sets (`get_round_questions`) or the deterministic daily set (`get_daily_challenge_questions`).
- **`SaveManager`** — reads/writes `user://savegame.json`: unlocked categories, high scores per category, daily-challenge streak, purchase entitlements, settings.
- **`SceneManager`** — scene file path constants + navigation, with a small back-stack for screens reachable from more than one place (Store, Settings).
- **`AdManager`** — wraps a native AdMob plugin singleton (see `docs/ADMOB_SETUP.md`) behind `show_banner()`/`hide_banner()`, `notify_round_completed()` (interstitial every 2-3 rounds), and `show_rewarded(placement, on_reward, on_failed)`. No-ops safely with a console message wherever the plugin isn't installed — including every editor run on desktop — and simulates rewarded-ad rewards in that case so the +5s/revive flow in Gameplay stays testable without a device.
- **`IAPManager`** — wraps a native Google Play Billing plugin singleton (see `docs/PLAY_BILLING_SETUP.md`) behind `purchase(product_id)`, `restore_purchases()`, and `get_price_string(product_id)`. Same no-op-safe pattern as `AdManager`, via the shared `NativePluginBridge` helper; simulates a successful purchase when no plugin is installed so the Store screen is fully testable from the editor.

### Question content

Questions live in `data/questions/<category_id>.json`, separate from game logic, so content can be added or edited without touching scripts. Each starter category currently has a handful of sample questions to prove the pipeline end-to-end — see `data/questions/SCHEMA.md` for the schema and content guidelines, and expand toward the launch target of 50-80 questions per category.

## Pre-Build Manual Steps

Not this repo's job, but required before shipping:

- Install Godot 4 + Android export templates + Android SDK
- Create a Google Play Console account
- Create an AdMob account and generate ad unit IDs
- Set up Google Play Billing products in Play Console — product IDs must match `IAPManager`'s catalog exactly; see `docs/PLAY_BILLING_SETUP.md`
