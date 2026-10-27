# Navigation & Screen-ViewModel Architecture Rules

@../skills/android-navigation-architecture/SKILL.md
@../skills/kotlin-compose-architect/references/03-architecture-decisions.md
@../skills/kotlin-compose-architect/references/10-compose-ui-rules.md
@10-project-architecture.md
@30-state-management.md

## 1. Route Declaration & Destination Modeling

1. **Structured Destination Hierarchy**:
   - Define all navigation targets centrally in `navigation/Destination.kt` using sealed or structured `Screen` and `DynamicScreen<T>` hierarchies.
   - Static screens inherit from `Screen(baseRoute: String)`:
     ```kotlin
     data object Splash : Screen("splash")
     data object Login : Screen("login")
     ```
   - Parameterized screens inherit from `DynamicScreen<T : Any>(baseRoute, routeArgName, navType)`:
     ```kotlin
     data object StockDetails : DynamicScreen<String>(
         baseRoute = "stock-details",
         routeArgName = "stockId",
         navType = serializableNavType<String>()
     )
     data object NewsDetail : DynamicScreen<NewsItem>(
         baseRoute = "news-detail",
         routeArgName = "newsItem",
         navType = serializableNavType<NewsItem>()
     )
     ```

2. **Type-Safe Complex Arguments via `SerializableNavType<T>`**:
   - For complex data models passed in route paths, use custom `NavType<T>` powered by `kotlinx.serialization` and URL encoding/decoding:
     ```kotlin
     inline fun <reified T : Any> serializableNavType(): NavType<T> =
         SerializableNavType(serializer(), typeOf<T>())
     ```
   - Ensure `@Serializable` models used in routes are stable and immutable.
   - Pass concise route data or IDs. For rich multi-field entities, pass typed args objects (e.g. `PdfViewerArgs`, `TradingChartArgs`).

3. **Query Parameter Routes**:
   - For screens with optional or multi-variable query parameters, define explicit query routes with nullability and default values:
     ```kotlin
     data object AiChat : Screen("ai-chat") {
         const val argTicker = "ticker"
         override val route = "ai-chat?$argTicker={$argTicker}"
         val navArguments = listOf(navArgument(argTicker) {
             type = NavType.StringType
             nullable = true
             defaultValue = null
         })
         fun dynamicRoute(ticker: String? = null): String =
             if (!ticker.isNullOrEmpty()) "ai-chat?$argTicker=${Uri.encode(ticker)}" else "ai-chat"
     }
     ```

---

## 2. Decoupled Navigation Execution (`Navigator` + `NavHandler`)

1. **STRICT RULE: No `NavController` or Result Callbacks in Composables**:
   - Never pass `NavController` / `NavHostController` into Screen or Route composables (`navController: NavController? = null` is strictly forbidden).
   - Never pass result callbacks (`onBackWithResult`, `onStockSelected`, etc.) into Screen or Route composables.
   - All navigation operations, argument parsing, and result returning MUST be executed exclusively within the ViewModel.

2. **ActivityRetainedScoped `Navigator`**:
   - Provide an `@ActivityRetainedScoped class Navigator @Inject constructor()` that exposes navigation and result event channels (`SharedFlow`):
     - `navigate: SharedFlow<NavTarget>` (handles plain navigate, popBackstack before navigate, and `popUpTo` semantics)
     - `deeplink: SharedFlow<NavDeeplink>`
     - `back: SharedFlow<NavBack>` (supports standard pop and `recreate = true`)
     - `results: SharedFlow<Pair<String, Any>>` (inter-ViewModel result communication)
     - `end: SharedFlow<Boolean>` (finish Activity)
   - ViewModels inject `Navigator` directly and trigger navigation actions without UI context:
     ```kotlin
     navigator.navigateTo(Destination.StockDetails.dynamicRoute(stockId))
     navigator.navigateTo(Destination.Home.route, popUpToRoute = Destination.Login.route, inclusive = true)
     navigator.navigateBack()
     navigator.navigateBackWithResult(NavigationKeys.KEY_ITEM_UPDATED, true)
     ```

3. **Composable `NavHandler` Bridge**:
   - Inside the root composable host (`TradingApp` / `AppNavHost`), mount `NavHandler(navController, navigator, finish)` inside a `LaunchedEffect("navigation")` to observe Navigator flows and invoke `navController` methods.

