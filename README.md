# Naija Trivia Blitz

A Nigerian-themed mobile trivia game for Android, built in **Godot 4** (GDScript). Players answer timed multiple-choice questions across categories like Naija Music, Nollywood, Nigerian History, Sports, General Knowledge, and Pidgin & Proverbs — earning streak bonuses and competing on daily challenge scores.

## Getting started

1. Install [Godot 4.x](https://godotengine.org/download) (this project targets 4.3+).
2. Open Godot, choose **Import**, and select `project.godot` in this repo.
3. Press **F5** (or the Play button) to run — it boots straight into a playable vertical slice: pick a category on the home screen, answer 10 questions, see your results.

Android export (signed `.aab`) requires the Android build template + SDK/keystore, which are set up separately in the Godot editor's Export settings — see [Pre-Build Manual Steps](#pre-build-manual-steps) below.

## Project structure

```
scenes/            Godot scenes (.tscn)
scripts/
  autoload/         Singletons (GameStateManager, QuestionBank, SaveManager)
  ui/                Screen/UI scripts
  systems/           Reserved for non-autoload gameplay systems as they're split out
data/
  categories.json    Category metadata (id, display name, icon, premium flag)
  questions/         One JSON file per category — see SCHEMA.md
assets/
  icons/ audio/ fonts/ images/   Art & sound (empty for now, .gitkeep placeholders)
```

## Core systems (current status)

| # | System | Status |
|---|--------|--------|
| 1 | Project scaffold | ✅ |
| 2 | Question data system (JSON schema + validation) | ✅ — see `data/questions/SCHEMA.md` |
| 3 | GameStateManager (score/streak/question index/timer) | ✅ `scripts/autoload/GameStateManager.gd` |
| 4 | Core gameplay loop (question → answer → score) | ✅ minimal version in `scripts/ui/Main.gd` |
| 5 | Round flow (10 questions → results) | ✅ minimal version in `scripts/ui/Main.gd` |
| 6 | Save/load (unlocked categories, high scores, streak days) | ✅ `scripts/autoload/SaveManager.gd` |
| 7 | Daily challenge (deterministic date-seeded question set) | ✅ `QuestionBank.get_daily_challenge_questions()`, not yet wired to a UI entry point |
| 8 | AdMob integration | ⬜ not started |
| 9 | IAP integration (Google Play Billing) | ⬜ not started — `SaveManager` has purchase-state stubs ready for it |
| 10 | UI polish (animations, SFX, category icons, dedicated scenes) | ⬜ not started — `scripts/ui/Main.gd` is an all-in-one placeholder for Home/Gameplay/Results |

### Autoload singletons

- **`GameStateManager`** — the state of the round currently being played: current question index, score, streak, per-question countdown. Scenes call `start_round()`, `submit_answer()`, `next_question()` and listen to its signals (`question_changed`, `answer_submitted`, `round_completed`, etc.) rather than tracking this themselves.
- **`QuestionBank`** — loads and validates every `data/questions/*.json` file at startup, and hands out shuffled round sets (`get_round_questions`) or the deterministic daily set (`get_daily_challenge_questions`).
- **`SaveManager`** — reads/writes `user://savegame.json`: unlocked categories, high scores per category, daily-challenge streak, purchase entitlements, settings.

### Question content

Questions live in `data/questions/<category_id>.json`, separate from game logic, so content can be added or edited without touching scripts. Each starter category currently has a handful of sample questions to prove the pipeline end-to-end — see `data/questions/SCHEMA.md` for the schema and content guidelines, and expand toward the launch target of 50-80 questions per category.

## Pre-Build Manual Steps

Not this repo's job, but required before shipping:

- Install Godot 4 + Android export templates + Android SDK
- Create a Google Play Console account
- Create an AdMob account and generate ad unit IDs
- Set up Google Play Billing products in Play Console (product IDs must match the code once IAP is wired up)
