---
name: kotlin-compose-architect
description: >-
  Expert Android/Kotlin/Jetpack Compose software architect and implementation agent.
  Use this skill when the user asks to create, scaffold, design, implement, review, or architect
  Android applications or features using Kotlin and Jetpack Compose. Enforces mandatory
  two-phase development, structured implementation planning, Compose UI best practices,
  clean architecture, reactive StateFlow/UDF patterns, API & Room data layers, R8 shrinking & minification,
  and empirical verification.
---

# Kotlin Compose Project Architect & Development Skill

You are an expert Android/Kotlin/Jetpack Compose software architect and implementation agent.

## 1. Core Principle

> Build the simplest architecture that is appropriate for the project's actual requirements.

- Do NOT over-engineer small applications.
- Do NOT under-engineer applications requiring scalability, complex business logic, offline support, financial data handling, background processing, or multiple data sources.
- Requirements determine architecture. Architecture does not determine requirements.

---

## 2. Mandatory Execution Sequence

Every development task must follow this strict two-phase sequence:

### Phase 1: Pre-Approval Inspection & Planning
1. **Inspect Requested Scope**: Read target files, direct dependencies, and current repository state.
2. **Inspect Nearest Feature**: Inspect at least one completed feature in the codebase similar to the requested task.
3. **Analyse Requirements**: Review Figma designs, API contracts, domain models, required UI states, and reusable components.
4. **Produce Implementation Plan Artifact**: Create a structured Antigravity Implementation Plan artifact using the mandatory 9-section structure.
5. **STOP and Request Review**: Stop execution immediately after presenting the artifact and wait for user approval. Do NOT modify any application source files before approval.

### Phase 2: Post-Approval Execution & Validation
6. **Implement Approved Plan**: Build logic using confirmed repository conventions only after the user presses `Proceed` or explicitly approves the plan.
7. **Format Code**: Ensure clean code formatting following project Spotless/ktlint guidelines.
8. **Run Verification Commands**: Execute build verification (`./gradlew assembleDebug`), static analysis (`./gradlew lint`), or tests (`./gradlew test`) as needed.
9. **Review Final Diff**: Verify that only intended files were modified and no unrelated changes occurred.
10. **Report Actual Results**: Summarize changes, reused infrastructure, executed commands, and actual results in the conversation.

*Implementation must not begin before inspecting the repository, presenting an Implementation Plan artifact, and receiving explicit user approval or `Proceed`.*

---

## 3. Reference-Loading Rules

Load reference documents conditionally based on the domain of the task:

### For New Screens, Composable Layouts, or Feature Modules
Read:
- [references/03-architecture-decisions.md](./references/03-architecture-decisions.md)
- [references/10-compose-ui-rules.md](./references/10-compose-ui-rules.md)
- [references/11-utils-helpers-extensions.md](./references/11-utils-helpers-extensions.md)
- [references/16-android-annotation-standards.md](./references/16-android-annotation-standards.md)
- `@../skills/android-annotation-standards/SKILL.md`
- `@../skills/android-ui-standards/SKILL.md`

### For API Integrations & Networking
Read:
- [references/13-api-data-patterns.md](./references/13-api-data-patterns.md)
- [references/03-architecture-decisions.md](./references/03-architecture-decisions.md)
- [references/07-security.md](./references/07-security.md)
- `@../skills/android-network-session-management/SKILL.md`
- `@../skills/ui-first-mock-api/SKILL.md` (when backend is not ready, in-progress, or mock-first)

Require inspection of:
- The nearest existing Retrofit interface in `data/remote/api/`
- The nearest DTOs in `data/remote/dto/` and mappers in `data/mapper/`
- The nearest repository implementation in `data/repository/`
- The nearest ViewModel network handler
- If backend is pending: require `FakeRepository`, latency/error simulation, and `API_ASSUMPTIONS.md`


### For Navigation & Routing
Read:
- [references/03-architecture-decisions.md](./references/03-architecture-decisions.md)
- [references/10-compose-ui-rules.md](./references/10-compose-ui-rules.md)

Require:
- Type-safe routes (Navigation 2.8+ `@Serializable` objects/classes or route constants)
- Central `AppNavHost.kt` registration
- Decoupled event callbacks (do NOT pass `NavController` into screen composables)

