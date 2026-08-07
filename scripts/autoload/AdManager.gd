extends Node
## AdManager (autoload singleton)
##
## Thin wrapper around the platform AdMob plugin so the rest of the game
## never touches ad-plugin APIs directly — it calls show_banner(),
## notify_round_completed(), and show_rewarded(), and doesn't care whether
## a real plugin is present.
##
## WHICH PLUGIN THIS TARGETS
## This wraps the "Admob" native singleton exposed by the Poing Studios
## Godot AdMob plugin (https://github.com/poing-studios/godot-admob-plugin,
## also on the Godot AssetLib) — the most widely used Godot 4 AdMob addon
## at time of writing. It is NOT installed by this scaffold: install it
## yourself (AssetLib or manual addon), enable it under Project Settings >
## Plugins, and configure your Android export preset per its README
## (Gradle build enabled, AdMob export plugin enabled, AdMob App ID set).
## See docs/ADMOB_SETUP.md.
##
## The plugin only exists as a native singleton on an actual Android
## export — never in the editor on desktop, and never if the plugin isn't
## installed/enabled. Every call into it goes through _call_plugin() /
## _safe_connect(), which check has_method()/has_signal() first and log +
## no-op instead of crashing when something doesn't match. Method and
## signal names below are this plugin's documented API as of writing;
## re-check them against whatever version you actually install — AdMob
## plugin APIs change between releases and this file cannot be verified
## against a real install from here.
##
## Rewarded ads specifically fall back to a simulated reward (after a
## short delay) whenever the plugin isn't present, so the "+5 seconds" /
## "revive" flow in Gameplay is testable from the editor without a device.

signal interstitial_shown
signal rewarded_earned(placement: String)
signal rewarded_failed(placement: String)

const PLUGIN_SINGLETON_NAME := "Admob"

# Google's official AdMob TEST ad unit IDs (Android). Safe to ship as-is
# during development — they always serve test creatives and never earn or
# risk real money. Replace with your own ad unit IDs from the AdMob
# console before release.
const BANNER_AD_UNIT_ID := "ca-app-pub-3940256099942544/6300978111"
const INTERSTITIAL_AD_UNIT_ID := "ca-app-pub-3940256099942544/1033173712"
const REWARDED_AD_UNIT_ID := "ca-app-pub-3940256099942544/5224354917"

# "Every 2-3 completed rounds" per the monetization brief.
const INTERSTITIAL_MIN_ROUNDS := 2
const INTERSTITIAL_MAX_ROUNDS := 3

const SIMULATE_ADS_WITHOUT_PLUGIN := true
const SIMULATED_AD_WATCH_SECONDS := 1.0

var _plugin: Object = null
var _interstitial_loaded := false
var _rewarded_loaded := false

var _rounds_since_interstitial := 0
var _next_interstitial_threshold := INTERSTITIAL_MIN_ROUNDS

var _pending_reward_placement := ""
var _pending_reward_callback: Callable
var _pending_failed_callback: Callable


func _ready() -> void:
	_reroll_interstitial_threshold()

	if Engine.has_singleton(PLUGIN_SINGLETON_NAME):
		_plugin = Engine.get_singleton(PLUGIN_SINGLETON_NAME)
		_connect_plugin_signals()
		# TODO: verify this against your installed plugin version's actual
		# initialize() signature (some versions take
		# is_real_device/is_for_family/tag_for_child/tag_under_consent/
		# max_ad_content_rating args, others take none and read config
		# from the Android export plugin settings instead).
		_call_plugin("initialize", [])
		_load_interstitial()
		_load_rewarded()
	else:
		print("AdManager: '%s' plugin singleton not found — ads are disabled (expected outside an Android export with the plugin installed)." % PLUGIN_SINGLETON_NAME)


func is_plugin_available() -> bool:
	return _plugin != null


# ---------------------------------------------------------------------------
# Banner — persistent on Home only, never during gameplay
# ---------------------------------------------------------------------------

func show_banner() -> void:
	if SaveManager.has_ads_removed():
		return
	_call_plugin("load_banner_ad", [BANNER_AD_UNIT_ID])
	_call_plugin("show_banner_ad", [])


func hide_banner() -> void:
	_call_plugin("hide_banner_ad", [])


# ---------------------------------------------------------------------------
# Interstitial — every 2-3 completed rounds, never mid-question
# ---------------------------------------------------------------------------