---

## 3. Pure Screen vs. Route Separation Pattern

1. **`<Feature>Route` (Stateful Destination)**:
   - Serves as the navigation node registered in `NavGraph.kt`.
   - Injects the ViewModel via `hiltViewModel()`. Accepts ONLY the ViewModel:
     ```kotlin
     @Composable
     fun StockDetailsRoute(viewModel: StockDetailsViewModel = hiltViewModel())
     ```
   - Collects `uiState` via `collectAsState()` or `collectAsStateWithLifecycle()`.
   - Hooks into lifecycle events (`LifecycleEventEffect(Lifecycle.Event.ON_RESUME)` / `ON_PAUSE`) for active subscriptions (e.g. WebSockets, polling).
   - Delegates rendering directly to `<Feature>Screen`.
   - Contains NO `navController` and NO navigation callbacks.

2. **`<Feature>Screen` (Stateless UI)**:
   - Pure UI composable that accepts ONLY `uiState: <Feature>UiState` and `onEvent: (<Feature>Event) -> Unit`:
     ```kotlin
     @Composable
     fun StockDetailsScreen(
         uiState: StockDetailsUiState,
         onEvent: (StockDetailsEvent) -> Unit
     )
     ```
   - Completely free of Hilt, ViewModels, NavController, or navigation lambdas.
   - Provides `@Preview` variants for all states (Loading, Content, Empty, Error).

---

## 4. ViewModel-Driven Bidirectional Data Passing

1. **Forward Passing (Route Args -> ViewModel via `SavedStateHandle`)**:
   - Destination ViewModel constructor injects `savedStateHandle: SavedStateHandle`.
   - Extracts arguments directly inside the ViewModel:
     ```kotlin
     val item: NewsItem = checkNotNull(savedStateHandle.getArguments<NewsItem>(Destination.NewsDetail.routeArgName))
     ```
   - Uses `SavedStateHandle.getArguments<T>(argName)` with JSON and URL fallback decoding.

2. **Backward Passing (Inter-ViewModel Result Communication)**:
   - **Child ViewModel**: Returns result and navigates back in a single operation:
     ```kotlin
     navigator.navigateBackWithResult(NavigationKeys.KEY_ITEM_UPDATED, true)
     ```
   - **Parent ViewModel**: Injects `Navigator` and observes results in `viewModelScope`:
     ```kotlin
     init {
         viewModelScope.launch {
             navigator.observeResult<Boolean>(NavigationKeys.KEY_ITEM_UPDATED).collect { isUpdated ->
                 if (isUpdated) refreshData()
             }
         }
     }
     ```
   - Zero callback plumbing through Compose layers.

3. **Sub-Graph Shared ViewModels**:
   - For multi-step wizards or grouped sub-flows, use `NavBackStackEntry.sharedViewModel<T>(navController)` in `NavGraph.kt` to scope the ViewModel to the parent navigation graph rather than recreating state per screen.

---

## 5. Backstack & Transition Management

1. **Bottom Navigation Backstack**:
   - Prevent duplicate destinations and state leaks on bottom bar tab switching:
     ```kotlin
     navController.navigate(route) {
         popUpTo(navController.graph.findStartDestination().id) { saveState = true }
         launchSingleTop = true
         restoreState = true
     }
     ```

2. **Authentication Flow Popping**:
   - When completing login/onboarding or forced logout, pop the auth/root graph:
     ```kotlin
     navigator.navigateTo(Destination.Home.route, popUpToRoute = Destination.Login.route, inclusive = true)
     ```

3. **Standard Animated Transitions (`navComposable`)**:
   - Wrap routes using `navComposable` with standardized slide-in/out, fade, and scale transitions via `TransitionAnimations`.
   - Bottom tabs must use subtle fade transitions without horizontal sliding.

4. **Deep Linking with Preserved Backstack**:
   - Configure deep links using `navDeepLink { uriPattern = "..." }`.
   - For notification deep links, construct pending intents via `TaskStackBuilder` with `PendingIntent.FLAG_IMMUTABLE` to preserve the parent backstack hierarchy as specified in `@65-notifications-push-management.md`.
