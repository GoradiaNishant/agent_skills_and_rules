---
name: 30-state-management
description: "MVI and reactive state management (UiState, UiEvent, StateFlow)"
trigger: always_on
alwaysApply: true
globs:
  - "**/*ViewModel.kt"
  - "**/*UiState.kt"
  - "**/*Event.kt"
---

# State Management Rules

@../skills/kotlin-compose-architect/references/03-architecture-decisions.md
@../skills/kotlin-compose-architect/references/10-compose-ui-rules.md

## ViewModel & State Management Implementation Rules

1. **ViewModel Architecture & DI**:
   - ViewModels must extend `androidx.lifecycle.ViewModel` and be annotated with `@HiltViewModel`.
   - Dependencies must be injected via constructor using `@Inject constructor(...)`.
   - Never inject `Context` or Activity references into a ViewModel; use `Application` only if AndroidViewModel is strictly required.

2. **Immutable UI State Design**:
   - Define a single immutable `data class <Feature>UiState` representing the entire screen state.
   - Supply sensible default values for all state fields.
   - Model all meaningful conditions explicitly:
     - `isLoading: Boolean = false`
     - `isRefreshing: Boolean = false`
     - `isPaginating: Boolean = false`
     - `error: UiError? = null` (or String/Int resource ID)
     - `data: List<Item> = emptyList()`
   - Do NOT scatter multiple disconnected `StateFlow` primitives across a ViewModel for a single screen.

3. **Unidirectional Data Flow (UDF)**:
   - ViewModel exposes state via `val uiState: StateFlow<UiState> = _uiState.asStateFlow()`.
   - UI emits user actions through a sealed interface/class: `sealed interface <Feature>UiEvent`.
   - Composables collect state using `collectAsStateWithLifecycle()`.

4. **One-Off UI Events / Side Effects**:
   - For one-off occurrences (Snackbars, toasts, navigation commands), use a Kotlin `Channel<UiEffect>(Channel.BUFFERED)` exposed as `Flow<UiEffect>`.
   - Consume effects in Compose using `LaunchedEffect(Unit)` or `LaunchedEffect(viewModel)`.

5. **Search Debouncing & Stream Operators**:
   - Use Flow operators (`debounce(400L)`) with `distinctUntilChanged()` and `flatMapLatest()` for search inputs to prevent redundant API queries.
   - Ensure the debounce delay is set to 400ms (`AppConstants.DEBOUNCE_SEARCH_MS`).

6. **Coroutine Scope, Cancellation Safety & Exception Handling**:
   - Always launch coroutines within `viewModelScope`; never use `GlobalScope`.
   - **Never swallow `CancellationException`**: In `try-catch` blocks wrapping suspending operations, either rethrow `CancellationException` first or check `if (e is CancellationException) throw e`. Swallowing it breaks structured concurrency and child job cancellation propagation.
     ```kotlin
     viewModelScope.launch {
         try {
             val result = repository.fetchData()
             _uiState.update { it.copy(data = result, isLoading = false) }
         } catch (e: CancellationException) {
             throw e // MUST rethrow to propagate cancellation
         } catch (e: Exception) {
             _uiState.update { it.copy(error = e.toUiError(), isLoading = false) }
         }
     }
     ```

7. **Flow Sharing Policy (`stateIn`)**:
   - When converting upstream cold repository flows into `StateFlow` for screens, use `SharingStarted.WhileSubscribed(5_000)`:
     ```kotlin
     val uiState: StateFlow<UiState> = repository.getDataStream()
         .map { UiState.Success(it) }
         .stateIn(
             scope = viewModelScope,
             started = SharingStarted.WhileSubscribed(5_000),
             initialValue = UiState.Loading
         )
     ```
   - **Why 5,000ms?** The 5-second buffer keeps upstream collection active across Android configuration changes (such as screen rotations or theme switches) where the Composable temporarily unsubscribes, while correctly stopping collection when the screen is navigated away or backgrounded.
   - Avoid `SharingStarted.Eagerly` for screen-bound flows as it keeps upstream collection running indefinitely even when the UI is absent.

8. **Collection Encapsulation & Defensive Copies**:
   - Do not expose an internal `MutableList` through a `List<T>` reference without defensive copying. Callers or background operations can mutate the underlying list.
   - Use defensive snapshots (`get() = _items.toList()`) or persistent immutable collections (`kotlinx.collections.immutable.PersistentList` / `persistentListOf()`).

9. **Concurrency & Race Condition Handling**:
   - **Superseding Operations (e.g. Refresh)**: Cancel existing in-flight `Job` prior to launching a newer fetch to prevent older slow responses from overwriting newer fast responses (`refreshJob?.cancel(); refreshJob = viewModelScope.launch { ... }`).
   - **Query Streams**: Use `flatMapLatest` so new emissions cancel the collection of previous query flows.
   - **State / Financial Mutations**: Use `kotlinx.coroutines.sync.Mutex` with `mutex.withLock { ... }` to serialize access to critical sections.

10. **Null Safety & Assertion Prohibitions**:
    - **Never use `!!` (double bang)** in production Android code. Use safe calls (`?.`), the Elvis operator (`?:`), `let`, or explicit preconditions (`requireNotNull(value) { "Descriptive domain reason" }`).

