# Reference 03 — Architecture Decisions

Detailed rules for each architectural component. Evaluate each one
independently — do not apply all of them automatically.

---

## Clean Architecture & Modern Android Architecture (MAD) Standards

Structure Android applications following Google's official **Modern Android Development (MAD) Guide to App Architecture** combined with **Clean Architecture principles**.

### 1. The Three Architectural Layers

Android applications are organized into up to three distinct layers with a strict unidirectional dependency rule:

```text
┌────────────────────────────────────────────────────────┐
│                   UI Layer (Presentation)              │
│  - Composables: <Feature>Route & <Feature>Screen       │
│  - State Holders: ViewModels exposing StateFlow<UiState>│
│  - Decoupled Navigator & SavedStateHandle in VM       │
└───────────────────────────┬────────────────────────────┘
                            │ depends on (calls)
                            ▼
┌────────────────────────────────────────────────────────┐
│               Domain Layer (Optional)                  │
│  - UseCases / Interactors (pure Kotlin business logic)  │
│  - Domain Models & Business Validation Rules           │
│  - Repository Interfaces (contracts)                   │
│  - Zero Android framework dependencies (no Context)    │
└───────────────────────────┬────────────────────────────┘
                            │ depends on (calls)
                            ▼
┌────────────────────────────────────────────────────────┐
│                 Data Layer (Single Source of Truth)    │
│  - Repositories: Coordinate data sources & expose Flow  │
│  - Remote Data Source: Retrofit / OkHttp / Sockets     │
│  - Local Data Source: Room Database / DataStore Prefs  │
│  - DTOs, Room Entities, and Data Mappers               │
└────────────────────────────────────────────────────────┘
```

#### Strict Dependency Rule:
- **Dependencies flow strictly inward**: UI depends on Domain and Data; Domain depends on Data contracts; Data never depends on Domain or UI.
- **UI Independence**: UI Composables and ViewModels never touch Room Entities, SQL queries, Retrofit interfaces, or raw HTTP status codes.
- **Single Source of Truth (SSOT)**: Repositories in the Data Layer serve as the single source of truth for all application data, caching remote responses into local storage and exposing reactive `Flow<T>`.

---

### 2. Modern Package & Module Organization Standards

Avoid catch-all `shared/` or `common/` dumping grounds. In modern Android development, select one of two proven, standard organizational structures based on app size:

#### Standard A: Feature-First Architecture (Recommended for Scalable / Multi-Module Apps)
Features own their user journey and presentation; core infrastructure is partitioned into focused, single-responsibility modules or packages:

```text
app/src/main/java/com/yourpackage/
├── core/
│   ├── common/             # Coroutine dispatchers, formatters, universal extensions
│   ├── database/           # Room database, DAOs, entities, type converters
│   ├── datastore/          # DataStore preferences & user settings storage
│   ├── designsystem/       # Material 3 tokens, theme, AppButton, AppTextField, custom components
│   ├── model/              # Shared domain models & universal business entities
│   ├── navigation/         # Destination, Navigator, NavHandler, NavUtils, SerializableNavType
│   ├── network/            # Retrofit/OkHttp clients, interceptors, NetworkResult, token auth
│   └── session/            # UserSessionManager, SessionStorage
│
└── features/
    ├── auth/
    │   ├── data/           # Auth API service, auth DTOs, AuthRepositoryImpl
    │   ├── domain/         # LoginUseCase, AuthToken entity, AuthRepository interface
    │   └── presentation/   # LoginRoute, LoginScreen, LoginViewModel, LoginUiState, LoginEvent
    ├── portfolio/
    │   ├── data/
    │   ├── domain/
    │   └── presentation/
    ├── watchlist/
    │   ├── data/
    │   ├── domain/
    │   └── presentation/
    └── settings/
        └── presentation/   # Simple features with no custom domain/data consume core directly
```

#### Standard B: Layer-First Clean Architecture (Standard for Focused / Single-Module Apps like Portly)
Used in single-module codebases where all data sources, UI screens, and utilities are partitioned cleanly by architectural layer:

```text
app/src/main/java/com/yourpackage/
├── data/
│   ├── local/              # Room database, DAOs, DataStore preferences
│   ├── remote/             # Retrofit interfaces, DTOs, interceptors, network error parsers
│   ├── repository/         # Repository implementations (SSOT)
│   ├── model/              # Domain models, serializable DTOs, request/response objects
│   └── socket/             # WebSocket clients, real-time live price subscriptions
│
├── domain/ (optional)
│   ├── usecase/            # Multi-repository orchestrators & complex business rules
│   └── repository/         # Repository interfaces (contracts)
│
├── ui/
│   ├── screen/             # Feature screens (auth/, portfolio/, watchlist/, settings/)
│   ├── component/          # Reusable design system widgets (buttons, cards, dialogs, shimmer)
│   ├── navigation/         # Destination, Navigator, NavHandler, NavUtils, Transitions
│   └── theme/              # Color, Type, Shape, Theme
│
├── di/                     # Central Hilt modules (NetworkModule, DatabaseModule, RepositoryModule)
└── utils/                  # Universal extensions, date/time helpers, logging trees, crypto
```

