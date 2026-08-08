# Naija Trivia Blitz

A Nigerian-themed mobile trivia game for Android, built in **Godot 4** (GDScript). Players answer timed multiple-choice questions across categories like Naija Music, Nollywood, Nigerian History, Sports, General Knowledge, and Pidgin & Proverbs — earning streak bonuses and competing on daily challenge scores.

## Getting started

1. Install [Godot 4.x](https://godotengine.org/download) (this project targets 4.3+).
2. Open Godot, choose **Import**, and select `project.godot` in this repo.
3. Press **F5** (or the Play button) to run — it boots into the Home screen: pick a category or Daily Challenge, play a 10-question round, see your results.

Android export (signed `.aab`) requires the Android build template + SDK/keystore, which are set up separately in the Godot editor's Export settings — see [Pre-Build Manual Steps](#pre-build-manual-steps) below.

## Project structure

```
scenes/              One .tscn per screen (Home, Gameplay, Results, DailyChallenge, Store, Settings, Leaderboard)
scripts/
  autoload/           Singletons — GameStateManager, QuestionBank, SaveManager, SFXManager, SceneManager, AdManager, IAPManager, ShareManager, LeaderboardManager
  ui/                  Screen scripts (one per scene) + UIHelpers, the shared procedural-UI builder
  systems/             Non-autoload shared systems — currently NativePluginBridge (the plugin-call guard AdManager/IAPManager/ShareManager/LeaderboardManager all share)
data/
  categories.json      Category metadata (id, display name, icon, premium flag, unlock_level)
  questions/           One JSON file per category — see SCHEMA.md
  questions/drafts/    Gitignored — unreviewed AI-drafted batches, see QUESTION_GENERATION.md
assets/
  icons/               A placeholder SVG per category (see Polish below)
  audio/ fonts/ images/  Empty for now, .gitkeep placeholders — SFXManager synthesizes sound in code rather than loading files here
tools/
  generate_questions.py  Drafts new questions via Claude, using the existing bank as few-shot examples
  merge_draft.py          Reviewed-draft -> live-file merge step, with its own validation pass
docs/
  ADMOB_SETUP.md         Manual steps to wire a real AdMob plugin into AdManager
  PLAY_BILLING_SETUP.md  Manual steps to wire a real Play Billing plugin into IAPManager
  SHARE_SETUP.md         What already works with zero plugins (WhatsApp text share) vs. what needs one (image auto-attach)
  LEADERBOARD_SETUP.md   Firebase project + GodotFirebase addon setup for LeaderboardManager (lowest-confidence of the four — read this one first)
  QUESTION_GENERATION.md  The AI-assisted content pipeline: draft -> human review -> merge
```

### Why screens are built in code, not the Godot editor

Every `scenes/*.tscn` file is a bare `Control` node with a script attached — the actual UI tree (labels, buttons, containers) is built procedurally in each script's `_ready()`, using the shared helpers in `scripts/ui/UIHelpers.gd`. There's no Godot editor in this workflow to visually lay out and verify `.tscn` node trees, and a procedural tree is far easier to write and review correctly by hand than raw `.tscn` anchor/layout syntax. Nothing about this is permanent: once the project is opened in the actual editor, any screen can be rebuilt visually as a normal editor-authored scene — the autoloads (`GameStateManager`, `QuestionBank`, `SaveManager`, `SFXManager`, `SceneManager`, `AdManager`, `IAPManager`, `ShareManager`, `LeaderboardManager`) are what everything depends on, not how a given screen's tree was built.

## Core systems (current status)

Numbered per the project brief's build order (v2, which inserted streak tracking and share-score as core systems #8-9, ahead of ads/IAP):

| # | System | Status |
|---|--------|--------|
| 1 | Project scaffold | ✅ |
| 2 | Question data system (JSON schema + validation) | ✅ — see `data/questions/SCHEMA.md` |
| 3 | GameStateManager (score/streak/question index/timer) | ✅ `scripts/autoload/GameStateManager.gd` |
| 4 | Core gameplay loop (question → answer → score) | ✅ `scenes/Gameplay.tscn` |
| 5 | Round flow (10 questions → results) | ✅ `scenes/Results.tscn` |
| 6 | Save/load (unlocked categories, high scores, streak days) | ✅ `scripts/autoload/SaveManager.gd` |
| 7 | Daily challenge (deterministic date-seeded question set) | ✅ `scenes/DailyChallenge.tscn` |
| 8 | Streak tracking (increments on daily play, resets on missed day, drives Home display) | ✅ `SaveManager.get_effective_daily_streak()` — see the Retention & Growth section below for why this needed to be more than a stored counter |
| 9 | Share-score flow (score card + WhatsApp share) | ✅ `scripts/autoload/ShareManager.gd`, wired into Results. See `docs/SHARE_SETUP.md` |
| 10 | AdMob integration | ✅ scaffolded — `scripts/autoload/AdManager.gd` wraps a native AdMob plugin (not installed by this repo); banner/interstitial/rewarded all wired into the screens. See `docs/ADMOB_SETUP.md` |
| 11 | IAP integration (Google Play Billing) | ✅ scaffolded — `scripts/autoload/IAPManager.gd` wraps a native Play Billing plugin (not installed by this repo); Store + Settings wired up, product catalog built from `categories.json`'s premium flags. See `docs/PLAY_BILLING_SETUP.md` |
| 12 | UI polish (animations, SFX, category icons, editor-authored scenes) | 🟡 animations/SFX/icons done, see Polish below — editor-authored scenes still need an actual Godot editor in the loop, which this scaffold doesn't have |

See [Known gaps & open decisions](#known-gaps--open-decisions) below for what's still deliberately deferred — the coins/airtime system and the post-launch roadmap.

### Screens

| Screen | Scene | Notes |
|--------|-------|-------|
| Home / Main Menu | `scenes/Home.tscn` | Category grid, level display, Daily Challenge entry, Remove Ads banner, Settings/Store/Leaderboard links |
| Gameplay | `scenes/Gameplay.tscn` | Renders whatever round `GameStateManager` currently has in progress — doesn't start rounds itself |
| Round Results | `scenes/Results.tscn` | Score/streak/level-up summary, Share Score, records progression + submits to the leaderboard, Play Again / Back Home |
| Daily Challenge | `scenes/DailyChallenge.tscn` | Streak display + entry point into the date-seeded question set, link to today's leaderboard |
| Leaderboard | `scenes/Leaderboard.tscn` | Today's Daily Challenge rankings + the player's regional rankings, via `LeaderboardManager` |
| Store | `scenes/Store.tscn` | IAP tiers via `IAPManager`, localized price once the plugin's SKU query resolves |
| Settings | `scenes/Settings.tscn` | Leaderboard profile (display name/region), sound/music toggles, restore purchases + privacy policy stubs |

Category Select from the brief isn't a separate scene — Home's grid already covers category selection (locked/unlocked state, premium indicators, and now level-gate hints all shown right on the button).

### Retention & Growth

Per the brief's "Player Motivation & Retention Design": the reason to come back daily has to be competence, bragging rights, and streak loss-aversion — ads/IAP monetize players who are already returning for those reasons, not the other way round.

- **Streak loss-aversion** — `SaveManager.get_effective_daily_streak()` computes the streak as of *today*, not as of the last save write. The raw stored counter (`current_streak_days`) only gets corrected back to 1 the next time the player completes a Daily Challenge, so reading it directly could show a stale streak for days after it actually lapsed — which defeats the entire point of loss-aversion (it only works if a broken streak reads as broken *the moment* it breaks). Home and the Daily Challenge screen both read the effective value; Daily Challenge also surfaces "🔥 play today to keep your streak" / "you lost your streak" messaging based on it.
- **Share-score flow** — `ShareManager` (see `docs/SHARE_SETUP.md`) renders a branded score card and opens WhatsApp with the score pre-filled, from a "Share Score" button on Results. The WhatsApp text-share half of this works today with **zero plugins installed** — it's just `OS.shell_open()` on the `whatsapp://send` URL scheme, no addon required. Auto-attaching the rendered score card image to the outgoing message is the one piece that needs a native share-sheet plugin (same no-op-safe pattern as AdMob/Billing); until one's installed, the card is still generated and saved locally.
- **Progression (level-gated categories)** — `SaveManager.get_player_level()` derives a level from lifetime score across every round ever played (`POINTS_PER_LEVEL`, tunable). Each premium category in `categories.json` has an `unlock_level`; `is_category_unlocked()` now checks *either* the level gate *or* an IAP purchase — per your call on how this should interact with the existing premium packs, leveling and buying open the same door, not a separate free tier. Results shows a "🎉 Level up!" / category-unlocked message whenever a round crosses a level boundary.
- **Regional + Daily leaderboard** — `LeaderboardManager` (see `docs/LEADERBOARD_SETUP.md`, per your call to build this on Firebase/Firestore) submits to a shared daily leaderboard after every Daily Challenge round, and to a regional leaderboard whenever a round beats the player's all-time best score. Settings gained a "Leaderboard Profile" section (display name + free-text region) to drive it. This is the one system here with no local-only fallback — comparing against other real players needs the backend actually connected, so until Firestore is wired up the Leaderboard screen just says so.

**A confidence note on `LeaderboardManager` specifically:** AdMob and Play Billing are singular, canonical, extremely well-documented plugin targets, so `AdManager`/`IAPManager`'s guessed method names are fairly safe bets. Firebase Firestore's query-builder API (via the community GodotFirebase addon) is less certain from here — `LeaderboardManager` is a structurally-correct sketch (collections, document IDs, data shape, the has_method-guarded call pattern) more than a verified API surface. `docs/LEADERBOARD_SETUP.md` says exactly what to check first.

### Polish

The parts of build-order item #12 that don't need a Godot editor in the loop:

- **Category icons** — a small flat SVG per category in `assets/icons/`, referenced by `categories.json`'s `icon` field and loaded via `UIHelpers.load_category_icon()`. They're intentionally simple placeholder glyphs (a music note, a clapperboard, a trophy, ...) matching each category's brand color — swap them for real art by replacing the files; nothing in code needs to change since the path comes from `categories.json`.
- **Sound effects** — `SFXManager` synthesizes a handful of short tones in code at startup (sine-wave notes into a 16-bit WAV) rather than loading audio files, so there's nothing to source or license for a first playable build. Wired in: a click on every button in the app (via `UIHelpers.add_button`, so every screen gets it for free), correct/wrong/streak-milestone in Gameplay, round-complete/level-up on Results. Respects the existing `sound_enabled` setting. Swap for real SFX later by pointing `SFXManager._build_streams()` at `load()`ed files instead — `play()`'s call sites don't change.
- **Animations** — a button press "bounce" (also from `UIHelpers.add_button`, so again every button gets it automatically), the Results score counting up from 0 rather than appearing instantly, and a brief fade-to-black between every scene change (owned by `SceneManager`, since it has to persist across the very scene swap it's covering up).
- **Editor-authored scenes** — still not done, and can't be from here: every screen's UI tree is built in code (see "Why screens are built in code" above) precisely because there's no Godot editor available in this workflow to build and verify `.tscn` node trees visually. This is the one sub-item of #12 that's a straightforward, if tedious, task once someone opens the project in the real editor — rebuild each screen as an editor-authored scene using the same autoloads, no logic changes needed.

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
- **`SaveManager`** — reads/writes `user://savegame.json`: unlocked categories, high scores per category, daily-challenge streak, purchase entitlements, progression, player identity, settings.
- **`SFXManager`** — a handful of sound effects synthesized in code at startup (see Polish above) behind `play(Sound.CLICK/CORRECT/WRONG/...)`. Respects the `sound_enabled` setting.
- **`SceneManager`** — scene file path constants + navigation (with the fade transition, see Polish above), and a small back-stack for screens reachable from more than one place (Store, Settings).
- **`AdManager`** — wraps a native AdMob plugin singleton (see `docs/ADMOB_SETUP.md`) behind `show_banner()`/`hide_banner()`, `notify_round_completed()` (interstitial every 2-3 rounds), and `show_rewarded(placement, on_reward, on_failed)`. No-ops safely with a console message wherever the plugin isn't installed — including every editor run on desktop — and simulates rewarded-ad rewards in that case so the +5s/revive flow in Gameplay stays testable without a device.
- **`IAPManager`** — wraps a native Google Play Billing plugin singleton (see `docs/PLAY_BILLING_SETUP.md`) behind `purchase(product_id)`, `restore_purchases()`, and `get_price_string(product_id)`. Same no-op-safe pattern as `AdManager`, via the shared `NativePluginBridge` helper; simulates a successful purchase when no plugin is installed so the Store screen is fully testable from the editor.
- **`ShareManager`** — builds the share message + score card image and opens WhatsApp (see `docs/SHARE_SETUP.md`) behind `share_score(summary)`. Unlike the other two, its no-plugin fallback (WhatsApp text share) isn't a degraded stand-in — it's the real, shipped mechanism; a share plugin only adds image auto-attach on top of it.
- **`LeaderboardManager`** — wraps a Firebase Firestore connection (see `docs/LEADERBOARD_SETUP.md`) behind `submit_daily_score()`, `submit_regional_best()`, `get_daily_leaderboard()`, and `get_regional_leaderboard()`. Detected differently from the native-plugin managers above — Firebase installs as ordinary autoloads rather than an engine singleton, so this looks the Firestore node up dynamically by path instead of a static identifier — but funnels every call through the same `NativePluginBridge` guard. No local-only fallback: a leaderboard is inherently about other real players.

### Question content

Questions live in `data/questions/<category_id>.json`, separate from game logic, so content can be added or edited without touching scripts — see `data/questions/SCHEMA.md` for the schema and content guidelines.

**Current count: 400 questions**, comfortably inside the 300-500 launch target, and every category now within or above the 50-80/category range:

| Category | Count |
|---|---|
| Naija Music | 70 |
| Nollywood | 63 |
| Nigerian History | 65 |
| Sports | 66 |
| General Knowledge | 68 |
| Pidgin & Proverbs | 68 |

Every question here is a fact I'm reasonably confident about from general knowledge, but none of it has been checked against a live source or a domain expert — worth a review pass before shipping, particularly anything with a specific date, award, or record. One factual error caught and fixed while expanding this batch: an earlier sports question misattributed Yamile Aldama (who competed for Sudan, then Cuba, then Great Britain — never Nigeria, and never won Olympic gold) as a Nigerian gold medalist; replaced with a verified Chioma Ajunwa fact. Worth treating as a reminder to spot-check the rest, not just the newly-added questions.

**Growing the bank further:** `tools/generate_questions.py` (see `docs/QUESTION_GENERATION.md`) drafts new questions per category using the existing ones as few-shot style examples via the Claude API, run entirely offline as a content-authoring aid — not a runtime feature of the app. Deliberately *not* an in-app "AI layer": an API key embedded in a shipped APK is extractable and abusable, it would break offline play (the brief's own target sessions are "waiting in traffic," exactly when connectivity is weakest), and — most importantly for a trivia game — it would put unreviewed model output directly in front of players with no human fact-check, which is how the Aldama-style error above gets caught in the first place. Drafts land in the gitignored `data/questions/drafts/`, get reviewed by a human, and only then get merged into the live files via `tools/merge_draft.py`.

## Known gaps & open decisions

**Resolved:** regional leaderboard backend (Firebase/Firestore) and how category-unlock-via-level interacts with the existing premium packs (same door as IAP, not a separate tier) — both built per your call, see Retention & Growth above.

**Deliberately not started — the brief itself says to wait:**

- **Coins & Airtime Redemption.** The brief explicitly marks this lower priority than the core loop and flags an open legal question (whether flat-rate, performance-decoupled coin earning avoids gaming/lottery classification under Nigerian law) that should be resolved with a lawyer before any of it is built — including the append-only coin ledger, since that's meaningful engineering effort for a system that might need to change shape based on that answer.
- **Post-Launch Roadmap** (v1.1-v2: head-to-head challenges, achievements, seasonal packs, sponsorships, audio round, web/iOS). The brief says not to start these until v1 has real DAU numbers to validate against — nothing here yet, by design.

**Still open, smaller in scope:**

- **Regional leaderboard security.** As scaffolded, the client writes leaderboard entries directly to Firestore — nothing server-side validates that a submitted score matches an actual completed round. `docs/LEADERBOARD_SETUP.md` has minimum-viable Firestore rules and notes moving to a Cloud Function if the leaderboard starts to matter competitively.
- **Player identity is anonymous.** `SaveManager.get_player_id()` is a random per-install ID, not a real account — reinstalling or switching devices starts a fresh leaderboard history. Fine for now; swapping in Firebase Auth later is a contained change (see the setup doc).

## Pre-Build Manual Steps

Not this repo's job, but required before shipping:

- Install Godot 4 + Android export templates + Android SDK
- Create a Google Play Console account
- Create an AdMob account and generate ad unit IDs
- Set up Google Play Billing products in Play Console — product IDs must match `IAPManager`'s catalog exactly; see `docs/PLAY_BILLING_SETUP.md`
