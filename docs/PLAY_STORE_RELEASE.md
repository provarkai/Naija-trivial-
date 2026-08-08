# Play Store release guide

## What's already done (in this repo)

- ✅ Release signing configured (`app/build.gradle.kts` reads `keystore.properties`,
  which is gitignored — the actual key was generated and handed to you
  separately, **not** committed).
- ✅ R8 minification + resource shrinking enabled for release builds.
- ✅ A signed release AAB has been built and verified
  (`app/build/outputs/bundle/release/app-release.aab`) — this build artifact
  itself isn't committed to git (build outputs never are); rebuild it
  anytime with `./gradlew bundleRelease` once `keystore.properties` is in
  place locally.
- ✅ `versionCode` bumped to `4` (`1`–`3` were consumed by earlier upload
  attempts — Play never lets you reuse a version code, even for a
  rejected/draft upload).
- ✅ `compileSdk`/`targetSdk` bumped to `35` (Play now requires targeting
  API 35 minimum; the first build targeted 34 and was rejected).
- ✅ Privacy policy drafted (`docs/PRIVACY_POLICY.md`) — you've already
  filled in the date and contact email.
- ✅ AdMob wired in: banner ad on Home, an interstitial shown every 3rd
  generation, and a rewarded ad to unlock +1 generation once the daily free
  limit (5/day) is hit. Real AdMob IDs are configured via
  `local.properties` (gitignored); debug builds always use Google's test
  ad IDs regardless, so ads are never accidentally served/clicked during
  development.
- ✅ Google Play Billing wired in: the Subscription screen now launches
  real purchase flows (`BillingManager`), and premium subscribers
  (`BillingManager.isPremium`) skip both ads and the daily free-generation
  limit entirely — see "Set up billing products" below, since this needs
  matching products created in Play Console before it can actually work.
- ✅ Server-side purchase verification wired in (`/server`'s
  `/api/verify-purchase`, backed by the Google Play Developer API) — every
  purchase is checked against Google before `isPremium` is trusted, so a
  patched client can no longer fake premium status just by lying to
  `BillingClient`. Needs a Google Cloud service account granted access in
  Play Console — see "Set up server-side purchase verification" below.

## What only you can do

### 1. Play Console account
Create one at https://play.google.com/console ($25 one-time fee). Identity
verification for new accounts can take a few days — start this early.

### 2. Host the privacy policy
Fill in the placeholders in `docs/PRIVACY_POLICY.md`, publish it somewhere
public (GitHub Pages is free and simple: enable Pages on this repo pointing
at `/docs`, and the policy will be at
`https://<you>.github.io/<repo>/PRIVACY_POLICY`), and keep the URL handy.

### 3. Create the app in Play Console
"Create app" → fill in name, default language, app/game type, free/paid.
`com.ai4biz.app` is the package name baked into this build — **it can never
change** after your first upload, so confirm you're happy with it now.

### 4. App content section
Play Console → App content, and fill in:

- **Privacy policy**: the URL from step 2.
- **App access**: since there's no login wall blocking core features
  (guest mode works), you can likely mark it as fully accessible; if any
  reviewer account is needed, note that guest mode requires no credentials.
- **Ads**: This build **does** show ads (AdMob: banner, interstitial,
  rewarded) → declare "Yes, my app contains ads".
- **Content rating**: fill in the questionnaire — this app has no violence,
  no user-generated content shared *between* users (it's private,
  per-device AI output), no gambling. Should land on the lowest rating tier
  in most regions.
- **Target audience**: not designed for children; select an adult-oriented
  business-tool audience.
- **Data safety**: use the table below.

### 5. Data Safety form — what to answer

Based on what the app's code actually does today:

