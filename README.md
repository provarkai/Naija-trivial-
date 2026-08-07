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
├── ai/                AiGeneratorService interface + MockAiGeneratorService
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

### Wiring in a real AI backend

`MockAiGeneratorService` currently returns templated text so the app is
fully usable with zero configuration. To use a real model:

1. Implement `AiGeneratorService` against your own backend.
2. **Proxy the LLM call through a server you control** — do not embed a
   model API key in the Android client.
3. Swap the instance created in `AppContainer.aiGeneratorService`.

Everything downstream (forms, history, PDF export) depends only on the
`AiGeneratorService` interface, so no UI code needs to change.

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
