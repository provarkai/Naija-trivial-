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
- ✅ `versionCode`/`versionName` set to `2` / `1.0.0` (bumped from `1` after
  Play rejected a duplicate version code on the first upload attempt).
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

### 6. Upload the build
Play Console → your app → **Testing → Internal testing** (start here, not
straight to Production) → **Create new release** → upload
`app-release.aab` → fill in release notes → save → review → roll out to
internal testers.

Install it on a real device via the internal testing link and click through
every screen once — this sandbox has no emulator (no hardware
virtualization available), so this build has been verified by compiling,
signing, and a jarsigner integrity check, but **not** by actually running
on a device. Do that before wider rollout.

### 7. Store listing
Short description (≤80 chars), full description, app icon (512×512),
feature graphic (1024×500), and 2+ screenshots. Take screenshots from the
internal test install.

### 8. Promote to Production
Once internal testing looks good, Play Console lets you promote the same
release to Closed testing, Open testing, or Production without
re-uploading.

## Known gaps worth fixing before a public (not just internal-test) release

- **Subscription screen** shows real-looking prices with no working
  purchase flow (just a "coming soon" snackbar) — fine for internal
  testing, but decide whether to gate it behind a flag or wire up real
  Play Billing before a public launch, since showing prices with no
  purchase path can read as broken to reviewers/users.
- **Google Sign-In** button is a stub — same consideration.
- No crash reporting is wired in, so you won't hear about crashes from real
  users automatically; consider adding Firebase Crashlytics (or similar)
  before a wide public release.
- **Ads are unconditional for everyone right now** — the free-tier daily
  limit (5/day) and ad-gating apply to all users regardless of what the
  (non-functional) Subscription screen shows. Once real billing exists,
  wire premium/subscribed users to skip ads and the usage cap entirely
  (`UsageRepository`/`AppContainer` are where that check would go).
- **Content rating questionnaire**: answer "Yes" to showing ads when
  asked, and expect a follow-up question about ad content control — AdMob
  serves general-audience ads by default, which is fine for this app's
  target audience.
