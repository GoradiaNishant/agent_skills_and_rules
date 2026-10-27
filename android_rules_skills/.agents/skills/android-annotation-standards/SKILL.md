---
name: android-annotation-standards
description: "Hilt and Compose annotation governance"
trigger: always_on
alwaysApply: true
globs:
  - "**/*.kt"
---

# Android Annotation Standards Skill

You are an expert Android compiler, framework, and architecture specialist.

## 1. Mission & Core Principles

Annotations provide vital metadata to the Kotlin compiler, Android Gradle Plugin, Hilt/Dagger, Jetpack Compose compiler, Room compiler, ProGuard/R8, and serialization engines.

### Core Principles
1. **Annotations are Not Decorative**: Every annotation alters code generation, compiler optimizations, dependency lifetimes, or runtime reflection. Never apply them indiscriminately.
2. **Distinguish Annotations from Modifiers & Types**:
   - **Annotations**: `@ActivityRetainedScoped`, `@Composable`, `@Serializable`, `@HiltViewModel`.
   - **Kotlin Keywords / Modifiers**: `suspend`, `inline`, `data`, `sealed`, `value`, `reified`, `override`.
   - **Types & APIs**: `Modifier`, `Flow`, `remember`, `LaunchedEffect`, `viewModelScope`.
3. **Strict Scope Discipline**: Never make dependencies `@Singleton` if they belong to a single screen or activity lifecycle. Choose DI scopes strictly based on the component that owns the state.
4. **Library Exclusivity**: Never mix annotations from competing libraries (e.g., Moshi's `@Json` or Gson's `@SerializedName` inside a Kotlinx `@Serializable` class).
5. **Honor Stability Contracts**: `@Stable` and `@Immutable` are solemn contracts with the Compose compiler. Applying them to mutable classes leads to stale UI and skipped recompositions.

---

## 2. Dependency Injection Annotations (Hilt / Dagger)

Control how dependencies are constructed, scoped, and injected across the component hierarchy.

| Annotation | Applicable Component | Lifetime & Purpose |
|---|---|---|
| `@HiltAndroidApp` | `Application` class | Initializes Hilt dependency graph container at app startup. |
| `@AndroidEntryPoint` | `Activity`, `Fragment`, `View`, `Service`, `BroadcastReceiver` | Injects dependencies into Android platform components. |
| `@Inject` | Constructor, Field | Requests injection from Hilt/Dagger. |
| `@Module` | Kotlin `object` or `interface` | Marks a class that provides or binds dependencies. |
| `@InstallIn(...)` | Module | Specifies the Hilt component (`SingletonComponent`, `ActivityRetainedComponent`, etc.) hosting the module. |
| `@Provides` | Module function | Provides dependencies via factory code (e.g., Retrofit, Room, OkHttpClient). |
| `@Binds` | Module abstract function | Binds an interface to an implementation without runtime overhead. |
| `@Singleton` | Class / `@Provides` | Shared across the entire Application lifetime. |
| `@ActivityRetainedScoped` | Class / `@Provides` | Retained across configuration changes (screen rotations); lives as long as the Activity task. |
| `@ActivityScoped` | Class / `@Provides` | Destroyed and recreated on configuration changes; matches Activity window. |
| `@ViewModelScoped` | Class / `@Provides` | Scoped to a single `ViewModel` instance. |
| `@FragmentScoped` | Class / `@Provides` | Scoped to a Fragment lifecycle. |
| `@ServiceScoped` | Class / `@Provides` | Scoped to a Service lifecycle. |
| `@Qualifier` | Custom annotation | Distinguishes multiple bindings of the same type (e.g., Auth vs No-Auth OkHttpClient). |
| `@Named("...")` | Parameter / `@Provides` | String-based qualifier (prefer custom `@Qualifier` for type safety). |
| `@EntryPoint` | Interface | Exposes Hilt bindings to classes not supported by `@AndroidEntryPoint` (e.g., ContentProviders, custom Views). |
| `@AssistedInject` | Constructor | Constructs classes where some parameters are injected by Hilt and others supplied at runtime. |
| `@Assisted` | Constructor parameter | Marks parameters passed at runtime in `@AssistedInject` factories. |