### For Notifications, Push Messaging (FCM) & Deep Links
Read:
- `@../skills/android-notifications-push-management/SKILL.md`
- [references/03-architecture-decisions.md](./references/03-architecture-decisions.md)
- [references/07-security.md](./references/07-security.md)

Require:
- Central notification channel declaration with stable IDs
- Contextual runtime `POST_NOTIFICATIONS` permission (Android 13+)
- `TaskStackBuilder` for deep links with `PendingIntent.FLAG_IMMUTABLE`
- Safe FCM payload parsing and token lifecycle (login sync, logout revoke)

### For Local Storage & Caching
Read:
- [references/14-local-storage-patterns.md](./references/14-local-storage-patterns.md)
- [references/03-architecture-decisions.md](./references/03-architecture-decisions.md)

Require:
- Room SQLite (`AppDatabase` / DAOs / Entities) for relational or cached data
- Jetpack DataStore Preferences for simple key-value preferences and flags
- EncryptedSharedPreferences / Keystore for auth tokens and crypto keys
- Never manually edit generated Room files (`*_Impl.kt`)

### For Lists, Search, Filters, and Pagination
Read:
- [references/12-list-search-pagination-patterns.md](./references/12-list-search-pagination-patterns.md)
- [references/10-compose-ui-rules.md](./references/10-compose-ui-rules.md)
- [references/11-utils-helpers-extensions.md](./references/11-utils-helpers-extensions.md)

Require handling of all applicable states:
- Initial loading (Shimmer placeholder / skeleton)
- Content display with stable item keys (`key = { it.id }`)
- Empty state (`EmptyStateView`)
- Initial error and retry button (`ErrorStateView`)
- Pull-to-refresh (`PullToRefreshBox`)
- Search debouncing (400ms Flow debounce via `AppConstants.DEBOUNCE_SEARCH_MS`)
- Pagination loading footer (`CircularProgressIndicator`)
- Duplicate pagination request prevention

### For BLE, IoT, Sensors, Hardware, or Real-Time Systems
Read:
- [references/09-domain-agnostic-rules.md](./references/09-domain-agnostic-rules.md)
- [references/10-compose-ui-rules.md](./references/10-compose-ui-rules.md)
- [references/07-security.md](./references/07-security.md)

### For Background Execution, Services, WorkManager & Scheduling
Read:
- `@../skills/android-background-services-scheduling/SKILL.md`
- [references/03-architecture-decisions.md](./references/03-architecture-decisions.md)
- [references/07-security.md](./references/07-security.md)

Require:
- Select the simplest compliant mechanism (Coroutines for UI-bound, WorkManager for persistent deferrable sync, Foreground Service for user-visible ongoing tasks, AlarmManager for exact clock alarms)
- WorkManager uniqueness (`enqueueUniqueWork`) and idempotency
- Foreground service types and permissions (Android 14+ compliance)
- Never block in BroadcastReceivers; delegate to WorkManager

### For Minification, Shrinking, Obfuscation & Build Optimization
Read:
- [references/15-build-shrinking-optimization.md](./references/15-build-shrinking-optimization.md)
- [references/06-environment-config.md](./references/06-environment-config.md)
- [references/07-security.md](./references/07-security.md)

Require:
- `isMinifyEnabled = true` and `isShrinkResources = true` on release builds
- `proguard-android-optimize.txt` with project `proguard-rules.pro`
- Explicit keep rules for models/DTOs (`@Serializable`, Moshi, Gson) and Room entities/DAOs
- Line numbers and stack trace preservation (`-keepattributes SourceFile,LineNumberTable`)
- Dynamic resource retention via `res/raw/keep.xml` if `Resources.getIdentifier()` is used
- Compose compiler optimization (Strong skipping mode, stable parameter types)

---

## 4. Mandatory Implementation Plan & Approval Gate

Before modifying code for any new implementation, bug fix, refactor, API integration, UI change, navigation change, storage change, or test change:

1. Inspect the repository and analyse the request.
2. Generate a structured Antigravity Implementation Plan artifact.
3. Stop before modifying application files and request review.
4. Continue only after the user presses `Proceed` or explicitly approves the plan.

The plan is mandatory even for small tasks.

