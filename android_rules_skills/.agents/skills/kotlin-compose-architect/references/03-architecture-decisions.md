# Reference 03 — Architecture Decisions

Detailed rules for each architectural component. Evaluate each one
independently — do not apply all of them automatically.

---

## Clean Architecture & Three-Tier Organization

Structure Android applications using a clear three-tier separation (`core/`, `shared/`, `features/`):

```text
app/src/main/java/com/yourpackage/
├── core/
│   ├── di/                 # Central Hilt modules (NetworkModule, DatabaseModule)
│   ├── network/            # OkHttp, Retrofit clients, NetworkResult, Interceptors
│   ├── session/            # SessionManager, TokenProvider, TokenAuthenticator
│   ├── monitoring/         # AppLogger with timers, CrashReporter, AnalyticsTracker
│   └── errors/             # GlobalErrorHandler, ApiErrorParser
│
├── shared/
│   ├── data/
│   │   └── models/         # Cross-feature DTO models (UserModel.kt, AddressModel.kt, PaginationModel.kt)
│   ├── domain/
│   │   └── entities/       # Cross-feature domain entities (User.kt, Address.kt, PaginatedResult.kt)
│   ├── ui/                 # Reusable design system Composables, buttons, text fields, theme tokens
│   └── utils/              # General extensions, formatters, DateTime utilities
│
└── features/
    ├── auth/
    │   ├── data/           # Remote API, auth DTOs, repository implementations
    │   ├── domain/         # Repository contracts, auth entities, use cases
    │   └── presentation/   # Composables, screens, AuthViewModel, AuthUiState
    │
    ├── profile/
    │   ├── data/
    │   ├── domain/
    │   └── presentation/
    │
    ├── customers/
    │   ├── data/
    │   ├── domain/
    │   └── presentation/
    │
    └── orders/
        ├── data/
        ├── domain/
        └── presentation/
```

Do not create empty abstraction layers without concrete responsibilities.

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

## Navigation

For multiple screens, use Navigation Compose.
Keep navigation decisions outside business logic.
Pass only IDs between destinations (userId, productId) — load data in destination.

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
