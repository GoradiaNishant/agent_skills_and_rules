---
name: android-navigation-architecture
description: "Decoupled navigation, Navigator, and ViewModel data passing"
trigger: always_on
alwaysApply: true
globs:
  - "**/navigation/**"
  - "**/*Route.kt"
---

# Android Navigation & Screen-ViewModel Architecture Skill

This skill provides comprehensive instructions, patterns, and production-tested templates for architecting scalable, decoupled, and type-safe navigation and screen state management in Jetpack Compose applications.

Derived from production financial and modular trading applications (e.g. Portly), this architecture strictly separates UI rendering from navigation mechanics.

---

## 1. The Strict Non-Negotiable Rules

> 🚫 **NEVER PASS `NavController` INTO SCREENS OR ROUTES**  
> Composables must NEVER accept `navController: NavController? = null` or reference navigation controllers.
>
> 🚫 **NEVER PASS RESULT CALLBACKS INTO SCREENS OR ROUTES**  
> Composables must NEVER accept `onBackWithResult: ((Boolean) -> Unit)? = null` or navigation lambdas.
>
> ✅ **INJECT `Navigator` AND `SavedStateHandle` EXCLUSIVELY IN VIEWMODELS**  
> All forward navigation, back navigation, route argument parsing, and bidirectional result communication MUST live inside `@HiltViewModel` classes via `Navigator` and `SavedStateHandle`.
>
> ✅ **PURE COMPOSABLE CONTRACTS**  
> - `<Feature>Route(viewModel: FeatureViewModel = hiltViewModel())`: Takes ONLY the ViewModel, handles lifecycle effects, and delegates to the screen.
> - `<Feature>Screen(uiState: FeatureUiState, onEvent: (FeatureEvent) -> Unit)`: Pure, stateless, and 100% previewable.

---

## 2. Core Architectural Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                      ViewModel (All Logic & Navigation)                │
│  - Constructor injects SavedStateHandle (reads typed route arguments)  │
│  - Constructor injects @ActivityRetainedScoped Navigator               │
│  - Navigates: navigator.navigateTo(destination)                        │
│  - Navigates Back: navigator.navigateBack()                            │
│  - Returns Result: navigator.navigateBackWithResult(KEY, result)       │
│  - Observes Result: navigator.observeResult<T>(KEY)                    │
│  - ZERO references to NavController, Context, or UI Callbacks          │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ emits via SharedFlow
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                 NavHandler (@Composable Bridge in Root NavHost)        │
│  - Observes Navigator flows inside LaunchedEffect("navigation")        │
│  - Executes real operations on NavController: navigate(), popBack()    │
│  - Dispatches popUpTo, launchSingleTop, deep links, and finish()       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ controls
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   NavHost / NavGraphBuilder Registry                   │
│  - navComposable() with TransitionAnimations (slide, fade, scale)      │
│  - Destination declarations with typed DynamicScreen<T>                │
│  - SerializableNavType<T> encoding with kotlinx.serialization          │
└───────────────────┬────────────────────────────────┬───────────────────┘
                    │ renders                        │ renders
                    ▼                                ▼
┌───────────────────────────────────────┐ ┌──────────────────────────────┐
│       <Feature>Route (Stateful)       │ │ <Feature>Screen (Stateless)  │
│ - signature: fun FeatureRoute(        │ │ - signature: fun FeatureScreen(
│     viewModel = hiltViewModel()       │ │     uiState: FeatureUiState, │
│   )                                   │ │     onEvent: (Event) -> Unit │
│ - LifecycleEventEffect hooks          │ │   )                          │
│ - Passes uiState & viewModel::onEvent │ │ - Pure UI, 100% Previewable  │
│ - NO NavController, NO callbacks      │ │ - NO NavController, NO calls │
└───────────────────────────────────────┘ └──────────────────────────────┘
```

---

## 3. Directory & File Organization

Place navigation core infrastructure in `core/navigation/` (or `ui/navigation/` in monolithic layouts):

```text
core/navigation/
├── Destination.kt               # Central Screen & DynamicScreen definitions
├── NavigationKeys.kt            # String constants for inter-screen communication
├── Navigator.kt                 # Decoupled @ActivityRetainedScoped coordinator & result bus
├── NavHandler.kt                # Composable bridge observing Navigator and driving NavController
├── NavUtils.kt                  # navComposable DSL extension & sharedViewModel<T>()
├── SerializableNavType.kt       # Custom NavType<T> using kotlinx.serialization
└── TransitionAnimations.kt      # Enter/exit/pop animations (slide, fade, scale)
```

Feature presentation layers follow:
```text
features/<feature_name>/presentation/
├── <Feature>Route.kt            # Stateful composable: fun <Feature>Route(viewModel = hiltViewModel())
├── <Feature>Screen.kt           # Pure UI composable: fun <Feature>Screen(uiState, onEvent)
├── <Feature>ViewModel.kt        # Injects SavedStateHandle and Navigator exclusively
├── <Feature>UiState.kt          # Immutable state data class
└── <Feature>Event.kt            # Sealed interface of user/lifecycle actions
```

---

## 4. Step-by-Step Implementation Guide

### Step 1: Destination Declarations (`Destination.kt`)

Model destinations using `Screen` for static routes and `DynamicScreen<T>` for parameterized routes:

```kotlin
object Destination {
    const val BASE_DEEPLINK_URL = "app://example.com"