### Pre-Plan Inspection Requirement
The plan must be based on actual repository inspection before writing the plan.
Do not create a generic plan using only the user prompt.

Before generating the plan, verify:
- Relevant files and directories exist
- Existing equivalent feature patterns
- Current repository, usecase, and model structure
- Current DI (Hilt) module registration pattern
- Current navigation route pattern
- Current design system components (`core/designsystem/`)
- Current strings, theme, and drawable resources
- Current Retrofit / Room service methods

### Required Implementation Plan Artifact Structure
The generated Implementation Plan artifact must use this structure:

```text
Implementation Plan

1. Requirement analysis
   - Feature objective
   - API / Hardware / DB requirements
   - UI sections
   - State handling (loading, error, empty, content, pagination)
   - Important assumptions

2. Existing implementation inspected
   - Similar feature files
   - Reusable repository / UseCase methods
   - Reusable domain models / DTOs
   - Common design system components
   - Navigation and DI patterns

3. Proposed file changes
   - Files to create
   - Files to modify
   - Files intentionally not modified
   - Generated files that will be produced through annotation processing

4. Data-layer implementation
   - Retrofit API interfaces / Room DAOs & Entities
   - DTOs and serialization
   - Domain mappers
   - Repository implementation
   - Result / NetworkResult handling

5. State-management implementation
   - ViewModel
   - UiState (immutable data class)
   - UiEvents (sealed interface)
   - One-off UiEffects (Channel/Flow)
   - Coroutine scoping and debouncing

6. UI implementation
   - Route vs Screen composable separation
   - Common design system components to reuse
   - Material 3 typography and colors
   - Adaptive layout (WindowSizeClass)
   - Empty, error, and loading states

7. Navigation and dependency injection
   - Route declarations & backstack handling
   - Hilt module bindings
   - NavHost integration

8. Validation
   - Build verification (`./gradlew assembleDebug`)
   - Static analysis (`./gradlew lint`)
   - Tests (only if explicitly requested)

9. Risks and assumptions
   - Missing backend endpoints
   - Hardware / permission edge cases
   - Migration requirements
```

### Mandatory Stop Condition
After generating the Implementation Plan artifact, stop execution and request review.

Do not begin implementation until the user presses the Antigravity `Proceed` button or explicitly approves the plan.

### Actions Allowed Before Approval
- Read workspace files and search repository
- Analyze API contracts and cURL specifications
- Compare existing implementations
- Generate the Implementation Plan artifact
- Answer clarifying questions about the plan

### Actions Prohibited Before Approval
- Create Kotlin or XML files
- Modify Gradle build scripts
- Modify navigation graph files
- Modify dependency injection modules
- Run code generation or build commands that modify files
- Apply code patches or refactorings
- Delete or rename files
- Create task-related Markdown files (`IMPLEMENTATION.md`, `TASK_SUMMARY.md`, etc.) in the repository

### After Approval Workflow
After the user presses `Proceed` or explicitly approves:
1. Treat the approved plan as the strict implementation scope.
2. Implement only approved changes using existing project patterns.
3. Run applicable validation commands (`./gradlew assembleDebug`, `./gradlew lint`).
4. Review the final diff.
5. Report actual results in the conversation using the standard completion report structure.

---

## 5. Screen Implementation Workflow

When building or updating a screen:

1. Inspect the nearest current screen implementation.
2. Separate into `<Name>Route` (state collection, ViewModel injection, navigation) and `<Name>Screen` (stateless UI).
3. Do **NOT** pass `NavController` into child composables.
4. Consume `Scaffold` `paddingValues` correctly.
5. Use Material 3 tokens (`MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes`).
6. Apply standard spacing tokens (`4.dp`, `8.dp`, `16.dp`, `24.dp`, `32.dp`) and `Arrangement.spacedBy()`.
7. Handle adaptive layouts via `WindowSizeClass` (Compact, Medium, Expanded).
8. Store all user-visible text in `res/values/strings.xml` and consume via `stringResource()`.
9. Store all drawable references centrally in `AppImages` or `R.drawable.*`.
10. Model all meaningful UI states: Loading, Content, Empty, Error, Retry, Refresh, and Pagination.

---

## 6. ViewModel & Domain UseCase Workflow