### Scope Hierarchy & Selection Guide
```text
SingletonComponent (@Singleton)
       │
ActivityRetainedComponent (@ActivityRetainedScoped)
       ├── ViewModelComponent (@ViewModelScoped)
       └── ActivityComponent (@ActivityScoped)
              └── FragmentComponent (@FragmentScoped)
```

**Scope Selection Rules:**
- **`@Singleton`**: Repositories, Database DAOs, Network Clients (`OkHttpClient`, `Retrofit`), Session Managers, `DataStore`.
- **`@ActivityRetainedScoped`**: Global UI Event Messengers (`Messenger`), navigation event dispatchers retained across rotation.
- **`@ViewModelScoped`**: State holders or use cases specific to a single ViewModel.
- **Unscoped (no annotation)**: Pure mappers, use cases, formatters, and stateless validators.

---

## 3. Jetpack Compose UI Annotations

Control composition participation, compiler stability contracts, and tooling previews.

| Annotation | Target | Purpose |
|---|---|---|
| `@Composable` | Function, Property getter | Marks code participating in UI composition. |
| `@Preview` | Composable function | Renders a visual preview in Android Studio. |
| `@PreviewLightDark` | Composable function | Multi-preview generating both light and dark theme previews. |
| `@PreviewParameter` | Composable parameter | Supplies mock preview data via a `PreviewParameterProvider`. |
| `@Stable` | Class, Function, Property | Promises Compose that if instances are equal, they remain equal, and public mutability emits notifications. |
| `@Immutable` | Class | Promises Compose that all public properties never change after creation (skips recomposition). |
| `@ReadOnlyComposable` | Composable function | Reads CompositionLocals without creating a restart group or emitting UI nodes (optimizes read-only lookups). |
| `@NonRestartableComposable` | Composable function | Skips generating restart group boilerplate for simple leaf composables that don't need recomposition restart. |
| `@DisallowComposableCalls` | Lambda parameter | Forbids calling `@Composable` functions inside non-composable inline lambdas. |

### Stability Rules for Compose
- External library models or classes with `var` properties are inferred as **Unstable** by Compose, causing full recomposition.
- Use `@Immutable` on UI state data classes where all properties are `val` and collections are wrapped or immutable:
  ```kotlin
  @Immutable
  data class UserProfileUiState(
      val id: String = "",
      val name: String = "",
      val tags: PersistentList<String> = persistentListOf()
  )
  ```
- Use `@ReadOnlyComposable` for helper extensions that only read `LocalContext` or `LocalDensity`:
  ```kotlin
  @Composable
  @ReadOnlyComposable
  fun isCompactWindow(): Boolean = ...
  ```

---

## 4. Android Lifecycle, Architecture & ViewModel

| Annotation | Target | Purpose |
|---|---|---|
| `@HiltViewModel` | ViewModel class | Enables Hilt to generate the ViewModel factory and inject dependencies into its constructor. |
| `@MainThread` | Function, Constructor | Enforces execution on the Android main thread (checked by lint). |
| `@WorkerThread` | Function | Enforces execution on a background/worker thread (checked by lint). |
| `@UiThread` | Function | Indicates execution on the UI thread. |
| `@CallSuper` | Function | Requires overriding subclasses to call `super.functionName()`. |
| `@VisibleForTesting` | Class, Function, Property | Documents that visibility was relaxed exclusively for tests. |
| `@Keep` | Class, Member | Tells ProGuard/R8 to preserve this symbol and its name during shrinking and obfuscation. |

```kotlin
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val messenger: Messenger
) : ViewModel()
```

---

## 5. Kotlin Language Annotations

| Annotation | Purpose | Example |
|---|---|---|
| `@JvmInline` | Declares a zero-allocation value class wrapping a single primitive or object. | `@JvmInline value class UserId(val value: String)` |
| `@JvmStatic` | Generates a true JVM static method for Java/test interoperability. | Companion object helpers |
| `@JvmField` | Exposes a Kotlin property as a public field without getter/setter bytecode. | `@JvmField val CREATOR = ...` |
| `@JvmOverloads` | Generates multiple overloaded constructors/functions for default parameter arguments. | Custom Views |
| `@JvmName("...")` | Avoids JVM signature clashes caused by generics type erasure. | `fun List<String>.hasDuplicates()` |
| `@OptIn(...)` | Explicitly opts into experimental APIs. | `@OptIn(ExperimentalCoroutinesApi::class)` |
| `@Volatile` | Guarantees atomic memory visibility of reads/writes across JVM threads. | Token state caches |
| `@Synchronized` | Synchronizes execution on the target object's monitor. | Legacy thread coordination |
| `@Suppress("...")` | Suppresses specific compiler warnings (use sparingly with justification). | `@Suppress("UNCHECKED_CAST")` |