    // 1. Static screen
    data object Login : Screen("login")

    // 2. Dynamic screen with primitive argument
    data object StockDetails : DynamicScreen<String>(
        baseRoute = "stock-details",
        routeArgName = "stockId",
        navType = serializableNavType<String>()
    )

    // 3. Dynamic screen with complex @Serializable object
    data object NewsDetail : DynamicScreen<NewsItem>(
        baseRoute = "news-detail",
        routeArgName = "newsItem",
        navType = serializableNavType<NewsItem>()
    )

    // 4. Query parameter screen with optional parameters
    data object AiChat : Screen("ai-chat") {
        const val argTicker = "ticker"
        const val argStockName = "stock_name"

        override val route = "ai-chat?$argTicker={$argTicker}&$argStockName={$argStockName}"

        val navArguments = listOf(
            navArgument(argTicker) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(argStockName) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )

        fun dynamicRoute(ticker: String? = null, stockName: String? = null): String {
            return buildString {
                append("ai-chat")
                val params = mutableListOf<String>()
                if (!ticker.isNullOrEmpty()) params.add("$argTicker=${Uri.encode(ticker)}")
                if (!stockName.isNullOrEmpty()) params.add("$argStockName=${Uri.encode(stockName)}")
                if (params.isNotEmpty()) {
                    append("?")
                    append(params.joinToString("&"))
                }
            }
        }
    }
}
```

---

### Step 2: The Decoupled Navigator with Result Support (`Navigator.kt`)

Provide an `@ActivityRetainedScoped` `Navigator` that manages both navigation events and inter-screen results:

```kotlin
@ActivityRetainedScoped
class Navigator @Inject constructor() {
    private val _navigate = MutableSharedFlow<NavTarget>(extraBufferCapacity = 1)
    private val _deeplink = MutableSharedFlow<NavDeeplink>(extraBufferCapacity = 1)
    private val _back = MutableSharedFlow<NavBack>(extraBufferCapacity = 1)
    private val _end = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    private val _results = MutableSharedFlow<Pair<String, Any>>(extraBufferCapacity = 5)

    val navigate = _navigate.asSharedFlow()
    val deeplink = _deeplink.asSharedFlow()
    val back = _back.asSharedFlow()
    val end = _end.asSharedFlow()
    val results = _results.asSharedFlow()

    fun navigateTo(route: String, popBackstack: Boolean = false) {
        _navigate.tryEmit(NavTarget(route, popBackstack))
    }

    fun navigateTo(route: String, popUpToRoute: String, inclusive: Boolean = true) {
        _navigate.tryEmit(NavTarget(route, false, popUpToRoute, inclusive))
    }

    fun navigateBack(recreate: Boolean = false) {
        _back.tryEmit(NavBack(recreate))
    }

    /** Sends result to calling ViewModel and pops backstack */
    fun <T : Any> navigateBackWithResult(key: String, result: T) {
        _results.tryEmit(key to result)
        navigateBack()
    }

    /** Observe results in the calling ViewModel */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> observeResult(key: String): Flow<T> {
        return results
            .filter { it.first == key }
            .map { it.second as T }
    }

    fun finish() {
        _end.tryEmit(true)
    }
}
```

---

### Step 3: Pure Route vs. Screen Separation

#### Route Composable
Accepts ONLY the ViewModel. Never accepts `NavController` or navigation/result callbacks:
```kotlin
@Composable
fun StockDetailsRoute(
    viewModel: StockDetailsViewModel = hiltViewModel()
) {
    AppStatusBar()
    val uiState by viewModel.uiState.collectAsState()

    // Lifecycle hooks for sockets, polling, or screen tracking
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onEvent(StockDetailsEvent.OnResume)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.onEvent(StockDetailsEvent.OnPause)
    }

    StockDetailsScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}
