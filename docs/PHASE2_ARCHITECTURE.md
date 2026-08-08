# Phase 2 architecture — Business AI Workspace

## 1. Purpose and scope

Business Edge AI's MVP is a collection of independent AI generator tools:
pick a tool, fill a form, generate, save/export. Phase 2's goal is to turn
it into a **personalized AI business workspace** — the app learns who the
user's business is once (name, industry, products, brand voice, goals,
customers) and every AI tool (and eventually a conversational assistant)
reuses that context instead of asking for it again each time.

The full Phase 2 concept — Business Workspace, an onboarding wizard, a
Business Context Engine, a personalized AI Assistant with intent routing
and a tool registry, controlled AI memory, structured AI responses, a new
backend API surface, real authentication, multi-workspace support, and an
entitlements overhaul — is large. It's broken into 10 sprints, one slice
at a time, so each is independently reviewable and shippable. **Sprint 1
is the only one with real code so far** (this session). Sprints 2-10 below
are the forward-looking plan, not yet built — treat them as a reference
for scoping future sessions, not a promise of what exists today.

## 2. Sprint 1 — Data foundation (done)

Adds the relational data model everything else builds on, with zero UI or
behavior change to the existing MVP.

**New domain models** (`app/src/main/java/com/ai4biz/app/model/`):
`Workspace`, `BusinessProfile`, `BrandSettings` (+ `BrandTone` enum),
`ProductService` (+ `ProductServiceType` enum), `BusinessGoal` (+
`BusinessGoalType` enum), `Customer`.

**New Room tables** (`app/src/main/java/com/ai4biz/app/data/local/`):
`workspaces`, `business_profiles`, `brand_settings`, `products_services`,
`business_goals`, `customers` — one `@Entity`/`@Dao` pair each, following
the exact convention `GeneratedDocumentEntity`/`GeneratedDocumentDao`
already used. `AppDatabase` is now `version = 2`; `MIGRATION_1_2` creates
these tables, seeds one default workspace, and adds a `workspaceId` column
to `generated_documents` (backfilled via SQL `DEFAULT`, no data loss).
Enum fields use `Converters.kt` (`@TypeConverter`, stores `.name` as TEXT).

**New repositories** (`app/src/main/java/com/ai4biz/app/data/repository/`):
`WorkspaceRepository`, `BusinessProfileRepository`, `BrandSettingsRepository`,
`ProductServiceRepository`, `BusinessGoalRepository`, `CustomerRepository`
— each mirrors `DocumentRepository`'s shape (`observeAllByWorkspace`,
`observe`, `suspend save`, `suspend delete`). All wired into `AppContainer`
as plain `val`s, same pattern as every existing repository — no DI
framework introduced.

**Key decisions future sprints must know about:**

- **`Workspace.ownerUserId` is not a real user ID yet.** `AuthRepository`
  has no stable account identity (guest or a freely-typed email string, no
  backend account system). A new `DeviceIdentityRepository` generates one
  random UUID per device on first use and that's what populates
  `ownerUserId` today. **Sprint 9 (real auth) must backfill this** — a
  plain `UPDATE`, not a schema migration — once real accounts exist, and
  decide whether device-pseudo-IDs get linked to the new account or
  discarded.
- **`WorkspaceDefaults.DEFAULT_WORKSPACE_ID = "default-workspace"`** is a
  fixed, well-known id (not a random UUID) used by both the migration SQL
  and the Kotlin-side `WorkspaceRepository.getOrCreateDefaultWorkspace()`,
  so a migrated install and a fresh install converge on the same workspace
  instead of two different random ones. Reuse this constant, don't
  reinvent a second "default workspace" concept.
