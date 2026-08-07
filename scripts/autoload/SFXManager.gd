extends Node
## SFXManager (autoload singleton)
##
## Short sound effects, synthesized in code at startup rather than loaded
## from audio files — there's nothing to source, license, or ship as
## binary assets for a first playable build. Swap for real SFX later by
## dropping files in assets/audio/ and changing _build_streams() to
## load() them instead; everything else (play(), respecting the sound
## setting) stays the same.

enum Sound { CLICK, CORRECT, WRONG, ROUND_COMPLETE, LEVEL_UP, STREAK }

const MIX_RATE := 22050
const PLAYER_POOL_SIZE := 4

var _streams: Dictionary = {}
var _players: Array[AudioStreamPlayer] = []


func _ready() -> void:
	_build_streams()
	for i in range(PLAYER_POOL_SIZE):
		var player := AudioStreamPlayer.new()
		add_child(player)
		_players.append(player)


func play(sound: Sound) -> void:
	if not SaveManager.data.get("settings", {}).get("sound_enabled", true):
		return
	var stream: AudioStream = _streams.get(sound)
	if stream == null:
		return
	var player := _next_free_player()
	player.stream = stream
	player.play()


func _next_free_player() -> AudioStreamPlayer:
	for player in _players:
		if not player.playing:
			return player
	return _players[0] # pool exhausted (unlikely for effects this short) — steal the first


# ---------------------------------------------------------------------------
# Synthesis
# ---------------------------------------------------------------------------

func _build_streams() -> void:
	_streams[Sound.CLICK] = _tone([880.0], 0.05, 0.15)
	_streams[Sound.CORRECT] = _tone([659.0, 880.0, 1109.0], 0.09, 0.25)
	_streams[Sound.WRONG] = _tone([300.0, 200.0], 0.12, 0.25)
	_streams[Sound.ROUND_COMPLETE] = _tone([523.0, 659.0, 784.0, 1046.0], 0.11, 0.3)
	_streams[Sound.LEVEL_UP] = _tone([523.0, 659.0, 784.0, 1046.0, 1318.0], 0.09, 0.3)
	_streams[Sound.STREAK] = _tone([784.0, 988.0], 0.07, 0.2)


## Builds a short sequence of sine-wave notes as a single 16-bit mono WAV
## stream. `note_duration` is per-note in seconds, `volume` is 0-1.
func _tone(frequencies: Array, note_duration: float, volume: float) -> AudioStreamWAV:
	var samples_per_note := int(MIX_RATE * note_duration)
	var data := PackedByteArray()
	data.resize(samples_per_note * frequencies.size() * 2) # 16-bit = 2 bytes/sample

	var sample_index := 0
	for freq in frequencies:
		for i in range(samples_per_note):
			# Linear fade-out per note avoids an audible click at the
			# boundary between notes (or at the very end of the stream).
			var envelope := 1.0 - float(i) / float(samples_per_note)
			var t := float(i) / float(MIX_RATE)
			var sample := sin(TAU * freq * t) * volume * envelope
			var value := int(clamp(sample, -1.0, 1.0) * 32767.0)
			data.encode_s16(sample_index * 2, value)
			sample_index += 1

	var stream := AudioStreamWAV.new()
	stream.data = data
	stream.format = AudioStreamWAV.FORMAT_16_BITS
	stream.mix_rate = MIX_RATE
	stream.stereo = false
	return stream