#### Why Catch-All `shared/` Folders Are Avoided:
- Catch-all `shared/` directories inevitably degrade into high-coupling dumping grounds where models, utilities, and UI widgets become tightly intermingled.
- Partitioning into explicit functional areas (`core/designsystem`, `core/network`, `core/model`, `core/navigation`) preserves clear ownership boundaries, eliminates circular dependencies, and enables seamless modularization into Gradle subprojects when scaling.

---

---

## UseCase Decision Rule — Business Operations vs. API Delegation

Having N API calls does **not** automatically mean you should have N UseCases.

### Core Principle
> **Create a UseCase when there is a business action, business rule, transformation, validation, or multi-repository orchestration — not simply because an API exists.**
> 
> The goal of Clean Architecture is **separation of responsibilities and maintainability**, not maximizing the number of classes.

Clean Architecture does **not** mean "ViewModels must never depend on repositories."
- If an operation is simple CRUD with no business logic (e.g. fetching a list or single entity directly), the ViewModel can depend on the repository interface directly without adding an artificial pass-through wrapper.
- UseCases earn their place when they encapsulate real business logic or orchestrate multiple data sources.

### Option A vs. Option B vs. Business Orchestration

Suppose a screen interacts with `CustomerRepository` (2 APIs), `DeviceRepository` (3 APIs), and `TenantRepository` (3 APIs):

1. **Option A — 8 1-to-1 UseCases (Anti-Pattern if logic is trivial):**
   ```text
   ViewModel ──► GetCustomerUseCase, DeleteCustomerUseCase, GetDevicesUseCase, ProvisionDeviceUseCase, ...
   ```
   If every UseCase is merely a single-line pass-through:
   ```kotlin
   class GetCustomerUseCase(private val repo: CustomerRepository) {
       suspend operator fun invoke() = repo.getCustomer()
   }
   ```
   You are adding boilerplate and indirection without adding domain or business value.

2. **Option B — ViewModel consumes repositories directly (Valid for simple operations):**
   ```text
   ViewModel ──► CustomerRepository, DeviceRepository, TenantRepository
   ```
   Reasonable when the ViewModel is orchestrating simple, independent reads/writes and repository methods cleanly represent domain models.

3. **Recommended — Group by meaningful business functionality:**
   ```text
   CustomerManagementViewModel
           ↓
   LoadCustomerManagementUseCase (orchestrates customer + device + tenant)
   DeleteCustomerUseCase (validates customer status & provisioned devices before delete)
   ```
   ```kotlin
   class LoadCustomerManagementUseCase @Inject constructor(
       private val customerRepository: CustomerRepository,
       private val deviceRepository: DeviceRepository,
       private val tenantRepository: TenantRepository
   ) {
       suspend operator fun invoke(): CustomerManagementData {
           val customers = customerRepository.getCustomers()
           val devices = deviceRepository.getDevices()
           val tenant = tenantRepository.getTenant()

           return CustomerManagementData(
               customers = customers,
               devices = devices,
               tenant = tenant
           )
       }
   }
   ```
   Business orchestration and multi-repository combination live outside the ViewModel and remain independently testable.

### Avoid "God UseCases"
Do **NOT** replace a bloated ViewModel with a single giant "God UseCase" containing 15-20 methods covering every API in the feature or app.
Instead, create focused use cases around **distinct business operations**:
- `LoadCustomerManagementUseCase` (reads from Customer, Device, and Tenant repos)
- `DeleteCustomerUseCase` (validates device dependencies across DeviceRepo and deletes via CustomerRepo)

### Decision Matrix

| Situation | Use Case? | Rationale |
|-----------|-----------|-----------|
| `repository.getCustomers()` directly | ⚠️ Optional / Not necessarily | Simple query; ViewModel can call repo interface directly. |
| `repository.getCustomer(id)` directly | ⚠️ Optional / Not necessarily | Trivial read; no added value in pass-through wrapper. |
| API response → domain model mapping | ❌ No | Repository / Mapper responsibility in data layer. |
| Validate entity before deletion | ✅ **Yes** | Business validation rule belongs in domain layer. |
| Delete customer + verify provisioned devices | ✅ **Yes** | Multi-repository coordination and domain rule. |
| Call 3 repositories to construct screen state | ✅ **Yes** | Orchestration lives in domain, keeping ViewModel lean. |
| Complex calculation / domain filtering | ✅ **Yes** | Pure Kotlin business logic belongs in UseCase. |
| Simple CRUD with zero business logic | ⚠️ Optional | ViewModel can depend directly on Repository. |
| One God UseCase containing every API | ❌ **No** | Anti-pattern; defeats single-responsibility principle. |

---

## Repository Decision Rule

Use repositories when data comes from:
- REST API, GraphQL, Firebase, Database, File system, Multiple sources, External SDKs

