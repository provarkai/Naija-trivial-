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

## 3. Sprint 2 — Business Setup wizard (done)

An 8-step onboarding flow (Business Basics, Industry, Products/Services,
Target Customers, Location, Brand Personality, Goals, Contact Info) that
writes into the Sprint 1 repositories, shown once right after sign-in.

**What got built**, under `ui/businesssetup/`:
- `BusinessSetupViewModel` — one ViewModel for the whole flow, holding all
  8 steps' in-progress state as plain Compose `mutableStateOf`/
  `mutableStateListOf` fields (not `StateFlow` — nothing here is async
  except the initial prefill load and the final save). `loadExisting()`
  one-shot-prefills from any already-saved `BusinessProfile`/
  `BrandSettings`/`ProductService` list/`BusinessGoal` list so re-entry
  (see below) edits real data instead of starting blank. `finish()` writes
  everything in one pass (profile → brand → products → goals → updates
  `Workspace.businessProfileId` → marks the onboarding-seen flag);
  `skip()` only marks the flag. Nothing is persisted progressively per
  step, so an abandoned wizard never leaves a half-valid row behind.
- `BusinessSetupScreen` — single route (`Routes.BUSINESS_SETUP`), internal
  `currentStep: Int` dispatches via `AnimatedContent` to the step
  composables in `ui/businesssetup/steps/BusinessSetupSteps.kt`.
  `LinearProgressIndicator` for step progress, `FilterChip`+`FlowRow` for
  all single/multi-select fields, `BackHandler` moves one step back
  instead of leaving the route. Zero new Gradle dependencies.
- **Deviation from the original sketch above**: a single flat route with
  internal step state, **not** `business_setup/{step}` as first sketched —
  per-step routes would have re-scoped a fresh `viewModel()` per
  back-stack entry, losing shared wizard state on Next/Back.
- New `OnboardingRepository` (DataStore, same convention as
  `AuthRepository`) tracks a `has_completed_business_setup` flag so a
  returning user never sees the wizard again — `AuthScreen.goHome()` now
  gates on it (first-time → wizard, already-seen → straight to Home,
  exactly like before).
- Re-entry for editing: "Edit Business Profile" button on `ProfileScreen`
  (Sprint 3's real editing UI doesn't exist yet) navigates to the same
  route; the wizard's default exit action tries `popBackStack()` first
  (returns to Profile) and only falls back to navigating Home if that
  fails (the fresh-sign-in case, where the stack is just `[BUSINESS_SETUP]`).

**Explicitly deferred, not done in this sprint** (carried over, don't lose
track of these):
- Extending `GeneratedDocument`/`GeneratorViewModel` to use the real active
  workspace — still flagged from Sprint 1, still not done. Pick this up in
  Sprint 3 or a small standalone follow-up.
- `businessType`/`industry` are populated from fixed UI-layer option lists
  (`BUSINESS_TYPE_OPTIONS`/`INDUSTRY_OPTIONS` in `BusinessSetupSteps.kt`),
  not enums — matches the Sprint 1 schema (`BusinessProfile.businessType`/
  `industry: String`) exactly, so no new migration was needed.
- `BusinessProfile.taxNumber` and `BrandSettings.primaryColor`/
  `secondaryColor`/`defaultLanguage`/`logoUri` have no wizard-step UI —
  new records get sane defaults (`"#4F46E5"`/`"#0EA5E9"`/`"en"`/`null`,
  matching the app's own theme colors), edited records keep their existing
  values untouched.
- No Android emulator in this sandbox — the wizard's rendering, step
  transitions, back-gesture handling, and the prefill-on-edit path are
  verified by compilation only, not by running the app on a device.

## 4. Sprint 3 — Workspace screen (done)

One `WorkspaceScreen` (tabs: Documents, Products, Customers, Business
Profile), reading/writing the Sprint 1 repositories directly, replacing
the old standalone History screen.

**What got built**, under `ui/workspace/`:
- `WorkspaceViewModel` — reactive off `workspaceRepository.observeActiveWorkspace()`
  (`flatMapLatest`, not a one-shot load like Sprint 2's wizard), so it
  stays correct if multi-workspace switching ever lands later. Each tab's
  data is `activeWorkspaceId.filterNotNull().flatMapLatest { repo.observeAllByWorkspace(it) }`.
- `WorkspaceScreen` — `TabRow`+`Tab` for the 4 tabs, `AnimatedContent` for
  tab-content transitions (not `HorizontalPager` — sticking to a component
  already proven to compile in this codebase, see the `FlowRow` note
  below), a `FloatingActionButton` shown only on the Products/Customers
  tabs. Documents tab reuses the exact card/row layout the old
  `HistoryScreen` had.
- `WorkspaceDialogs.kt` — `ProductDialog`/`CustomerDialog`, `AlertDialog`-
  based add/edit forms. Delete is a per-row icon button, no confirmation
  — matches the old `HistoryScreen`'s existing convention.
- `DocumentRepository` gained one delegating method,
  `observeHistoryByWorkspace(workspaceId)`, calling the DAO method Sprint
  1 added but nothing used until now.

**Deviations from the original sketch above**:
- **4 tabs, not 5.** No `Template` entity/DAO/migration exists anywhere —
  building one would be schema-level scope creep for a sprint about
  consolidating *existing* data. Deferred to a future increment.
- **Client-side search, not a Room `LIKE` query.** The DAO has no such
  query and data volume per workspace is small enough that
  `WorkspaceViewModel` just combines the document flow with a local
  `searchQuery` and filters in Kotlin (`title`/`toolTitle`/`content`,
  case-insensitive `contains`). Revisit only if this becomes a measured
  problem at real scale.
- **History fully consolidated, not duplicated.** `ui/history/HistoryScreen.kt`
  and `HistoryViewModel.kt` are deleted, `Routes.HISTORY` is gone, and
  `HomeScreen`'s top-bar icon now points at `Routes.WORKSPACE` instead.

**Still deferred, carried forward a third time**: `GeneratedDocument`/
`GeneratorViewModel` still don't use the real active workspace — every
document is still written with `workspaceId = WorkspaceDefaults.DEFAULT_WORKSPACE_ID`
in `DocumentRepository.toEntity()`. `observeHistoryByWorkspace` exists and
is now used by the Workspace screen, but this only stays correct because
the app has exactly one workspace today. Must be fixed before any
multi-workspace UI (Sprint 9 territory) ships.

**Lesson carried from Sprint 2, worth repeating**: a sub-agent claimed
`FlowRow` needed no experimental opt-in at this project's exact Compose
version, and the real build proved that wrong. This sprint's design
deliberately avoided re-risking that by using only long-stable, proven
Material3 components (`TabRow`, `AlertDialog`, `FloatingActionButton`,
`AnimatedContent`) — verify any future experimental-API claim by actually
building, not by inference.

No Android emulator in this sandbox — tab switching, dialog behavior,
search filtering, and FAB visibility are verified by compilation only,
matching Sprints 1-2's residual-risk note.

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
