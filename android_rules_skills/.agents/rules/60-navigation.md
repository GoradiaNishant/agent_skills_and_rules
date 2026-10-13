# Navigation Rules

@../skills/kotlin-compose-architect/references/03-architecture-decisions.md
@../skills/kotlin-compose-architect/references/10-compose-ui-rules.md

## Navigation & Routing Guidelines

1. **Route Declaration & Type-Safe Destinations**:
   - Use Jetpack Navigation Compose with type-safe routes (Navigation 2.8+ with `@Serializable` classes/objects) or structured sealed route hierarchies:
     ```kotlin
     @Serializable object HomeRoute
     @Serializable data class DetailRoute(val itemId: String)
     ```
   - Centralize navigation graph definitions in `navigation/AppNavHost.kt`.

2. **Decoupled Navigation Execution**:
   - Do NOT pass `NavController` or `NavHostController` deep into screen or component composables.
   - Route composables handle navigation by observing ViewModel events or accepting navigation lambdas:
     ```kotlin
     fun FeatureRoute(
         onNavigateToDetail: (String) -> Unit,
         onNavigateBack: () -> Unit,
         viewModel: FeatureViewModel = hiltViewModel()
     )
     ```
   - Keep screen composables purely event-driven and easily testable/previewable.

3. **Backstack Management**:
   - Prevent duplicate destinations on bottom navigation bars by using:
     ```kotlin
     popUpTo(navController.graph.findStartDestination().id) { saveState = true }
     launchSingleTop = true
     restoreState = true
     ```
   - When completing auth/onboarding flows, pop the auth route off the backstack using `popUpTo(AuthRoute) { inclusive = true }`.

4. **Authentication Guarding & Deep Linking**:
   - Check authentication state prior to rendering protected screens or redirect unauthenticated users to the login route.
   - Configure deep links using `navDeepLink` with typed argument extraction and fallback navigation.
   - For notification-driven deep links, adhere to `@65-notifications-push-management.md` by constructing intents via `TaskStackBuilder` with `PendingIntent.FLAG_IMMUTABLE` to preserve the parent backstack.
