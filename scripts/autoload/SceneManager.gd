extends Node
## SceneManager (autoload singleton)
##
## Centralizes scene file paths (so screens reference SceneManager.HOME
## instead of hardcoding res:// strings for each other) and provides a
## small back-stack for screens that can be reached from more than one
## place (Store, Settings) so their Back button returns wherever the
## player actually came from. Also owns a simple fade-to-black transition
## between every scene change, via a CanvasLayer that lives here (not in
## any individual screen) since it needs to persist across the scene
## swap it's covering up.
##
## Screens reached through a single well-defined flow (Home -> Gameplay ->
## Results, Home -> Daily Challenge) navigate directly with
## remember_current = false instead of pushing onto the stack — that flow
## doesn't need a generic "back", each screen already knows where it goes.

const HOME := "res://scenes/Home.tscn"
const GAMEPLAY := "res://scenes/Gameplay.tscn"
const RESULTS := "res://scenes/Results.tscn"
const DAILY_CHALLENGE := "res://scenes/DailyChallenge.tscn"
const STORE := "res://scenes/Store.tscn"
const SETTINGS := "res://scenes/Settings.tscn"
const LEADERBOARD := "res://scenes/Leaderboard.tscn"

const FADE_DURATION := 0.15

var _back_stack: Array[String] = []
var _fade_rect: ColorRect


func _ready() -> void:
	var fade_layer := CanvasLayer.new()
	fade_layer.layer = 100 # above every screen's own content
	add_child(fade_layer)

	_fade_rect = ColorRect.new()
	_fade_rect.color = Color(0, 0, 0, 0)
	_fade_rect.set_anchors_preset(Control.PRESET_FULL_RECT)
	_fade_rect.mouse_filter = Control.MOUSE_FILTER_IGNORE
	fade_layer.add_child(_fade_rect)


func goto_scene(path: String, remember_current: bool = false) -> void:
	if remember_current:
		var current := get_tree().current_scene
		if current != null and not current.scene_file_path.is_empty():
			_back_stack.append(current.scene_file_path)

	await _fade_to(1.0)
	get_tree().change_scene_to_file(path)
	# change_scene_to_file() is deferred, not immediate — give it a couple
	# of frames to actually swap and run the new scene's _ready() before
	# fading back in, so the fade-in doesn't reveal a half-built screen.
	await get_tree().process_frame
	await get_tree().process_frame
	await _fade_to(0.0)


func go_back() -> void:
	if _back_stack.is_empty():
		goto_scene(HOME)
		return
	goto_scene(_back_stack.pop_back())


func _fade_to(alpha: float) -> void:
	var tween := create_tween()
	tween.tween_property(_fade_rect, "color:a", alpha, FADE_DURATION)
	await tween.finished