## Call once per completed round (Results screen, after the round is
## fully over) — never from Gameplay. Internally decides whether this
## round crosses the 2-3-round threshold and shows the ad if so.
func notify_round_completed() -> void:
	if SaveManager.has_ads_removed():
		return
	_rounds_since_interstitial += 1
	if _rounds_since_interstitial >= _next_interstitial_threshold:
		_show_interstitial()


func _show_interstitial() -> void:
	_rounds_since_interstitial = 0
	_reroll_interstitial_threshold()

	if not is_plugin_available() or not _interstitial_loaded:
		return # nothing loaded to show; we'll catch the next eligible round

	_call_plugin("show_interstitial_ad", [])
	_interstitial_loaded = false
	interstitial_shown.emit()
	_load_interstitial() # preload the next one


func _load_interstitial() -> void:
	_call_plugin("load_interstitial_ad", [INTERSTITIAL_AD_UNIT_ID])


func _reroll_interstitial_threshold() -> void:
	_next_interstitial_threshold = randi_range(INTERSTITIAL_MIN_ROUNDS, INTERSTITIAL_MAX_ROUNDS)


# ---------------------------------------------------------------------------
# Rewarded video — "+5 seconds" / "revive after wrong answer"
# ---------------------------------------------------------------------------

## Requests a rewarded ad for `placement` (a free-form label used only for
## the rewarded_earned/rewarded_failed signals and logging — e.g.
## "extra_time" or "revive"). Calls on_reward() if the player watched to
## completion and earned the reward, on_failed() otherwise (unavailable,
## dismissed early, load failure). Removing ads via IAP does not disable
## rewarded video — it's opt-in, not an interruption.
func show_rewarded(placement: String, on_reward: Callable, on_failed: Callable = Callable()) -> void:
	if is_plugin_available() and _rewarded_loaded:
		_pending_reward_placement = placement
		_pending_reward_callback = on_reward
		_pending_failed_callback = on_failed
		_call_plugin("show_rewarded_ad", [])
		_rewarded_loaded = false
		return

	if SIMULATE_ADS_WITHOUT_PLUGIN:
		print("AdManager: simulating rewarded ad for '%s' (no plugin installed)." % placement)
		await get_tree().create_timer(SIMULATED_AD_WATCH_SECONDS).timeout
		rewarded_earned.emit(placement)
		on_reward.call()
		return

	rewarded_failed.emit(placement)
	if on_failed.is_valid():
		on_failed.call()


func _load_rewarded() -> void:
	_call_plugin("load_rewarded_ad", [REWARDED_AD_UNIT_ID])


func _resolve_pending_reward(earned: bool) -> void:
	var placement := _pending_reward_placement
	var reward_cb := _pending_reward_callback
	var failed_cb := _pending_failed_callback
	_pending_reward_placement = ""
	_pending_reward_callback = Callable()
	_pending_failed_callback = Callable()

	if earned:
		rewarded_earned.emit(placement)
		if reward_cb.is_valid():
			reward_cb.call()
	else:
		rewarded_failed.emit(placement)
		if failed_cb.is_valid():
			failed_cb.call()


# ---------------------------------------------------------------------------
# Plugin signal wiring
# ---------------------------------------------------------------------------

func _connect_plugin_signals() -> void:
	_safe_connect("on_interstitial_ad_loaded", func(_id = null): _interstitial_loaded = true)
	_safe_connect("on_interstitial_ad_failed_to_load", func(_err = null): _interstitial_loaded = false)
	_safe_connect("on_rewarded_ad_loaded", func(_id = null): _rewarded_loaded = true)
	_safe_connect("on_rewarded_ad_failed_to_load", _on_plugin_rewarded_failed_to_load)
	_safe_connect("on_user_earned_reward", func(_amount = null, _type = null): _resolve_pending_reward(true))
	_safe_connect("on_rewarded_ad_failed_to_show", func(_err = null): _resolve_pending_reward(false))
	_safe_connect("on_rewarded_ad_dismissed_full_screen_content", func(): _load_rewarded())


func _on_plugin_rewarded_failed_to_load(_err = null) -> void:
	_rewarded_loaded = false
	_load_rewarded()


func _safe_connect(signal_name: String, handler: Callable) -> void:
	NativePluginBridge.safe_connect(_plugin, "AdManager", signal_name, handler)


func _call_plugin(method: String, args: Array):
	return NativePluginBridge.call_plugin(_plugin, "AdManager", method, args)