```

#### Screen Composable
Pure UI composable. Accepts ONLY `uiState` and `onEvent`:
```kotlin
@Composable
fun StockDetailsScreen(
    uiState: StockDetailsUiState,
    onEvent: (StockDetailsEvent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.title) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(StockDetailsEvent.BackClicked) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Render UI content
    }
}
```

---

### Step 4: ViewModel Implementation (Arguments, Navigation & Results)

#### A. Forward Arguments Retrieval via `SavedStateHandle`
The ViewModel constructor injects `savedStateHandle`:
```kotlin
@HiltViewModel
class StockDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigator: Navigator,
    private val repository: StockRepository,
) : ViewModel() {

    // Retrieve string route argument
    private val stockId: String = checkNotNull(
        savedStateHandle[Destination.StockDetails.routeArgName]
    ).toString().trim('"')

    // Or retrieve typed complex @Serializable argument:
    // private val args: NewsItem = checkNotNull(savedStateHandle.getArguments<NewsItem>(...))

    private val _uiState = MutableStateFlow(StockDetailsUiState(stockId = stockId))
    val uiState: StateFlow<StockDetailsUiState> = _uiState.asStateFlow()
```

#### B. Forward Navigation via `Navigator`
```kotlin
    fun onEvent(event: StockDetailsEvent) {
        when (event) {
            is StockDetailsEvent.ChartClicked -> {
                navigator.navigateTo(Destination.TradingChart.dynamicRoute(event.symbol))
            }
            StockDetailsEvent.BackClicked -> {
                navigator.navigateBack()
            }
        }
    }
```

#### C. Backward Result Passing via `Navigator`
When Screen B needs to return a result to Screen A:

1. **Child Screen B's ViewModel**:
```kotlin
@HiltViewModel
class EditWatchlistViewModel @Inject constructor(
    private val navigator: Navigator,
    ...
) : ViewModel() {
    fun onSaveClicked() {
        // Sets result and navigates back directly in ViewModel
        navigator.navigateBackWithResult(NavigationKeys.KEY_WATCHLIST_UPDATED, true)
    }
}
```

2. **Parent Screen A's ViewModel**:
```kotlin
@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val navigator: Navigator,
    ...
) : ViewModel() {

    init {
        observeChildResults()
    }

    private fun observeChildResults() {
        viewModelScope.launch {
            navigator.observeResult<Boolean>(NavigationKeys.KEY_WATCHLIST_UPDATED).collect { updated ->
                if (updated) {
                    refreshWatchlist()
                }
            }
        }
    }
}
```
**No plumbing through composables!** Neither `WatchlistRoute`, `WatchlistScreen`, `EditWatchlistRoute`, nor `EditWatchlistScreen` contains any callbacks or `NavController` references.

---

### Step 5: Root Navigation Graph Setup (`NavGraph.kt`)

In `NavGraph.kt`, register routes cleanly without passing any callbacks:

```kotlin
@Composable
fun AppNavHost(
    navController: NavHostController,
    navigator: Navigator,
    finish: () -> Unit
) {
    // Mount the bridge
    NavHandler(
        navController = navController,
        navigator = navigator,
        finish = finish
    )

    NavHost(
        navController = navController,
        startDestination = Destination.Login.route
    ) {
        navComposable(Destination.Login.route) {
            LoginRoute()
        }

        navComposable(
            route = Destination.StockDetails.route,
            arguments = Destination.StockDetails.navArguments
        ) {
            StockDetailsRoute()
        }

        navComposable(
            route = Destination.EditWatchlist.route,
            arguments = Destination.EditWatchlist.navArguments
        ) {
            EditWatchlistRoute()
        }
    }
}
```

---

## 5. Architectural Checklist

Before approving or finalizing any screen or navigation code, strictly verify:

- [ ] **NO `NavController` in Screen or Route**: Zero instances of `NavController` in composable parameters.
- [ ] **NO callback lambdas in Screen or Route for navigation**: Zero instances of `onBackWithResult`, `onStockSelected`, etc.
- [ ] **`Navigator` and `SavedStateHandle` injected into ViewModel**: Constructor injection only (`@Inject constructor(savedStateHandle: SavedStateHandle, navigator: Navigator)`).
- [ ] **Arguments parsed in ViewModel**: Initial arguments extracted via `savedStateHandle[key]` or `savedStateHandle.getArguments<T>(key)`.
- [ ] **Navigation triggered by ViewModel**: All `navigator.navigateTo()`, `navigator.navigateBack()`, and `navigator.navigateBackWithResult()` calls executed in `ViewModel.onEvent()`.
- [ ] **Route composable takes only ViewModel**: `fun <Name>Route(viewModel: <Name>ViewModel = hiltViewModel())`.
- [ ] **Screen composable is pure**: `fun <Name>Screen(uiState: <Name>UiState, onEvent: (<Name>Event) -> Unit)`.
- [ ] **Animated Transitions**: Handled centrally via `navComposable()` and `TransitionAnimations.kt`.

---

## 6. Template References

Use the pre-built templates in the `templates/` folder:
- [Navigator.kt](./templates/Navigator.kt) — ActivityRetainedScoped navigation flow emitter with built-in result bus
- [NavHandler.kt](./templates/NavHandler.kt) — Composable navigation bridge
- [Destination.kt](./templates/Destination.kt) — Central route and argument registry
- [SerializableNavType.kt](./templates/SerializableNavType.kt) — Custom NavType with kotlinx.serialization
- [NavUtils.kt](./templates/NavUtils.kt) — `navComposable` DSL & `sharedViewModel<T>()`
- [TransitionAnimations.kt](./templates/TransitionAnimations.kt) — Slide, fade, and scale transition specs
- [SavedStateHandleExtensions.kt](./templates/SavedStateHandleExtensions.kt) — Type-safe argument extraction
- [RouteScreenTemplate.kt](./templates/RouteScreenTemplate.kt) — Pure Route vs Screen + ViewModel implementation