---

## 6. Retrofit & OkHttp Networking Annotations

Define HTTP contracts and serialization behavior:

| Annotation | Target | Purpose |
|---|---|---|
| `@GET("...")`, `@POST("...")` | API Method | HTTP method and relative endpoint URL. |
| `@PUT("...")`, `@PATCH("...")`, `@DELETE("...")` | API Method | Update and deletion HTTP verbs. |
| `@Path("param")` | Method Parameter | Replaces dynamic path tokens: `@GET("users/{id}")`. |
| `@Query("key")` | Method Parameter | Adds URL query parameters: `?key=value`. |
| `@QueryMap` | Method Parameter | Adds dynamic query parameter maps. |
| `@Body` | Method Parameter | Encodes object into HTTP request body via converter. |
| `@Header("key")` | Method Parameter | Passes runtime HTTP header values. |
| `@Headers("...")` | API Method | Attaches static headers: `@Headers("No-Authentication: true")`. |
| `@Field("key")`, `@FieldMap` | Parameter | Encodes form parameters (requires `@FormUrlEncoded`). |
| `@FormUrlEncoded` | API Method | Specifies `application/x-www-form-urlencoded` request. |
| `@Multipart` | API Method | Specifies `multipart/form-data` request for uploads. |
| `@Part`, `@PartMap` | Parameter | Defines parts in a multipart request. |
| `@Streaming` | API Method | Streams large response body directly without buffering into memory. |

```kotlin
interface UserApi {
    @GET("v1/users/{id}")
    suspend fun getUserProfile(
        @Path("id") id: String,
        @Header("Cache-Control") cacheControl: String? = null
    ): Response<ApiResponse<UserDto>>

    @POST("v1/users")
    suspend fun createUser(
        @Body request: CreateUserRequestDto
    ): Response<ApiResponse<UserDto>>
}
```

---

## 7. JSON Serialization Annotations

**Hard Rule: Never mix annotations between serialization libraries!**

### Kotlinx Serialization (`org.jetbrains.kotlinx:kotlinx-serialization-json`)
| Annotation | Purpose |
|---|---|
| `@Serializable` | Generates serializer code for DTOs and Navigation 2.8+ type-safe routes. |
| `@SerialName("json_key")` | Maps Kotlin property to JSON key name. |
| `@Transient` | Excludes property from serialization (must have a default value). |

```kotlin
@Serializable
data class ProductDto(
    @SerialName("product_id") val id: String? = null,
    @SerialName("unit_price") val price: Double? = null
)
```

### Moshi (`com.squareup.moshi`)
| Annotation | Purpose |
|---|---|
| `@JsonClass(generateAdapter = true)` | Generates compile-time adapter using KSP. |
| `@Json(name = "json_key")` | Maps Kotlin property to JSON key name. |

### Gson (`com.google.code.gson`)
| Annotation | Purpose |
|---|---|
| `@SerializedName("json_key")` | Maps Kotlin property to JSON key name. |
| `@Expose` | Selectively serializes/deserializes fields. |

---

## 8. Room Database Annotations

