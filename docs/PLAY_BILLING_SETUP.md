# Google Play Billing Setup

`scripts/autoload/IAPManager.gd` wraps a Play Billing plugin behind
`purchase(product_id)`, `restore_purchases()`, and
`get_price_string(product_id)` — it contains no billing SDK itself.
Without a real plugin installed and connected, purchases are **simulated
locally** (granted instantly after a short delay) so the Store screen is
fully testable from the editor. This doc is the manual setup that can't
be done from here.

## 1. Install the plugin

IAPManager targets the **official Godot Play Billing plugin**
(https://github.com/godotengine/godot-play-billing), maintained under the
Godot Engine org and the standard choice for Play Billing on Godot 4.
Install it via:

- Godot's AssetLib tab (search "Play Billing"), or
- Manually: download the release and drop its `addons/godot-play-billing/`
  folder into this project's `addons/`.

Enable it under **Project Settings > Plugins**.

If you use a different Billing plugin, everything downstream still works
the same way — adjust the method/signal name strings in
`IAPManager._connect_plugin_signals()` and the `_call_plugin(...)` calls
to match that plugin's API.

## 2. Configure the Android export preset

In **Project > Export**, on your Android preset:

1. Enable **Gradle Build**.
2. Enable the Play Billing export plugin under its section of the export
   preset. No app-level config value is needed here (unlike AdMob's App
   ID) — Billing is tied to your Play Console listing and signing key,
   not anything in the manifest.
3. The app must be **signed with your upload/release key and uploaded to
   at least the Internal Testing track** in Play Console before real
   purchases (even test ones) will work — Play Billing does not work
   against arbitrary debug builds the way AdMob test ads do. Add your own
   Google account as a **license tester** (Play Console > Setup > License
   testing) to make test purchases that don't charge real money.

## 3. Create matching products in Play Console

Product IDs in code must match Play Console **exactly**. IAPManager's
catalog is built from:

| Product ID | Type | Maps to |
|---|---|---|
| `remove_ads` | Managed product (one-time) | `SaveManager.set_ads_removed(true)` |
| `pro_bundle` | Managed product (one-time) | `SaveManager.set_pro_bundle_owned(true)` |
| `pack_<category_id>` — one per category with `"premium": true` in `data/categories.json` (currently `pack_nigerian_history`, `pack_sports`, `pack_pidgin_proverbs`) | Managed product (one-time) | `SaveManager.unlock_category_pack(category_id)` |

These are built **dynamically** from `data/categories.json`'s `premium`
flags in `IAPManager._ready()` — add a new premium category there and its
`pack_<id>` product ID is picked up automatically; you still have to
create the matching product in Play Console by hand.

All of these are **managed products** ("inapp"), not subscriptions —
they're one-time unlocks, matching the brief's pricing:

- Remove Ads — ₦500-₦1,000
- Single category pack — ₦300-₦500
- Pro Bundle — ₦1,500

## 4. Verify the plugin's actual API against IAPManager.gd

Like AdManager, every call goes through `NativePluginBridge`
(`scripts/systems/NativePluginBridge.gd`), which checks
`has_method()`/`has_signal()` and logs + no-ops instead of crashing on a
mismatch — which also means a wrong method/signal name can silently look
like "purchases just don't work." After installing, check the plugin's
own README/example against the names used in `IAPManager.gd`:

- Methods: `startConnection`, `purchase`, `queryPurchases`,
  `querySkuDetails`, `acknowledgePurchase`
- Signals: `connected`, `disconnected`, `connect_error`,
  `purchases_updated`, `purchase_error`, `query_purchases_response`,
  `sku_details_query_completed`, `sku_details_query_error`
- Purchase result shape: `IAPManager._apply_purchase()` expects a
  dictionary with `products` (Array, Billing v4+) or `sku` (String,
  older), `is_acknowledged`, and `purchase_token`. Confirm this against
  what your installed version actually emits.

## 5. Testing without a device

`Engine.has_singleton("GodotGooglePlayBilling")` is only ever true inside
an actual Android build signed and uploaded per step 2 above — never in
the editor. Everywhere else, `IAPManager.purchase()` simulates a
successful purchase after a short delay and grants the entitlement
locally, so the Store screen, category unlocks, and ad-removal are all
testable without Play Console setup. Set
`SIMULATE_PURCHASES_WITHOUT_PLUGIN = false` in `IAPManager.gd` if you'd
rather those calls fail outright without a real plugin.

## What's already wired up in-game

- **Store screen** (`scenes/Store.tscn`) — one button per product,
  showing `IAPManager.get_price_string()` (the plugin's localized price
  once queried, the brief's Naira range as a placeholder until then),
  disabled once owned per `SaveManager`.
- **Restore Purchases** — wired on both the Store and Settings screens to
  `IAPManager.restore_purchases()`.
- **Ownership stays in `SaveManager`** — `IAPManager` never tracks what's
  owned itself, it only turns a successful purchase into the matching
  `SaveManager` call. Every other screen (Home's category grid,
  `AdManager`'s banner/interstitial checks) already reads ownership from
  `SaveManager`, so nothing else needs to change as Billing goes from
  simulated to real.
