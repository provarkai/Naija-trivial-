# AdMob Setup

`scripts/autoload/AdManager.gd` is a wrapper the rest of the game calls
(`AdManager.show_banner()`, `AdManager.notify_round_completed()`,
`AdManager.show_rewarded(...)`) — it does not itself contain an AdMob SDK.
Ads stay silently disabled (with a console message) until you install a
real AdMob plugin and export to Android with it. This doc is the manual
setup AdManager can't do for you.

## 1. Install the plugin

AdManager targets the **Poing Studios Godot AdMob plugin**
(https://github.com/poing-studios/godot-admob-plugin), the most widely
used Godot 4 AdMob addon at time of writing. Install it either via:

- Godot's AssetLib tab (search "AdMob"), or
- Manually: download the release and drop its `addons/admob/` folder into
  this project's `addons/`.

Then enable it under **Project Settings > Plugins**.

If you use a different AdMob plugin instead, everything downstream of the
plugin call still works the same way — you'll just need to adjust the
method/signal name strings in `AdManager._call_plugin()` /
`AdManager._connect_plugin_signals()` to match that plugin's API.

## 2. Configure the Android export preset

In **Project > Export**, on your Android preset:

1. Enable **Gradle Build** (Godot's Android export must use Gradle for
   native plugins to link in).
2. Under the plugin's own export settings section, enable the AdMob
   export plugin and enter your **AdMob App ID** (from the AdMob
   console — Apps > your app > App settings). This is what gets written
   into `AndroidManifest.xml` as the
   `com.google.android.gms.ads.APPLICATION_ID` meta-data; AdMob will not
   serve ads without it, even with valid ad unit IDs.

## 3. Verify the plugin's actual API against AdManager.gd

`AdManager.gd` calls into the plugin defensively — every call goes
through `has_method()`/`has_signal()` checks and logs + no-ops instead of
crashing if something doesn't match. But "no-ops silently" means a
mismatched method name can look like "ads just don't show" with no error.
After installing, check the plugin's own README/example scene for its
exact method and signal names and compare against the strings used in:

- `AdManager._call_plugin(...)` calls — `load_banner_ad`,
  `show_banner_ad`, `hide_banner_ad`, `load_interstitial_ad`,
  `show_interstitial_ad`, `load_rewarded_ad`, `show_rewarded_ad`,
  `initialize`
- `AdManager._connect_plugin_signals()` — `on_interstitial_ad_loaded`,
  `on_interstitial_ad_failed_to_load`, `on_rewarded_ad_loaded`,
  `on_rewarded_ad_failed_to_load`, `on_user_earned_reward`,
  `on_rewarded_ad_failed_to_show`,
  `on_rewarded_ad_dismissed_full_screen_content`

Update the strings in that file if your installed version differs —
nothing else in the game needs to change, since every screen only ever
calls the small public API on `AdManager` (`show_banner`, `hide_banner`,
`notify_round_completed`, `show_rewarded`), never the plugin directly.

## 4. Replace the test ad unit IDs

`AdManager.gd` ships with Google's official AdMob **test** ad unit IDs
(safe to leave in during development — they always serve test creatives).
Before release, replace `BANNER_AD_UNIT_ID`, `INTERSTITIAL_AD_UNIT_ID`,
and `REWARDED_AD_UNIT_ID` with the real ad unit IDs from your AdMob
console (create one ad unit per format, per app).

## 5. Testing without a device

The plugin only exists as a native singleton on an actual Android
export — running the project in the editor on desktop, `Engine.has_singleton("Admob")`
is always false, so banner/interstitial calls are no-ops. Rewarded ads
are the exception: `AdManager.show_rewarded()` simulates a reward after a
short delay when no plugin is present, so you can test the "+5 seconds"
and "revive" flows in Gameplay from the editor. Set
`SIMULATE_ADS_WITHOUT_PLUGIN = false` in `AdManager.gd` if you'd rather
those calls fail outright without a real plugin.

## What's already wired up in-game

- **Banner** — shown on Home only (`Home._ready()` calls
  `AdManager.show_banner()`); every other screen calls
  `AdManager.hide_banner()` on entry.
- **Interstitial** — `Results._ready()` calls
  `AdManager.notify_round_completed()`, which shows an interstitial every
  2-3 completed rounds (randomized each time). Never triggered from
  Gameplay, so it can't interrupt a question.
- **Rewarded video** — two placements in Gameplay: "Watch Ad for +5s"
  (available while a question is active) and "Watch Ad to Revive Streak"
  (shown for ~3.5s after a wrong answer, restoring the streak GameStateManager
  would otherwise have reset). Both call `GameStateManager`'s existing
  `grant_extra_time()` / `revive()` hooks on a successful watch.
- **Remove Ads respected everywhere** — `AdManager.show_banner()` and
  `AdManager.notify_round_completed()` both check
  `SaveManager.has_ads_removed()` first. Rewarded video is unaffected by
  Remove Ads — it's opt-in, not an interruption.
