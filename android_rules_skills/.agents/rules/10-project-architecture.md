# Project Architecture & Module Structure

@../skills/kotlin-compose-architect/references/03-architecture-decisions.md
@../skills/kotlin-compose-architect/references/05-implementation-rules.md
@05-annotation-standards.md

## Three-Tier Project Organization & Module Structure

1. **Three-Tier Project Structure**:
   - `core/`: Application-wide infrastructural components: dependency injection (`di/`), OkHttp/Retrofit clients & interceptors (`network/`), session management & secure token handling (`session/`), centralized telemetry & timers (`monitoring/`), and global error handling (`errors/`).
   - `shared/`: Cross-feature components shared across multiple domains:
     - `data/models/`: Shared DTOs (`UserModel.kt`, `AddressModel.kt`, `PaginationModel.kt`).
     - `domain/entities/`: Shared domain entities (`User.kt`, `Address.kt`, `PaginatedResult.kt`).
     - `ui/`: Reusable Compose design system components, buttons, text fields, theme tokens.
     - `utils/`: Common extensions, formatters, and date/time helpers.
   - `features/`: Feature-first modular packages (`auth/`, `profile/`, `customers/`, `orders/`). Each feature encapsulates its own `data/`, `domain/`, and `presentation/` layers.

2. **Presentation Layer (`features/<name>/presentation/`, `shared/ui/`)**:
   - Separate route from screen: `<Name>Route` collects `StateFlow` lifecycle-safely and handles navigation events; `<Name>Screen` is stateless and renders `UiState`.
   - ViewModels extend AndroidX `ViewModel`, injected via `@HiltViewModel`.
   - Unidirectional Data Flow (UDF): ViewModels expose immutable `StateFlow<UiState>` and process events via sealed interfaces.
   - Do NOT reference data layer classes (Retrofit APIs, Room DAOs, DTOs) directly from Composables or ViewModels.

3. **Domain Layer (`domain/`, `features/<name>/domain/`)**:
   - Houses pure Kotlin business logic: UseCases, repository interfaces, and clean domain entities.
   - Independent of Android framework dependencies (no `Context`, no UI, no database/network SDKs).
   - UseCases are conditional: create a UseCase when there is a business action, rule, validation, transformation, or multi-repository orchestration. Never create 1-to-1 pass-through UseCases that merely forward a single repository call.
   - Clean Architecture does NOT forbid ViewModels from depending directly on repository interfaces: for simple CRUD without business logic, ViewModels can consume repositories directly.
   - Avoid "God UseCases": group UseCases around distinct, cohesive business operations rather than dumping an entire feature's APIs into a single giant UseCase.
   - Guiding principle: *"Where does this business responsibility belong?"*, not *"How many Use Cases can I create?"* Clean Architecture is about separation of responsibilities, not class count.

4. **Data Layer (`data/`, `features/<name>/data/`)**:
   - Implements repository interfaces (`<Name>RepositoryImpl`).
   - Encapsulates remote data sources (Retrofit APIs and serializable DTOs) and local data sources (Room DAOs and DataStore).
   - Maps DTOs and database entities to clean domain models before returning them to upper layers.
   - Wraps network/disk operations into `Result<T>` or `NetworkResult<T>` to ensure graceful error handling.

5. **Dependency Injection (`core/di/`)**:
   - Central Hilt modules (`@Module`, `@InstallIn(SingletonComponent::class)` or `@ViewModelScoped`).
   - Network components (`Retrofit`, `OkHttpClient`, API interfaces) provided in `NetworkModule`.
   - Database components (`RoomDatabase`, DAOs) provided in `DatabaseModule`.
   - Repository bindings provided in `RepositoryModule` via `@Binds`.

6. **Generated Files**:
   - Do NOT edit generated files (Room `*_Impl.kt`, Hilt `Dagger*_Component`, KSP/KAPT generated code) manually.
   - Run `./gradlew assembleDebug` or build tasks to verify generated sources after schema or annotation changes.