| Data type | Collected? | Shared with 3rd party? | Purpose | Notes |
|---|---|---|---|---|
| Email address | Yes, *optional* | No | Account management | Only if user picks "Continue with Email"; stored on-device only, never transmitted |
| User-generated content (text you type into a generator form) | Yes | **Yes** — sent to OpenRouter to generate the response | App functionality | Not stored server-side after the response is returned |
| Advertising ID | Yes | **Yes** — Google AdMob | Advertising, analytics | Standard for any app showing AdMob ads; Play auto-detects this from the AdMob SDK, so leaving it undeclared will fail Play's automated check |
| App activity / crash logs | No | — | — | No analytics or crash reporting SDK is integrated (AdMob's own ad performance analytics are separate from this) |
| Location, contacts, photos | No | — | — | Not requested/accessed |

Also answer:
- **Is data encrypted in transit?** Yes (HTTPS to the backend and to
  OpenRouter).
- **Can users request data deletion?** Yes — in-app, per document
  (History screen), or by uninstalling (no server-side account exists to
  delete separately).

If you change the AI backend, add analytics, or wire up real billing later,
**update this form to match** — Play actively checks for mismatches
between declared and observed behavior.

### 6. Set up billing products
Billing won't work until these exist in Play Console, with these **exact**
product IDs (hardcoded in `app/src/main/java/com/ai4biz/app/billing/PlanId.kt`):

Play Console → your app → **Monetize → Products**:

- **Subscriptions** → Create subscription:
  - Product ID: `ai4biz_monthly` — set your monthly price, base plan billing period 1 month.
  - Product ID: `ai4biz_annual` — same, billing period 1 year.
- **In-app products** → Create product:
  - Product ID: `ai4biz_lifetime` — one-time purchase, set your price.

Each needs to be **Activated** (not left as a draft) before purchases work.
Prices shown in the app come live from what you set here (`BillingManager.priceFor`)
— there's no price hardcoded in the app.

**Testing purchases without spending real money:** add yourself as a
[License Tester](https://support.google.com/googleplay/android-developer/answer/6062777)
in Play Console → Setup → License testing, then buy through the internal
testing build — you'll see a test payment method, not a real charge.

### 7. Set up server-side purchase verification

Without this, `/api/verify-purchase` always fails and the app quietly
falls back to trusting Play Billing's client-side result only (see
`ClientOnlyPurchaseVerifier`) — purchases still work, they're just not
protected against a patched client. To turn on the real check:

1. **Link a Google Cloud project to Play Console** (if you haven't):
   Play Console → Setup → **API access** → follow the prompt to create/link
   a Google Cloud project.
2. **Create a service account**: on that same API access page, click
   "Create new service account" — it walks you to Google Cloud Console with
   the project pre-selected. In Cloud Console: IAM & Admin → Service
   Accounts → Create. No project-level role is needed there; access is
   granted back in Play Console (next step).
3. **Create a JSON key** for that service account: Cloud Console → your
   service account → Keys → Add Key → JSON. This downloads a file — treat
   it like a password, it's a credential.
4. **Grant it access in Play Console**: back on the API access page, find
   the new service account → Grant Access. Under app permissions, grant at
   least **"View financial data, orders, and cancellation survey
   responses"** for this app (that's what `purchases.subscriptions.get`
   and `purchases.products.get` need). Invite/send.
5. **Give the key to your server**, base64-encoded so it survives as a
   single-line env var:
   ```bash
   base64 -w0 path/to/service-account-key.json
   ```
   Set the result as `GOOGLE_SERVICE_ACCOUNT_JSON_BASE64` on your server
   host (Fly: `fly secrets set GOOGLE_SERVICE_ACCOUNT_JSON_BASE64=...`).
   Never commit the raw JSON file or this env var value.
6. Redeploy `/server`. Test with `curl -X POST .../api/verify-purchase`
   (see `server/README.md`) using a real purchase token from a test
   purchase (step 6 above, License Testers) — you should get back
   `{"valid":true,...}` instead of an error.

### 8. Upload the build
Play Console → your app → **Testing → Internal testing** (start here, not
straight to Production) → **Create new release** → upload
`app-release.aab` → fill in release notes → save → review → roll out to
internal testers.

Install it on a real device via the internal testing link and click through
every screen once — this sandbox has no emulator (no hardware
virtualization available), so this build has been verified by compiling,
signing, and a jarsigner integrity check, but **not** by actually running
on a device. Do that before wider rollout.

### 9. Store listing
Short description (≤80 chars), full description, app icon (512×512),
feature graphic (1024×500), and 2+ screenshots. Take screenshots from the
internal test install.

### 10. Promote to Production
Once internal testing looks good, Play Console lets you promote the same
release to Closed testing, Open testing, or Production without
re-uploading.

## Known gaps worth fixing before a public (not just internal-test) release

- **Billing verification degrades silently if misconfigured.** If
  `GOOGLE_SERVICE_ACCOUNT_JSON_BASE64` is never set, the server always
  fails verification and `isPremium` falls back to trusting Play Billing's
  client-side result alone (`ClientOnlyPurchaseVerifier`) — purchases still
  work, just without the anti-tampering check. Not dangerous, just worth
  knowing: check `/server`'s logs for "GOOGLE_SERVICE_ACCOUNT_JSON" errors
  if you expect verification to be active. Consider also adding [Real-time
  Developer
  Notifications](https://developer.android.com/google/play/billing/rtdn-reference)
  so cancellations/refunds revoke access immediately rather than waiting
  for the next app-open refresh.
- **Billing products must exist in Play Console before purchases work** —
  see "Set up billing products" above. Until they're created and activated,
  `priceFor()` returns null and the buttons show "This plan isn't available
  yet."
- **Google Sign-In** button is still a stub (email/guest sign-in is the
  real path) — decide whether that's fine for launch or wire up real OAuth
  first.
- No crash reporting is wired in, so you won't hear about crashes from real
  users automatically; consider adding Firebase Crashlytics (or similar)
  before a wide public release.
- **Content rating questionnaire**: answer "Yes" to showing ads when
  asked, and expect a follow-up question about ad content control — AdMob
  serves general-audience ads by default, which is fine for this app's
  target audience.
