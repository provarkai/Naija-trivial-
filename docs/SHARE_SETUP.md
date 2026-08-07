# Share-Score Setup

`scripts/autoload/ShareManager.gd` generates a score card image and opens
a share flow from the Results screen. Unlike AdMob/Billing, **the core of
this already works with zero plugins installed** — read this before
assuming it needs setup.

## What works today, out of the box

Tapping "Share Score to WhatsApp" on Results:

1. Renders a branded 1080×1080 score card (category, score, best streak)
   to `user://score_card.png`.
2. Opens WhatsApp with the score pre-filled as text, via
   `OS.shell_open("whatsapp://send?text=...")`. This is a URL scheme
   Android hands to WhatsApp directly if it's installed — not a plugin
   API, just a standard Godot call. If WhatsApp isn't installed (or this
   is running in the editor on desktop, where `shell_open` doesn't apply
   the same way), the text is put on the clipboard instead so it isn't
   just lost.

This is the primary organic-growth loop from the brief, and it's live
right now — no addon install needed.

## What needs a plugin: auto-attaching the image

The `whatsapp://send` URL scheme only supports pre-filled **text**, not
an attached file — WhatsApp's URL scheme doesn't expose a way to attach
an image. Auto-attaching the generated score card to the outgoing
message needs Android's native `Intent.ACTION_SEND` with
`EXTRA_STREAM`, which means a native share-sheet plugin.

Unlike AdMob or Play Billing, there isn't one obvious standard choice
here — pick a "share sheet" Godot addon (search the AssetLib for
"share"), then:

1. Install and enable it under Project Settings > Plugins.
2. Check its actual singleton name and method signature for
   image+text sharing, and update `ShareManager.SHARE_PLUGIN_SINGLETON_NAME`
   and the `"shareImage"` call in `ShareManager.share_score()` to match —
   both are placeholders right now, verified against nothing, since there
   was no single canonical plugin to target the way there was for AdMob
   or Billing.
3. Once installed, `ShareManager` picks it up automatically —
   `is_plugin_available()` gates the plugin path vs. the WhatsApp-text
   fallback, same defensive pattern as `AdManager`/`IAPManager` (guarded
   through `NativePluginBridge`, logs + no-ops on a mismatched API rather
   than crashing).

## The manual fallback in the meantime

Even without a share plugin, the score card image is genuinely rendered
and saved to `user://score_card.png` — `ShareManager.open_score_card_image()`
opens it in the device's default image viewer via `OS.shell_open()`,
which on many Android devices' gallery/photos apps has its own Share
button. This isn't wired to a Results screen button yet (only the
one-tap WhatsApp text flow is) — add a "View Score Card" button calling
it if you want that manual path exposed in the UI.

## Fill in before launch

- `ShareManager.APP_STORE_LINK` — empty until the app has a real Play
  Store listing; once set, it's appended to every share message so
  "can you beat me?" leads somewhere.
