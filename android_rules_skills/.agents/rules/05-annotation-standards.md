# Android Annotation Standards Rules

@../skills/android-annotation-standards/SKILL.md
@../skills/kotlin-compose-architect/references/16-android-annotation-standards.md

## Framework & Annotation Governance

1. **Dependency Injection & Scoping (Hilt)**:
   - **`@Singleton`**: Restrict strictly to long-lived application-wide resources: Network Clients, Retrofit, Database DAOs, Session Managers, Token Stores, and DataStore.
   - **`@ActivityRetainedScoped`**: Use for retained state coordinators (e.g. `Messenger`, multi-screen navigation state) that must survive configuration changes.
   - **`@ViewModelScoped`**: Use for dependencies scoped to a single ViewModel lifecycle.
   - **Unscoped**: Stateless use cases, formatters, validators, and mappers must NOT be scoped.
   - **Never Leak Lifecycles**: Never inject an `Activity`, `Fragment`, `View`, or non-application `Context` into `@Singleton` or `@ActivityRetainedScoped` components.

2. **Jetpack Compose UI Annotations**:
   - Every Composable function must be marked with `@Composable`.
   - Use `@Immutable` on UI state data classes when all properties are `val` to guarantee optimal skipping of recomposition.
   - Use `@ReadOnlyComposable` for composable functions that only read `CompositionLocal` and emit no UI nodes.
   - Every feature screen must have a private `@Preview` or `@PreviewLightDark` composable.

3. **ViewModel & Lifecycle**:
   - Every ViewModel managed by Hilt must be annotated with `@HiltViewModel` and have `@Inject constructor(...)`.
   - Use `@MainThread` or `@WorkerThread` on methods with strict threading assumptions.

4. **Data Serialization & Network**:
   - Use only the annotations corresponding to the project's chosen serializer. Never mix Kotlinx Serialization (`@Serializable`, `@SerialName`) with Moshi (`@Json`) or Gson (`@SerializedName`).
   - Type-safe navigation routes (Navigation 2.8+) must be annotated with `@Serializable`.
   - ProGuard/R8: Annotate models accessed via JNI or reflection with `@Keep`.

5. **Room SQLite Persistence**:
   - Entities must be annotated with `@Entity(tableName = "...")` and `@PrimaryKey`.
   - Multi-step or relational DAO queries must be annotated with `@Transaction`.

6. **WorkManager**:
   - Background workers must use `@HiltWorker` with `@AssistedInject` on the constructor, marking `appContext` and `workerParams` with `@Assisted`.
