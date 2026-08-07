class_name UIHelpers
extends RefCounted
## Small procedural-UI builder shared by every screen script.
##
## Every screen in this project builds its Control tree in code (see the
## note at the top of scripts/ui/Home.gd for why), so this factors out the
## bits that would otherwise be copy-pasted into each one: a full-rect
## background, a full-rect padded VBoxContainer, and common widget presets.

const BACKGROUND_COLOR := Color(0.043, 0.180, 0.110) # deep green


static func add_background(parent: Control, color: Color = BACKGROUND_COLOR) -> ColorRect:
	var bg := ColorRect.new()
	bg.set_anchors_preset(Control.PRESET_FULL_RECT)
	bg.color = color
	parent.add_child(bg)
	return bg


## A full-rect VBoxContainer with screen-edge padding, the layout every
## screen uses as its root content container.
static func add_screen_vbox(parent: Control, separation: int = 16, margin: int = 40, top_margin: int = 80) -> VBoxContainer:
	var vbox := VBoxContainer.new()
	vbox.set_anchors_preset(Control.PRESET_FULL_RECT)
	vbox.add_theme_constant_override("separation", separation)
	vbox.offset_left = margin
	vbox.offset_right = -margin
	vbox.offset_top = top_margin
	vbox.offset_bottom = -margin
	vbox.alignment = BoxContainer.ALIGNMENT_BEGIN
	parent.add_child(vbox)
	return vbox


static func add_title(parent: Control, text: String, font_size: int = 36) -> Label:
	var label := Label.new()
	label.text = text
	label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	label.add_theme_font_size_override("font_size", font_size)
	parent.add_child(label)
	return label


static func add_label(parent: Control, text: String = "", centered: bool = true) -> Label:
	var label := Label.new()
	label.text = text
	if centered:
		label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	parent.add_child(label)
	return label


## Every button built through here gets a click sound and a small press
## bounce for free — one place to wire tactile feedback into the whole
## app instead of every screen remembering to do it itself.
static func add_button(parent: Control, text: String, min_height: int = 64) -> Button:
	var button := Button.new()
	button.text = text
	button.custom_minimum_size = Vector2(0, min_height)
	button.pressed.connect(func():
		SFXManager.play(SFXManager.Sound.CLICK)
		bounce(button)
	)
	parent.add_child(button)
	return button


## A quick scale-down-then-back "pop" — used automatically by every
## button from add_button(), and callable directly on any other Control
## for the same feedback (e.g. a whole results card on a big moment).
static func bounce(control: Control, amount: float = 0.08, duration: float = 0.09) -> void:
	control.pivot_offset = control.size / 2.0
	var tween := control.create_tween()
	tween.tween_property(control, "scale", Vector2.ONE * (1.0 - amount), duration * 0.5)
	tween.tween_property(control, "scale", Vector2.ONE, duration * 0.5)


## A visual spacer for pushing content apart inside a VBoxContainer without
## depending on size-flags trickery.
static func add_spacer(parent: Control, min_height: int = 16) -> Control:
	var spacer := Control.new()
	spacer.custom_minimum_size = Vector2(0, min_height)
	parent.add_child(spacer)
	return spacer


## Loads a category's icon (data/categories.json's "icon" field) for use
## as a Button.icon or in a TextureRect. Returns null — never errors — if
## the field is missing or the file doesn't exist, so callers can assign
## it straight to `button.icon` unconditionally; a null icon just means
## no icon renders. The source SVGs declare a small width/height (40x40)
## with a 128x128 viewBox, so Godot's default SVG import rasterizes them
## already icon-sized — no runtime scaling needed here.
static func load_category_icon(category: Dictionary) -> Texture2D:
	var path: String = category.get("icon", "")
	if path.is_empty() or not ResourceLoader.exists(path):
		return null
	return load(path)