- **`GeneratedDocument` (domain model) was deliberately NOT extended** with
  a `workspaceId` field in Sprint 1 — only the underlying Room entity/table
  gained the column (defaulted to `DEFAULT_WORKSPACE_ID`). The one
  construction site, `GeneratorViewModel.generate()`, has no concept of
  "current workspace" yet. **Sprint 2 must**: add `workspaceId` to the
  `GeneratedDocument` domain model, update `DocumentRepository`/
  `GeneratorViewModel` to use the real active workspace, and switch History
  to `GeneratedDocumentDao.observeAllByWorkspace()` (already added,
  unused, in Sprint 1 so this doesn't need another migration).
- **No new `androidTest`/`MigrationTestHelper` harness was added.** This
  project has no test source sets at all yet; adding the first one was
  judged out of scope for a data-layer slice. The migration's SQL has been
  reviewed by hand and compiles/builds cleanly, but has **not** been
  exercised against a real pre-migration `ai4biz.db` on a device — treat
  that as a residual risk before wide rollout.
- **No Room `@Entity(indices = ...)` were added.** Room validates declared
  indices against what the migration actually created in SQL; skipping
  them entirely on both sides was the simplest way to keep Sprint 1's
  migration low-risk. Add real indices together with their matching
  migration once Sprint 2+'s actual query patterns are known.

## 3. Sprint 2 — Business Setup wizard

A multi-step onboarding flow (business name/type, industry, products sold,
target customers, location, brand personality, goals, contact info) that
writes into the Sprint 1 repositories (`WorkspaceRepository.save`,
`BusinessProfileRepository.save`, `ProductServiceRepository.save`,
`BrandSettingsRepository.save`, `BusinessGoalRepository.save`). New
`navigation/Routes.kt` entries (e.g. `business_setup/{step}`), new
ViewModels, new screens under a new `ui/businesssetup/` package. The
existing `GeneratorScreen`'s pattern of rendering fields from a metadata
list (`ToolType.fields: List<InputField>`) is the closest existing idiom
to imitate for data-driven step rendering — but the step/page mechanics
(progress indicator, next/back, partial-save, skip) don't exist anywhere
yet and need to be built new. Should allow "Skip for now" — onboarding
abandonment is a real risk. Must include: extend `GeneratedDocument` /
`GeneratorViewModel` to use the real workspace (see Sprint 1 notes above).

## 4. Sprint 3 — Workspace screen

A new `WorkspaceScreen` (tabs: Documents, Products, Customers, Templates,
Business Profile) reading/writing the Sprint 1 repositories directly. Add
search over documents (title/tool type/customer/date/content — a Room
`LIKE` query is enough at this scale; move to a dedicated search system
later only if needed).

## 5. Sprints 4-6 — Business Context Engine + AI Assistant

- **`BusinessContextService`**: `suspend fun getContext(workspaceId, contextType): BusinessContext`,
  reading from the Sprint 1 repositories and returning only what's relevant
  to the task at hand (a proposal needs business+brand+products+customer;
  a social post needs business+brand+one product) — never dump the entire
  business profile into every prompt, for cost, latency, and privacy
  reasons.
- **`BusinessAiAssistant`**: `suspend fun sendMessage(workspaceId, conversationId?, message): AssistantResponse`,
  pipeline: save user message → `IntentRouter` classifies intent → context
  retrieval → `ToolRegistry` picks a tool → `PromptBuilder` assembles
  system+context+task+request → `AiGeneratorService` → validate → persist
  → return.
- **`ToolRegistry`**: this is the natural evolution of `ToolType.kt`, which
  today is a fixed `enum` (good metadata shape — id/title/description/
  fields — but enums can't grow at runtime). Also closes a real existing
  gap: `server/tools.js` currently hand-duplicates `ToolType.kt`'s
  metadata as a second, manually-synced source of truth. A registry
  (client-driven or server-driven) should replace both.
  New conversation persistence: `ai_conversation`/`ai_message` tables,
  same Room conventions as Sprint 1.

## 6. Sprint 7 — Structured AI responses

`AiGeneratorService.generate()` currently returns `Result<String>` — plain
text, hand-parsed nowhere. Structured tool output (e.g. a business plan
with named sections) needs either a breaking change to this interface or
a new sibling interface, plus a JSON parsing strategy. **Note**: this
codebase deliberately has no `kotlinx-serialization` or Retrofit today
(OkHttp + `org.json.JSONObject` only, checked in Sprint 1 — no reason to
add either for Sprint 1's pure-data-layer work). Sprint 7 is where that
tradeoff needs an explicit decision: keep hand-parsing JSON with
`org.json`, or add `kotlinx-serialization-json` now that the payloads get
more structured.

## 7. Sprint 8 — New backend API surface

`/server/index.js` today exposes `POST /api/generate` and
`POST /api/verify-purchase` under one static shared-secret. Phase 2 wants
a real API surface (`/api/v1/workspaces`, `/api/v1/workspaces/:id/business`,
`/api/v1/workspaces/:id/documents`, `/api/v1/assistant/message`, etc.) —
not touched in Sprint 1, no server changes were made this sprint.

## 8. Sprint 9 — Real authentication & multi-workspace

Move from `AuthRepository`'s local-only guest/email state toward real
auth (email/password + Google, session management), and from the device
API and see what the token layer looks like. **Must include**: the
`ownerUserId` backfill flagged in Sprint 1 above, and per-request workspace
authorization on the backend (never trust a client-supplied `workspaceId`
without verifying membership).

## 9. Sprint 10 — Entitlements & billing touchpoints

Extend the existing `EntitlementRepository`/`BillingManager` (already
server-verified via Google Play, see `docs/PLAY_STORE_RELEASE.md`) with
real usage quotas per plan tier (AI generations/month, assistant messages,
storage) instead of today's simple local daily-limit nudge
(`UsageRepository`). Needs a `usage_record`-style table (Room, same
conventions) and an entitlement check before every AI request.

## 10. Deferred risks carried forward from Sprint 1

- `exportSchema = false` on `AppDatabase` — fine for now (no schema-diff
  tooling depends on it), but flipping to `true` with a committed
  `app/schemas/` directory would make a future `MigrationTestHelper` test
  much easier to add. Optional, not required.
- No migration test harness exists (see Sprint 1 notes above) — the
  `MIGRATION_1_2` SQL is unverified on a real device/emulator.
- No indices on any Sprint 1 table — fine at today's data volumes (one
  workspace, a handful of rows each), revisit once Sprint 2+ introduces
  real query patterns (e.g. searching customers/products at scale).

## What Phase 2 is explicitly NOT building yet

Per the product roadmap, these stay out of scope through Sprint 10: full
CRM, WhatsApp Business API integration, email automation, payments
processing (Stripe/Paystack), an AI agent marketplace, enterprise RBAC,
multi-agent workflows, a full accounting system. Those are Phase 3+.
