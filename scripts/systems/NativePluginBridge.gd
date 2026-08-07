class_name NativePluginBridge
extends RefCounted
## Small shared helper for talking to native Android plugin singletons
## (AdMob, Play Billing, ...) defensively. Every call or signal connection
## goes through here, guarded with has_method()/has_signal() checks, so a
## missing plugin or a version with a slightly different API degrades to a
## logged no-op instead of crashing. Used by AdManager and IAPManager —
## reach for this again for any future native plugin integration.


static func safe_connect(plugin: Object, log_prefix: String, signal_name: String, handler: Callable) -> void:
	if plugin == null:
		return
	if plugin.has_signal(signal_name):
		plugin.connect(signal_name, handler)
	else:
		print("%s: plugin has no signal '%s' — skipping (check plugin version)." % [log_prefix, signal_name])


static func call_plugin(plugin: Object, log_prefix: String, method: String, args: Array = []):
	if plugin == null:
		return null
	if not plugin.has_method(method):
		print("%s: plugin has no method '%s' — skipping (check plugin version)." % [log_prefix, method])
		return null
	return plugin.callv(method, args)