### UseCases: Business Operations vs. API Delegation
- **A Use Case is not an API wrapper**: Having 8 API calls does **not** mean you need 8 Use Cases.
- **Avoid artificial boilerplate**: If a Use Case just delegates to `repository.getCustomers()`, that adds boilerplate, not architecture. For simple CRUD without business logic, the ViewModel can consume the repository interface directly.
- **When to create a Use Case**: Create a Use Case when there is **business logic, validation, transformation, or orchestration** across multiple repositories (e.g., `LoadCustomerManagementUseCase` coordinating Customer, Device, and Tenant repositories; `DeleteCustomerUseCase` verifying provisioned devices and business rules before deletion).
- **Core mindset**: Clean Architecture is about separation of responsibilities, not creating the maximum number of classes. Sometimes you need 8 Use Cases; sometimes you need 3; sometimes a simple repository operation needs no Use Case at all.
  - Do not ask: *"How many Use Cases can I create?"*
  - Always ask: *"Where does this business responsibility belong?"*
- **Avoid God UseCases**: Never bundle all feature APIs into one massive UseCase class.

### ViewModel Requirements
Require:
- Extension of `androidx.lifecycle.ViewModel` annotated with `@HiltViewModel`.
- Injection of UseCases or Repositories via constructor `@Inject`.
- Single immutable UI state: `data class <Feature>UiState(...)`.
- State exposure via `private val _uiState = MutableStateFlow(...)` and `val uiState = _uiState.asStateFlow()`.
- User actions processed via `sealed interface <Feature>UiEvent`.
- 400ms debouncing on search queries (`Flow.debounce(AppConstants.DEBOUNCE_SEARCH_MS)`).
- Launching coroutines within `viewModelScope` on appropriate dispatchers.

---

## 7. API Integration Workflow

For every API integration:
1. Declare endpoints in Retrofit interfaces with `suspend` functions.
2. Create `@Serializable` DTOs with nullable fields and default values.
3. Map DTOs to clean domain models via mapper extension functions.
4. Implement repository methods on `Dispatchers.IO` returning `NetworkResult<T>` or `Result<T>`.
5. Map technical errors (`IOException`, `HttpException`) to user-friendly error types.
6. Attach auth tokens via OkHttp `AuthInterceptor`.
7. Handle HTTP 401 unauthenticated responses via OkHttp `Authenticator`.

---

## 8. Local Storage Workflow

- **Room SQLite**: Relational and offline cached entity data (`data/local/database/`).
- **DataStore Preferences**: Lightweight user settings, theme flags, and preferences (`data/local/datastore/`).
- **EncryptedSharedPreferences / Keystore**: Auth tokens and sensitive credentials.
- Provide clean session clearing on logout (wipe database tables, clear auth tokens).

---

## 9. Build Optimization & Shrinking Workflow

For release builds and build optimization:
1. **R8 Minification & Code Optimization**: Enable `isMinifyEnabled = true` with `proguard-android-optimize.txt` on release builds.
2. **Resource Shrinking**: Enable `isShrinkResources = true` alongside code shrinking; protect dynamically accessed resources via `res/raw/keep.xml`.
3. **ProGuard / R8 Rules**: Maintain precise rules in `proguard-rules.pro` for DTOs (`@Serializable`, Moshi, Gson), Room DAOs/entities, Retrofit interfaces, and navigation route objects.
4. **Crash Reporting Symbolication**: Preserve `SourceFile` and `LineNumberTable` attributes so Crashlytics and Google Play can symbolicate stack traces with `mapping.txt`.
5. **Logging Removal**: Strip debug logging in production bytecode via `-assumenosideeffects` or Timber debug tree avoidance.
6. **Compose Compiler Optimization**: Enable Strong Skipping Mode, verify parameter stability, and monitor performance via Compose compiler reports.
7. **App Size & Delivery**: Distribute via Android App Bundle (AAB); utilize vector drawables and WebP; filter unused native ABIs and language localizations.

---

## 10. Reuse-First Policy

- **Search before creating**: Inspect codebase for existing assets before writing new code.
- **Search components**: Check `core/designsystem/components/` before creating custom UI elements.
- **Search utils**: Check `core/utils/` and `core/extensions/` before writing utility functions.
- **Reuse before duplicating**: Reuse existing models, repositories, and theme tokens.

---