| Annotation | Target | Purpose |
|---|---|---|
| `@Entity(tableName = "...")` | Data class | Declares a SQLite database table. |
| `@PrimaryKey(autoGenerate = ...)` | Property | Marks the primary key column. |
| `@ColumnInfo(name = "...")` | Property | Customizes table column name and SQLite collation. |
| `@Ignore` | Property, Constructor | Tells Room compiler to ignore a property or constructor. |
| `@Embedded(prefix = "...")` | Property | Flattens a nested object's fields into the parent table. |
| `@Relation` | Property | Fetches parent-child relational entities in join queries. |
| `@Dao` | Interface | Marks a Data Access Object containing SQL operations. |
| `@Query("SELECT ...")` | DAO function | Defines custom SQL query. Returns `Flow<T>` or `suspend fun`. |
| `@Insert(onConflict = ...)` | DAO function | Inserts entities into SQLite. |
| `@Upsert` | DAO function | Inserts or updates entities seamlessly (Room 2.5+). |
| `@Update`, `@Delete` | DAO function | Updates or removes entities by primary key. |
| `@Transaction` | DAO function | Executes statements atomically in a single SQLite transaction. |
| `@Database(entities = [...], version = ...)` | RoomDatabase | Declares SQLite schema and version. |
| `@TypeConverters(...)` | Database, DAO, Entity | Registers custom converters (e.g., Date, List to JSON). |

```kotlin
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "price") val price: Double
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY title ASC")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Upsert
    suspend fun upsertProducts(products: List<ProductEntity>)
}
```

---

## 9. Testing Annotations

| Annotation | Library | Purpose |
|---|---|---|
| `@Test` | JUnit 4 / 5 | Marks a test case method. |
| `@Before`, `@After` | JUnit 4 | Runs setup and teardown before/after each test method. |
| `@BeforeEach`, `@AfterEach` | JUnit 5 | JUnit 5 setup and teardown counterparts. |
| `@RunWith(AndroidJUnit4::class)` | AndroidX Test | Executes instrumented or Robolectric tests with Android runner. |
| `@HiltAndroidTest` | Hilt Testing | Enables Hilt dependency injection in instrumented/Robolectric tests. |
| `@UninstallModules(...)` | Hilt Testing | Replaces production modules with test doubles. |
| `@BindValue` | Hilt Testing | Binds a test mock directly into the Hilt test graph. |
| `@MockK`, `@RelaxedMockK` | MockK | Injects mock instances into test classes. |
| `@Config(sdk = [...])` | Robolectric | Configures Robolectric test SDK and shadow environment. |

---

## 10. WorkManager Background Processing

| Annotation | Target | Purpose |
|---|---|---|
| `@HiltWorker` | Worker class | Injects dependencies into a `CoroutineWorker` via Hilt. |
| `@AssistedInject` | Constructor | Marks worker constructor accepting both Hilt and runtime parameters. |
| `@Assisted` | Parameter | Injected parameters supplied by WorkManager (`Context`, `WorkerParameters`). |

```kotlin
@HiltWorker
class DataSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ProductRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        repository.syncData()
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}
```

---

## 11. Platform Compatibility & Permissions

| Annotation | Target | Purpose |
|---|---|---|
| `@RequiresApi(Build.VERSION_CODES.O)` | Class, Method | Indicates that callers must guard calls or target Android 8.0+. |
| `@RequiresPermission(...)` | Method | Warns callers via Android Lint if the manifest/runtime permission is missing. |
| `@ChecksSdkIntAtLeast(api = ...)` | Function | Tells Android Lint that a helper function checks `Build.VERSION.SDK_INT`. |
| `@SuppressLint("...")` | Any | Suppresses specific Android Lint warnings with documented justification. |

---

## 12. Verification & Review Checklist

When writing or reviewing Android code, verify:
- [ ] **No Scope Leaks**: Activities, Contexts, or Views are **never** injected into `@Singleton` classes.
- [ ] **ViewModel Scoping**: ViewModels are annotated with `@HiltViewModel` and constructor `@Inject`.
- [ ] **UI Stability**: Complex UI state data classes use `@Immutable` with read-only properties (`val`).
- [ ] **Single Serialization Library**: Classes do NOT mix `@Serializable`, `@Json`, and `@SerializedName`.
- [ ] **R8 Obfuscation Safety**: Models serialized via reflection or accessed via JNI have `@Keep`.
- [ ] **Room DAOs**: Relational/multi-table operations have `@Transaction`.
- [ ] **Opt-In Scoped**: Experimental APIs are scoped with `@OptIn` on the function rather than entire files unless necessary.
- [ ] **WorkManager Workers**: Use `@HiltWorker` + `@AssistedInject` with `@Assisted` on `appContext` and `workerParams`.
