# Reference 16 — Android Annotation Standards & Scoping Rules

@../skills/android-annotation-standards/SKILL.md

Annotations supply structural metadata used by the Kotlin compiler, Compose runtime, Hilt dependency injector, Retrofit, Room, and R8 shrinking engines.

---

## 1. Scope & Lifecycle Matrix (Hilt / Dagger)

| Scope Annotation | Living Component | Typical Classes | Anti-Pattern to Avoid |
|---|---|---|---|
| `@Singleton` | `SingletonComponent` | Retrofit, OkHttp, Database, Room DAOs, DataStore, `SessionManager`, `TokenProvider` | ❌ Injecting an `Activity` or `Context` into a `@Singleton` (causes memory leaks). |
| `@ActivityRetainedScoped` | `ActivityRetainedComponent` | Global `Messenger`, cross-screen Navigation coordinators | ❌ Storing non-retained Android View references. |
| `@ViewModelScoped` | `ViewModelComponent` | Specialized Screen Use Cases, Screen State reducers | ❌ Marking pure stateless mappers as scoped. |
| `@ActivityScoped` | `ActivityComponent` | Activity-bound dialogs, Window size calculators | ❌ Injecting into a ViewModel or Repository. |
| `@FragmentScoped` | `FragmentComponent` | Fragment-bound adapters | ❌ Injecting into a ViewModel. |
| *(Unscoped)* | No annotation | Pure Use Cases, DTO Mappers, Formatters, Validators | ❌ Unnecessarily scoping stateless helper classes. |

---

## 2. Compose UI Annotations

- **`@Composable`**: Apply to all UI-emitting functions and composable property getters.
- **`@Preview` & `@PreviewLightDark`**: Always provide a private preview composable with realistic mock parameters using `@PreviewParameter`.
- **`@Immutable`**: Apply to UI state data classes where all fields are `val` and collections are immutable or wrapped. Guarantees Compose compiler skips unnecessary recomposition.
- **`@ReadOnlyComposable`**: Apply to composable utility functions that only read `CompositionLocal` values and emit no UI nodes.

```kotlin
@Immutable
data class OrderListUiState(
    val isLoading: Boolean = false,
    val orders: PersistentList<Order> = persistentListOf(),
    val errorMessage: UiText? = null
)
```

---

## 3. Serialization Annotations

**Strict Rule:** Use only the annotations belonging to the project's chosen serialization engine:

- **Kotlinx Serialization**: Use `@Serializable`, `@SerialName("...")`, `@Transient`.
- **Moshi**: Use `@JsonClass(generateAdapter = true)`, `@Json(name = "...")`.
- **Gson**: Use `@SerializedName("...")`, `@Expose`.

Never mix annotations from different libraries on the same model.

---

## 4. WorkManager & Background Tasks

Always construct WorkManager workers using `@HiltWorker` and `@AssistedInject`:

```kotlin
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: SyncRepository
) : CoroutineWorker(appContext, workerParams)
```

---

## 5. Summary Distinction

Remember the difference between:
- **Annotations** (start with `@`): `@ActivityRetainedScoped`, `@Composable`, `@Serializable`, `@HiltViewModel`.
- **Modifiers / Keywords**: `suspend`, `inline`, `data`, `sealed`, `value`, `reified`, `override`.
- **Types / Functions**: `Modifier`, `Flow`, `remember`, `LaunchedEffect`, `viewModelScope`.