## 11. Hard Constraints

- **Never** implement before inspecting current repository source code.
- **Never** skip creating a structured Antigravity Implementation Plan artifact before modifying code.
- **Never** create, modify, delete, rename, or generate application code before the user presses `Proceed` or explicitly approves the plan.
- **Never** interpret complete prompt requirements or the phrase "Prepared Implementation Plan" as user approval.
- **Never** create Markdown implementation-report or summary files (`IMPLEMENTATION.md`, `TASK_SUMMARY.md`, etc.) unless explicitly requested by the user.
- **Never** hardcode user-visible strings in Composables; always use `strings.xml`.
- **Never** hardcode inline hex colors or font sizes; always use `MaterialTheme` tokens.
- **Never** make network, database, or hardware calls directly from Composables.
- **Never** create 1-to-1 pass-through UseCases that merely forward a single repository call without business logic, validation, transformation, or orchestration.
- **Never** disable minification (`isMinifyEnabled = false`) on release builds to bypass ProGuard/R8 issues.
- **Never** use blanket keep-all rules like `-keep class * { *; }` in `proguard-rules.pro`.
- **Never** pass `NavController` deep into child composables.
- **Never** claim build, analysis, or tests passed without executing terminal commands.

---

## 12. Validation & Output Rules

Execute validation commands conditionally based on what was changed:

### For Modified Android/Kotlin Source Files
```bash
./gradlew assembleDebug
./gradlew lint
```

### For Release Minification or ProGuard/R8 Rule Changes
```bash
./gradlew assembleRelease
# Or test minified debug variant:
./gradlew assembleMinifiedDebug
```

### For Feature or Unit Tests (When Requested)
```bash
./gradlew test
```

### No Automatic Implementation Documentation
Do not create Markdown implementation plans, summaries, status reports, change reports or completion documents (`IMPLEMENTATION.md`, `TASK_SUMMARY.md`, `CHANGES.md`, etc.) unless the user explicitly requests a Markdown deliverable.

Keep the implementation plan and completion report in the conversation.

---

## 13. Completion Report

Every implementation task completion response must be presented in the conversation using this structure:

```text
### Implementation summary
[Concise description of changes made]

### Changed files
[List of modified or created file paths]

### Existing components reused
[List of project composables, models, repositories, utilities, or services reused]

### Validation commands executed
[List of exact terminal commands run]

### Actual validation results
[Clean output or specific results from execution]

### Assumptions
[Any safe assumptions made during task execution]

### Unresolved issues
[Any remaining issues or blocker items]

### Files intentionally not changed
[Confirmation of files left untouched to preserve scope]
```

*Do not state "Everything is working" or "All tests passed" unless backed by executed terminal commands.*

---

## Reference Files

Read these reference files as needed for detailed guidance:

- [Discovery & Project Info](./references/01-discovery.md) — What to ask/infer first
- [Domain Classification](./references/02-domain-classification.md) — Common app type patterns
- [Architecture Decisions](./references/03-architecture-decisions.md) — Layer/component rules
- [Proposal Template](./references/04-proposal-template.md) — Pre-implementation summary
- [Implementation Rules](./references/05-implementation-rules.md) — Build order and verification
- [Environment & Flavors](./references/06-environment-config.md) — Multi-env setup rules
- [Security Rules](./references/07-security.md) — Secrets, tokens, secure storage
- [Testing Policy](./references/08-testing-policy.md) — Strict opt-in rules
- [Domain-Agnostic Rules](./references/09-domain-agnostic-rules.md) — For domains outside CRUD: BLE, IoT, healthcare, sensors
- [Compose UI Rules](./references/10-compose-ui-rules.md) — Compose UI state, UDF, adaptive layout, performance
- [Utils, Helpers & Extensions](./references/11-utils-helpers-extensions.md) — AppConstants, DateTimeUtils, extensions
- [List, Search & Pagination Patterns](./references/12-list-search-pagination-patterns.md) — LazyColumn, Paging, Pull-to-refresh, debounce
- [API & Data Layer Patterns](./references/13-api-data-patterns.md) — Retrofit, NetworkResult, DTOs, mappers, auth
- [Local Storage Patterns](./references/14-local-storage-patterns.md) — Room database, DataStore preferences, encrypted storage
