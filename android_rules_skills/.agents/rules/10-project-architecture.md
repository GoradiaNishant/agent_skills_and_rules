# Project Architecture & Module Structure

@../skills/kotlin-compose-architect/references/03-architecture-decisions.md
@../skills/kotlin-compose-architect/references/05-implementation-rules.md
@05-annotation-standards.md
@60-navigation.md

## Clean Architecture & Modern Android Architecture (MAD) Standards

1. **Modern Android Architecture Layering (MAD Standards)**:
   - Follow Google's official Guide to App Architecture: **UI Layer**, optional **Domain Layer**, and **Data Layer**.
   - **Dependencies flow strictly inward**: UI depends on Domain/Data; Domain depends on Data contracts; Data never depends on Domain or UI.
   - Avoid catch-all `shared/` directories. Use explicit functional boundaries:
     - **Feature-First**: `core/` (`network/`, `database/`, `datastore/`, `designsystem/`, `model/`, `navigation/`, `common/`) + `features/<feature>/` (`presentation/`, `domain/`, `data/`).
     - **Layer-First**: `ui/` (`screen/`, `component/`, `navigation/`, `theme/`), `data/` (`remote/`, `local/`, `repository/`, `model/`, `socket/`), `domain/` (`usecase/`, `model/`), `di/`, `utils/`.

2. **UI Layer (`presentation/` or `ui/screen/`)**:
   - Separate route from screen: `<Name>Route` collects `StateFlow` lifecycle-safely and hooks into lifecycles; `<Name>Screen` is stateless and renders `UiState`.
   - ViewModels extend AndroidX `ViewModel`, injected via `@HiltViewModel`.
   - Constructor injects `Navigator` and `SavedStateHandle` exclusively for navigation, route argument parsing, and result communication.
   - Unidirectional Data Flow (UDF): ViewModels expose immutable `StateFlow<UiState>` and process events via sealed interfaces (`UiEvent`).
   - Do NOT reference data layer classes (Retrofit APIs, Room DAOs, DTOs) directly from Composables.

3. **Domain Layer (`domain/` or `features/<name>/domain/`)**:
   - Houses pure Kotlin business logic: UseCases, repository interfaces, and clean domain entities.
   - Independent of Android framework dependencies (no `Context`, no UI, no database/network SDKs).
   - UseCases are conditional: create a UseCase when there is a business action, rule, validation, transformation, or multi-repository orchestration. Never create 1-to-1 pass-through UseCases that merely forward a single repository call.
   - Clean Architecture does NOT forbid ViewModels from depending directly on repository interfaces: for simple CRUD without business logic, ViewModels can consume repositories directly.
   - Avoid "God UseCases": group UseCases around distinct, cohesive business operations rather than dumping an entire feature's APIs into a single giant UseCase.
   - Guiding principle: *"Where does this business responsibility belong?"*, not *"How many Use Cases can I create?"* Clean Architecture is about separation of responsibilities, not class count.

4. **Data Layer (`data/` or `features/<name>/data/`)**:
   - Implements repository interfaces (`<Name>RepositoryImpl`) acting as the Single Source of Truth (SSOT).
   - Encapsulates remote data sources (Retrofit APIs and serializable DTOs) and local data sources (Room DAOs and DataStore).
   - Maps DTOs and database entities to clean domain models before returning them to upper layers.
   - Wraps network/disk operations into `Result<T>` or `NetworkResult<T>` to ensure graceful error handling.

5. **Dependency Injection (`di/` or `core/di/`)**:
   - Central Hilt modules (`@Module`, `@InstallIn(SingletonComponent::class)` or `@ViewModelScoped`).
   - Network components (`Retrofit`, `OkHttpClient`, API interfaces) provided in `NetworkModule`.
   - Database components (`RoomDatabase`, DAOs) provided in `DatabaseModule`.
   - Repository bindings provided in `RepositoryModule` via `@Binds`.

6. **Generated Files**:
   - Do NOT edit generated files (Room `*_Impl.kt`, Hilt `Dagger*_Component`, KSP/KAPT generated code) manually.
   - Run `./gradlew assembleDebug` or build tasks to verify generated sources after schema or annotation changes.
