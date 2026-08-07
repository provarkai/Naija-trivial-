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


static func add_button(parent: Control, text: String, min_height: int = 64) -> Button:
	var button := Button.new()
	button.text = text
	button.custom_minimum_size = Vector2(0, min_height)
	parent.add_child(button)
	return button


## A visual spacer for pushing content apart inside a VBoxContainer without
## depending on size-flags trickery.
static func add_spacer(parent: Control, min_height: int = 16) -> Control:
	var spacer := Control.new()
	spacer.custom_minimum_size = Vector2(0, min_height)
	parent.add_child(spacer)
	return spacer
