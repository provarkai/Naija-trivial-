extends Node
## SceneManager (autoload singleton)
##
## Centralizes scene file paths (so screens reference SceneManager.HOME
## instead of hardcoding res:// strings for each other) and provides a
## small back-stack for screens that can be reached from more than one
## place (Store, Settings) so their Back button returns wherever the
## player actually came from.
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

var _back_stack: Array[String] = []


func goto_scene(path: String, remember_current: bool = false) -> void:
	if remember_current:
		var current := get_tree().current_scene
		if current != null and not current.scene_file_path.is_empty():
			_back_stack.append(current.scene_file_path)
	get_tree().change_scene_to_file(path)


func go_back() -> void:
	if _back_stack.is_empty():
		goto_scene(HOME)
		return
	goto_scene(_back_stack.pop_back())
