# List, Search, & Pagination Rules

@../skills/kotlin-compose-architect/references/10-compose-ui-rules.md
@../skills/kotlin-compose-architect/references/11-utils-helpers-extensions.md

## Canonical List Rules

1. **View Implementation**:
   - Always use `LazyColumn`, `LazyRow`, or `LazyVerticalGrid` for dynamic or unbounded datasets.
   - Always specify stable unique keys in `items()`:
     ```kotlin
     items(items = state.items, key = { it.id }) { item ->
         ItemCard(item = item)
     }
     ```
   - Support Pull-to-Refresh using Material 3 `PullToRefreshBox` triggering `onRefresh()`.
   - Initial Loading: Display a shimmer placeholder layout or skeleton screen.
   - Pagination Footer: Display a centered `CircularProgressIndicator` item at the bottom of the lazy list when `isPaginating` is true.
   - Empty State: Render an empty state view with title, description, and optional action button when `items.isEmpty()` and not loading.
   - Error State: Render an error banner or screen with an explicit "Retry" button.

2. **ViewModel Implementation**:
   - Manage pagination state flags: `page: Int = 1`, `hasMorePages: Boolean = true`, `isPaginating: Boolean = false`, `isRefreshing: Boolean = false`.
   - Prevent duplicate network triggers: ignore pagination calls if `isPaginating == true`, `isLoading == true`, or `hasMorePages == false`.
   - Handle pull-to-refresh: reset `page = 1`, fetch page 1, replace list upon success.
   - Debounce search input using Kotlin Flow `debounce(400L)` (`AppConstants.DEBOUNCE_SEARCH_MS`), `distinctUntilChanged()`, and `flatMapLatest()`.
   - Set `hasMorePages = false` when returned list size is less than `AppConstants.API_PAGE_SIZE`.
