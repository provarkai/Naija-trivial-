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
  autoload/           Singletons — GameStateManager, QuestionBank, SaveManager, SceneManager
  ui/                  Screen scripts (one per scene) + UIHelpers, the shared procedural-UI builder
  systems/             Reserved for non-autoload gameplay systems as they're split out
data/
  categories.json      Category metadata (id, display name, icon, premium flag)
  questions/           One JSON file per category — see SCHEMA.md
assets/
  icons/ audio/ fonts/ images/   Art & sound (empty for now, .gitkeep placeholders)
```

### Why screens are built in code, not the Godot editor

Every `scenes/*.tscn` file is a bare `Control` node with a script attached — the actual UI tree (labels, buttons, containers) is built procedurally in each script's `_ready()`, using the shared helpers in `scripts/ui/UIHelpers.gd`. There's no Godot editor in this workflow to visually lay out and verify `.tscn` node trees, and a procedural tree is far easier to write and review correctly by hand than raw `.tscn` anchor/layout syntax. Nothing about this is permanent: once the project is opened in the actual editor, any screen can be rebuilt visually as a normal editor-authored scene — the autoloads (`GameStateManager`, `QuestionBank`, `SaveManager`, `SceneManager`) are what everything depends on, not how a given screen's tree was built.

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
| 8 | AdMob integration | ⬜ not started |
| 9 | IAP integration (Google Play Billing) | ⬜ not started — `scenes/Store.tscn` has the UI and calls `SaveManager`'s purchase-state methods directly as dev/test stand-ins until the Play Billing plugin is wired in |
| 10 | UI polish (animations, SFX, category icons, editor-authored scenes) | ⬜ not started |

### Screens

| Screen | Scene | Notes |
|--------|-------|-------|
| Home / Main Menu | `scenes/Home.tscn` | Category grid, Daily Challenge entry, Remove Ads banner, Settings/Store links |
| Gameplay | `scenes/Gameplay.tscn` | Renders whatever round `GameStateManager` currently has in progress — doesn't start rounds itself |
| Round Results | `scenes/Results.tscn` | Score/streak summary, records high score + daily-streak, Play Again / Back Home |
| Daily Challenge | `scenes/DailyChallenge.tscn` | Streak display + entry point into the date-seeded question set |
| Store | `scenes/Store.tscn` | IAP tiers, dev-unlock stand-ins pending Play Billing |
| Settings | `scenes/Settings.tscn` | Sound/music toggles, restore purchases + privacy policy stubs |

Category Select and a Leaderboard screen from the original brief aren't separate scenes yet — Home's grid already covers category selection, and there's no ranking backend (local/regional) to back a leaderboard yet.

### Autoload singletons

- **`GameStateManager`** — the state of the round currently being played: current question index, score, streak, per-question countdown. Scenes call `start_round()`, `submit_answer()`, `next_question()` and listen to its signals (`question_changed`, `answer_submitted`, `round_completed`, etc.) rather than tracking this themselves. `last_round_summary` caches the most recent `round_completed` payload so the Results screen can read it directly even though it wasn't around to catch the live signal.
- **`QuestionBank`** — loads and validates every `data/questions/*.json` file at startup, and hands out shuffled round sets (`get_round_questions`) or the deterministic daily set (`get_daily_challenge_questions`).
- **`SaveManager`** — reads/writes `user://savegame.json`: unlocked categories, high scores per category, daily-challenge streak, purchase entitlements, settings.
- **`SceneManager`** — scene file path constants + navigation, with a small back-stack for screens reachable from more than one place (Store, Settings).

### Question content

Questions live in `data/questions/<category_id>.json`, separate from game logic, so content can be added or edited without touching scripts. Each starter category currently has a handful of sample questions to prove the pipeline end-to-end — see `data/questions/SCHEMA.md` for the schema and content guidelines, and expand toward the launch target of 50-80 questions per category.

## Pre-Build Manual Steps

Not this repo's job, but required before shipping:

- Install Godot 4 + Android export templates + Android SDK
- Create a Google Play Console account
- Create an AdMob account and generate ad unit IDs
- Set up Google Play Billing products in Play Console (product IDs must match the code once IAP is wired up)
