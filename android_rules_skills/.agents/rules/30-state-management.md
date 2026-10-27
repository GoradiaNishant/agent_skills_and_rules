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

5. **Search Debouncing**:
   - Use Flow operators (`debounce(400L)`) with `distinctUntilChanged()` and `flatMapLatest()` for search inputs to prevent redundant API queries.
   - Ensure the debounce delay is set to 400ms (`AppConstants.DEBOUNCE_SEARCH_MS`).

6. **Coroutine Scope & Cancellation**:
   - Always launch coroutines within `viewModelScope`.
   - Never use `GlobalScope`.
   - Cancel prior jobs (e.g. search queries) when new conflicting queries are initiated.