Repository interfaces → `domain/repository/`
Repository implementations → `data/repository/`

```kotlin
// domain/repository/UserRepository.kt
interface UserRepository {
    suspend fun getUser(id: String): User
}

// data/repository/UserRepositoryImpl.kt
class UserRepositoryImpl @Inject constructor(
    private val api: UserApi,
    private val dao: UserDao
) : UserRepository { ... }
```

---

## DTO vs Domain Model

Do not expose API DTOs directly to the UI when meaningful domain logic exists.

```
API → UserDto → Mapper → User → UseCase → UiState → Compose
```

For very simple applications, skip unnecessary mapping layers.

---

## UI Architecture — Unidirectional Data Flow

```
User Action → ViewModel → UseCase / Repository → State Update → Compose
```

Each screen should have an explicit state model:

```kotlin
data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)
```

**Never put in Composables:**
- API calls
- Database calls
- Business logic
- Authentication logic

---

## UI State — Handle All Cases

Every meaningful screen must handle:
- Initial state
- Loading
- Success
- Empty
- Error
- Retry

Generic sealed interface (use when appropriate):
```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

Do not force this onto every screen if a simpler model is clearer.

---

## Dependency Injection — Hilt

DI is enabled by default for non-trivial applications. Prefer Hilt.

```
SingletonComponent
├── NetworkModule
├── DatabaseModule
├── RepositoryModule
└── Other infrastructure
```

For a trivial app with 1 screen, no API, and no database — DI may not be necessary.

---

## Networking — Retrofit + OkHttp + Kotlin Serialization

Preferred for REST APIs. Keep clear separation:

```
data/
├── remote/
│   ├── api/       ← Retrofit interfaces
│   └── dto/       ← API response models
```

Do not expose Retrofit interfaces directly to ViewModels.

---

## Error Handling

Never leak raw exceptions to the UI.

Map technical errors to application errors:
```
UnknownHostException → NetworkUnavailable
SocketTimeoutException → RequestTimeout
HttpException(401) → Unauthorized
HttpException(5xx) → ServerError
SerializationException → InvalidResponse
```

Only expose user-friendly messages to Compose.

---

## Local Storage

**DataStore** — Use for:
- Theme, user preferences, small settings, simple flags, session metadata

**Room** — Use for:
- Structured data, offline cache, large datasets, relationships, searchable local data

Do not introduce Room simply because the app has an API.

---

## Navigation & Screen Architecture

For multiple screens, use Navigation Compose with the decoupled Navigator pattern:
- **Decoupled Navigation**: ViewModels inject an `@ActivityRetainedScoped Navigator` to emit navigation events via `SharedFlow`. ViewModels never reference `NavController` or Android UI contexts.
- **Composable Bridge**: Use a root `NavHandler` composable to collect `Navigator` flows and execute commands on `NavController`.
- **Destination Modeling**: Define static routes with `Screen` and parameterized routes with `DynamicScreen<T>` using `serializableNavType<T>()`.
- **Forward Arguments**: Pass IDs or concise `@Serializable` args objects. Extract via `savedStateHandle.getArguments<T>()`.
- **Backward Results**: Child sets results via `previousBackStackEntry.savedStateHandle`. Parent observes via `getStateFlow` and **must immediately remove the key** upon consumption to prevent re-triggering.
- **Route vs. Screen Separation**: `<Feature>Route` handles VM injection and lifecycle events (`LifecycleEventEffect`); `<Feature>Screen` is stateless and previewable.
- **Cross-Graph Communication**: Provide `outerNavController` to nested tab graphs for inter-graph state synchronization.
- **Shared ViewModels**: Use `sharedViewModel<T>()` for multi-step wizards scoped to navigation sub-graphs.

---

## Background Work

Use WorkManager for:
- Periodic synchronization
- Upload retry
- Deferred work
- Background maintenance

Do not create background workers unless a real requirement exists.

---

## Design System

Create a shared design system when:
- Multiple screens exist
- Components are reused
- Branding matters

```
core/designsystem/
├── components/
├── theme/
├── typography/
└── colors/
```

---

## Dependency Selection Rules

Before adding any dependency, ask:
1. Is it actually required?
2. Does AndroidX already provide this?
3. Can Kotlin/Compose solve it simply?
4. Is it actively maintained and compatible?
5. Is it solving a real problem?

Avoid dependency bloat.

---

## Feature-Based Organization

Organize features into cohesive, self-contained packages under `features/`:

```text
features/
├── auth/
│   ├── data/           # Remote data sources, local DAOs, repository implementations
│   ├── domain/         # Entities, repository interfaces, use cases
│   └── presentation/   # <Name>Route, <Name>Screen, ViewModel, UiState
├── profile/
│   ├── data/
│   ├── domain/
│   └── presentation/
├── customers/
│   ├── data/
│   ├── domain/
│   └── presentation/
└── orders/
    ├── data/
    ├── domain/
    └── presentation/
```

Instead of mixing everything into global flat folders:
```
screens/   viewmodels/   repositories/   models/
```
