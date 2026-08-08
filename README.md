# Business Edge AI (Ai4biz)

The AI operating system for entrepreneurs, freelancers, and SMEs — AI writing,
automation, finance, marketing, sales, customer support, and analytics in one
Android app. This repo is currently scaffolded to **Phase 1 (MVP)** of the
product roadmap.

## What's implemented

An Android app (Kotlin + Jetpack Compose, Material 3) with:

- **5 MVP AI generator tools**: Business Plan, Proposal, Invoice & Receipt,
  Social Media Content, WhatsApp Reply — each a metadata-driven form
  (`ToolType`) so adding a new tool later means adding one enum entry, not a
  new screen.
- **Accounts** — local email/guest sign-in (`AuthRepository`, DataStore),
  with a Google Sign-In button stubbed for when Firebase is wired in.
- **History** — every generated document is saved (Room) and browsable.
- **Result editor** — edit AI output, copy to clipboard, delete.
- **PDF export** — export any result to a PDF and share/open it
  (`PdfExporter`, FileProvider).
- **Subscription screen** — Free / Monthly / Annual / Lifetime tiers from the
  PRD's monetization section, billing not yet wired up.

### Architecture

```
app/src/main/java/com/ai4biz/app/
├── model/            ToolType, InputField, GeneratedDocument (pure Kotlin)
├── ai/                AiGeneratorService interface + Mock/Remote implementations
├── data/
│   ├── local/         Room entity/DAO/database (History persistence)
│   └── repository/    DocumentRepository, AuthRepository (DataStore)
├── pdf/               PdfExporter
├── navigation/         NavHost + routes
└── ui/                One package per screen (onboarding, auth, home,
                        generator, result, history, subscription, profile),
                        each with a ViewModel
```

No DI framework — `AppContainer` (a small hand-rolled container created in
`Ai4bizApplication`) wires repositories/services and is passed down via a
`CompositionLocal`.

### Real AI backend

`AppContainer` auto-selects the generator implementation:

- **No backend configured** → `MockAiGeneratorService` (templated text,
  zero setup, always works).
- **Backend configured** → `RemoteAiGeneratorService`, which calls a small
  Express proxy (`/server`) that in turn calls
  [OpenRouter](https://openrouter.ai). The OpenRouter API key lives only on
  that server; the app holds just a lightweight shared secret for the proxy
  itself.

```
Android app --(Bearer APP_SHARED_SECRET)--> /server --(OpenRouter key)--> OpenRouter --> model
```

To turn it on:

1. `cd server && npm install && cp .env.example .env`, fill in
   `OPENROUTER_API_KEY` (and pick a model — see `server/README.md`), then
   `npm start` (or deploy it — Render/Railway/Fly.io all work).
2. In the Android project's `local.properties` (gitignored — see
   `local.properties.example`), set:
   ```
   ai4biz.backend.url=http://10.0.2.2:3000   # emulator -> local server
   ai4biz.backend.secret=<same value as APP_SHARED_SECRET>
   ```
3. Rebuild. No UI or ViewModel code changes — everything downstream (forms,
   history, PDF export) depends only on the `AiGeneratorService` interface.

Cleartext HTTP is allowed only to `10.0.2.2`/`localhost` (see
`network_security_config.xml`) for local dev; a deployed server should be
HTTPS, which needs no extra config.

## Building

Requires JDK 17+ and the Android SDK (compileSdk 34, minSdk 26).

```bash
export ANDROID_HOME=/path/to/android-sdk   # or set sdk.dir in local.properties
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
This build has been verified end-to-end (`compileDebugKotlin` and
`assembleDebug` both pass) against Android SDK Platform 34 / Build-Tools
34.0.0.

Open the project in Android Studio (Koala+) for the usual run/debug
experience — it will pick up the Gradle wrapper automatically.

## Roadmap (from the PRD)

1. **MVP** — this scaffold ✅
2. Business suite (SWOT/PESTLE, contracts, HR docs, ...)
3. Finance & CRM (cash flow, invoicing automation, sales tools)
4. Automation (WhatsApp/email automation, workflow builder)
5. Enterprise (team workspaces, admin console, RBAC)
6. AI agents marketplace

See the full PRD for target users, integrations (Firebase, Supabase, Stripe,
Paystack, WhatsApp Business, Slack, Zapier, ...), and success metrics.
