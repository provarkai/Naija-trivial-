extends Node
## ShareManager (autoload singleton)
##
## Generates a shareable score card image and opens WhatsApp with a
## pre-filled score message — per the brief, this is the primary organic
## growth channel, more effective than paid ads in this market, and a
## core system, not a polish item.
##
## TWO DIFFERENT LEVELS OF "WORKS" HERE:
##
## 1. WhatsApp TEXT share works today, with zero native plugins. It's
##    just OS.shell_open() on the `whatsapp://send?text=...` URL scheme,
##    which Android hands to WhatsApp (if installed) to open its chat
##    picker with the message pre-filled. This is the guaranteed
##    baseline and needs nothing installed.
##
## 2. The score CARD IMAGE is genuinely rendered and saved to
##    user://score_card.png (see _render_score_card()) — but the
##    `whatsapp://send` URL scheme only supports pre-filled text, not an
##    attached file. Auto-attaching the image to the outgoing message
##    needs a native Android share-sheet plugin (Intent.ACTION_SEND with
##    EXTRA_STREAM). SHARE_PLUGIN_SINGLETON_NAME below is a placeholder
##    name — there's no one obvious/official choice the way there is for
##    AdMob or Play Billing, so pick a "share sheet" addon, verify its
##    singleton/method names, and adjust here. See docs/SHARE_SETUP.md.
##    Without a plugin, the card is still saved locally and openable via
##    open_score_card_image(), which — depending on the device's default
##    image viewer/gallery app — often has its own Share button.

signal score_card_ready(image_path: String)
signal share_opened(method: String) # "plugin" | "whatsapp_text" | "clipboard_fallback"

const SCORE_CARD_PATH := "user://score_card.png"
const SCORE_CARD_SIZE := Vector2i(1080, 1080)

## Placeholder — see the class comment above. Verify against whatever
## share-sheet plugin you actually install.
const SHARE_PLUGIN_SINGLETON_NAME := "GodotShare"

## Fill in once the app has a real Play Store listing — appended to the
## share message so the "can you beat me?" loop leads somewhere.
const APP_STORE_LINK := ""

var _plugin: Object = null


func _ready() -> void:
	if Engine.has_singleton(SHARE_PLUGIN_SINGLETON_NAME):
		_plugin = Engine.get_singleton(SHARE_PLUGIN_SINGLETON_NAME)
	else:
		print("ShareManager: '%s' plugin singleton not found — sharing falls back to WhatsApp text-only (still fully functional)." % SHARE_PLUGIN_SINGLETON_NAME)


func is_plugin_available() -> bool:
	return _plugin != null


## Builds the share message and score card image for a completed round,
## then opens the native share sheet (if a plugin is installed) or
## WhatsApp with the text pre-filled (the guaranteed no-plugin path).
func share_score(summary: Dictionary) -> void:
	var text := _build_share_text(summary)
	var image_path := await _render_score_card(summary)
	score_card_ready.emit(image_path)

	if is_plugin_available():
		NativePluginBridge.call_plugin(_plugin, "ShareManager", "shareImage", [text, image_path])
		share_opened.emit("plugin")
		return

	_open_whatsapp_text(text)


## Opens the last-generated score card in the OS's default image viewer —
## a manual fallback for attaching it to a chat by hand when no share
## plugin is installed. Whether that viewer itself offers a Share button
## depends on the device.
func open_score_card_image() -> void:
	if not FileAccess.file_exists(SCORE_CARD_PATH):
		return
	OS.shell_open(ProjectSettings.globalize_path(SCORE_CARD_PATH))


func _build_share_text(summary: Dictionary) -> String:
	var category := "Daily Challenge" if summary.get("daily_challenge", false) else QuestionBank.get_category_display_name(summary.get("category", ""))
	var correct: int = summary.get("correct_count", 0)
	var total: int = summary.get("total_questions", 0)
	var streak: int = summary.get("longest_streak", 0)

	var lines := ["I scored %d/%d on %s in Naija Trivia Blitz! 🇳🇬🔥" % [correct, total, category]]
	if streak >= 3:
		lines.append("Best streak this round: %d in a row." % streak)
	lines.append("Think you can beat me?")
	if not APP_STORE_LINK.is_empty():
		lines.append(APP_STORE_LINK)

	return "\n".join(lines)


func _open_whatsapp_text(text: String) -> void:
	var err := OS.shell_open("whatsapp://send?text=%s" % text.uri_encode())
	if err == OK:
		share_opened.emit("whatsapp_text")
		return

	# WhatsApp isn't installed, or shell_open isn't supported here (e.g.
	# running in the editor on desktop) — put the text on the clipboard
	# rather than just losing it.
	DisplayServer.clipboard_set(text)
	share_opened.emit("clipboard_fallback")


## Renders a square score card (branding, category, score, streak) to
## user://score_card.png and returns its path via a throwaway SubViewport.
## Built in code rather than an authored scene — see the note at the top
## of scripts/ui/Home.gd on why every UI element in this project is.
func _render_score_card(summary: Dictionary) -> String:
	var viewport := SubViewport.new()
	viewport.size = SCORE_CARD_SIZE
	viewport.render_target_update_mode = SubViewport.UPDATE_ONCE
	viewport.transparent_bg = false
	add_child(viewport)

	var root := Control.new()
	root.set_anchors_preset(Control.PRESET_FULL_RECT)
	viewport.add_child(root)
	UIHelpers.add_background(root, Color(0.043, 0.180, 0.110))

	var vbox := VBoxContainer.new()
	vbox.set_anchors_preset(Control.PRESET_FULL_RECT)
	vbox.alignment = BoxContainer.ALIGNMENT_CENTER
	vbox.add_theme_constant_override("separation", 24)
	vbox.offset_left = 60
	vbox.offset_right = -60
	root.add_child(vbox)

	UIHelpers.add_title(vbox, "Naija Trivia Blitz", 44)

	var category := "Daily Challenge" if summary.get("daily_challenge", false) else QuestionBank.get_category_display_name(summary.get("category", ""))
	UIHelpers.add_title(vbox, category, 32)

	var score_label := UIHelpers.add_title(vbox, "%d / %d" % [summary.get("correct_count", 0), summary.get("total_questions", 0)], 72)
	score_label.modulate = Color(1, 0.85, 0.3)

	UIHelpers.add_title(vbox, "Best streak: %d 🔥" % summary.get("longest_streak", 0), 28)

	# Two frames: one for the freshly-added nodes to enter the tree, one
	# for the viewport to actually render before reading the texture back.
	await get_tree().process_frame
	await get_tree().process_frame

	var image := viewport.get_texture().get_image()
	image.save_png(SCORE_CARD_PATH)
	viewport.queue_free()

	return SCORE_CARD_PATH
