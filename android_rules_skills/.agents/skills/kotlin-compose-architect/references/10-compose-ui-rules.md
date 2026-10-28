# Reference 10 — Jetpack Compose UI Development Rules

---

## Primary Principle

> The feature description defines the required behavior and user experience.
> Android and Jetpack Compose best practices define the implementation.

Do not design the UI architecture first and then force the feature into it.

First understand:
- What the feature does and who uses it
- What information must be displayed; what is primary vs. secondary
- What actions the user can perform
- What data changes over time and at what frequency
- Whether the feature communicates with a backend, hardware, or sensors
- Whether data is real-time
- Whether the feature works offline
- Whether permissions are involved
- Whether the screen can exist in loading, empty, error, disconnected, reconnecting, or partial states
- Whether the UI must support phones, tablets, foldables, landscape, portrait, split-screen, and resizable windows

Then design the Compose UI around those requirements.

---

## Step 0 — Inspect Before Implementing

Before creating or modifying any Composable:

1. Inspect the existing project, theme, and Material 3 usage
2. Inspect existing reusable components, typography, colors, dimensions, shapes, and icons
3. Inspect navigation patterns, ViewModels, and UI state conventions
4. Inspect existing loading/error/empty-state components
5. Inspect existing responsive/adaptive implementations

**Reuse existing components when appropriate.**
Do not introduce a second design system or duplicate existing components.
Do not rewrite a working screen merely to prefer a different architecture.
Prefer incremental improvements.

---

## Pre-Implementation Process

Before writing any code for a new screen:

```
1.  Read feature description
2.  Inspect existing project
3.  Identify screen purpose and primary user action
4.  Identify data sources (API / DB / hardware / sensors / cache)
5.  Identify all meaningful UI states
6.  Identify real-time behavior and update frequency
7.  Identify lifecycle concerns (background, rotation, process death)
8.  Identify required permissions
9.  Identify error / empty / loading states
10. Identify adaptive requirements (compact / medium / expanded)
11. Identify accessibility requirements
12. Identify performance-sensitive areas
13. Determine UI state model
14. Determine composable hierarchy
15. Implement
16. Verify build
17. Verify behavior (real device where hardware is involved)
```

Do not skip directly from feature description to `@Composable fun Screen()`.

---

## UI State Must Be Explicit

Prefer a single observable UI state model per meaningful screen.

```kotlin
data class DeviceUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val deviceName: String? = null,
    val batteryLevel: Int? = null,
    val measurement: Measurement? = null,
    val isSyncing: Boolean = false,
    val error: UiError? = null
)
```

Expose immutable state from ViewModel:
```kotlin
val uiState: StateFlow<DeviceUiState>
```

Expose events separately:
```kotlin
sealed interface DeviceEvent {
    data object Connect : DeviceEvent
    data object Disconnect : DeviceEvent
    data object Retry : DeviceEvent
}
```

The Composable renders state and emits events:
```kotlin
DeviceScreen(state = uiState, onEvent = viewModel::onEvent)
```

---

## All Meaningful UI States

Do not assume a screen only has `Loading → Success`.

A screen may need some or all of these:

```
Initial · Loading · Success · Empty · Error · Refreshing · Offline
Connecting · Connected · Disconnected · Reconnecting
PermissionRequired · Unauthorized · DeviceUnavailable
Unsupported · PartialData · Syncing · SyncFailed
```

Evaluate which states apply to the actual feature.

---

## Unidirectional Data Flow

```
Data source → Repository / domain → ViewModel → UiState → Composable
                                                              ↓
                                                         User event
                                                              ↓
                                                         ViewModel
```

The UI must NOT directly:
- Call Retrofit / Room
- Communicate with BLE / USB
- Start device synchronization
- Execute business rules
- Access authentication repositories
- Manipulate hardware SDKs

---

## Separate Route From Screen

For meaningful screens, prefer two layers:

```kotlin
// Handles: ViewModel, lifecycle-aware collection, navigation, side effects
@Composable
fun DeviceRoute(viewModel: DeviceViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DeviceScreen(state = state, onEvent = viewModel::onEvent)
}

// Handles: rendering, composition, user interaction callbacks
@Composable
fun DeviceScreen(state: DeviceUiState, onEvent: (DeviceEvent) -> Unit) { ... }
```

For genuinely tiny screens, do not create unnecessary layers.

---

## Keep Composables Focused

A Composable should answer: *"Given this state, what should the UI look like?"*

Prefer state that already describes the UI condition:
```kotlin
state.showStartButton
state.showReconnectButton
state.isLowBattery
```

Rather than embedding business rules:
```kotlin
if (device.isConnected && battery > 20 && ...) // ← belongs in ViewModel/domain
```

Use judgment — do not move every trivial UI calculation into a UseCase.

---

## Do Not Over-Componentize

Create reusable Composables when they represent a meaningful UI concept,
repeated pattern, or logically independent section.

Do NOT create wrappers for standard Compose APIs:
```kotlin
// Bad — no added value
@Composable fun MyText(text: String) { Text(text) }
```

Good:
```kotlin
@Composable fun DeviceConnectionCard(...) // meaningful concept
```

---

## Material 3

Use the project's Material 3 system:
```kotlin
MaterialTheme.colorScheme
MaterialTheme.typography
MaterialTheme.shapes
```

Do not scatter `Color(...)` or `fontSize = 14.sp` throughout the codebase.
Do not create a custom design system unless the application actually requires one.

---

## Layout Dimensions

Avoid:
```kotlin
width = 320.dp
height = 600.dp
```

Prefer adaptive modifiers:
```kotlin
fillMaxWidth() · weight() · wrapContentHeight() · padding() · widthIn() · heightIn()
```

Use explicit dimensions only for intentional design constraints.

---

## Adaptive UI Is Required

Design for the available application window, not a single phone size.

Account for: Compact / Medium / Expanded widths · Tablets · Foldables ·
Landscape · Portrait · Multi-window · Resizable windows

Use **Window Size Classes** — not device-name checks:
```kotlin
// Wrong
if (isTablet) { ... }

// Correct
if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) { ... }
```

Use reflow, additional columns, list/detail layouts, navigation rail/drawer,
supporting panes, and wider content constraints — not just scaling.

---

## Lists Must Be Lazy

For potentially large collections, always use:
```kotlin
LazyColumn · LazyRow · LazyVerticalGrid
```

Never:
```kotlin
Column { items.forEach { ... } }   // ← for large/unbounded datasets
```

Always provide stable keys:
```kotlin
items(items = devices, key = { it.id }) { device -> DeviceItem(device) }
```

---

## Compose Performance Rules

1. Implement correctly first.
2. Measure if performance is an issue.
3. Identify the specific expensive operation.
4. Fix and measure again.
5. Keep the optimization only if it improves the measured result.

**Never optimize based purely on intuition.**

### Avoid expensive work during composition
```kotlin
// Bad — runs on every recomposition
val sorted = items.sortedBy { expensiveCalculation(it) }
```
Prefer ViewModel preprocessing, `remember`, or `derivedStateOf`.

### Use `remember` correctly
For UI-local calculations/objects that should survive recomposition.
Do not use `remember { mutableStateOf(...) }` for state that belongs in ViewModel.

### Use `derivedStateOf` only when appropriate
```kotlin
val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
```
Reduces recompositions when source state changes frequently. Not a blanket rule.

### Never perform backward writes
Never mutate state after reading it during composition.

---

## Side Effects

Use effect APIs with clear lifecycle and key:
```kotlin
LaunchedEffect · DisposableEffect · SideEffect · produceState · rememberCoroutineScope
```

```kotlin
LaunchedEffect(deviceId) { viewModel.loadDevice(deviceId) }
```

Never call `repository.load()`, `database.insert()`, `ble.connect()`, or
`navigation.navigate()` during normal composition.

---

## Navigation Must Be Event-Driven

Do not pass `NavController` through UI components.
Prefer callbacks:
```kotlin
onDeviceSelected: (DeviceId) -> Unit   // ✅
deviceCard(navController)              // ❌
```

---

## Real-Time Data — Special Design Required

For frequently updating values (BLE, sensors, WebSocket, telemetry, GPS, IoT):

```
Raw stream → Repository/device manager → Processing/throttling/aggregation → ViewModel → UI state → Focused UI updates
```

The UI displays information at the rate meaningful to the user, not the maximum hardware rate.

Collect state using lifecycle-aware APIs:
```kotlin
collectAsStateWithLifecycle()
```

### No hardware calls from Composables — ever
```kotlin
// All wrong:
@Composable fun Screen() { bleManager.connect() }
@Composable fun Screen() { usbDevice.readData() }
@Composable fun Screen() { sensor.start() }
```
Composition may run repeatedly. Hardware operations live outside composition.

---

## Device Connection State — Explicit Model

Do not model connection as a single Boolean.

```kotlin
sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data object Connected : ConnectionState
    data object Reconnecting : ConnectionState
    data class Failed(val reason: String) : ConnectionState
}
```

Display meaningful state — do not hide failures behind generic spinners.

Connection recovery belongs in the Repository / DeviceManager / ViewModel — not in Composables.

---

## Rapid State Changes

For frequently changing values:
- Avoid unnecessary animations on every raw event
- Avoid rebuilding large UI trees
- Keep rapidly changing state close to the UI that consumes it
- Separate stable UI from dynamic UI
- Avoid expensive formatting during every recomposition
- Consider upstream throttling/aggregation where appropriate
- Display only the precision users actually need

For lambda modifier optimization:
```kotlin
Modifier.offset { IntOffset(x.roundToInt(), 0) }
```
Only when it provides a measurable benefit — not as a blanket rule.

---

## Error Messages Must Be User-Oriented

Do not display raw exceptions:
```
java.net.SocketTimeoutException...   ❌
GATT status 133                       ❌
```

Map technical failures:
```
Unable to connect to the device.
Please make sure the device is nearby and powered on.   ✅
```

The technical error can still be logged for debugging.

---

## Device UI States

For hardware/device screens, handle in addition to standard states:

```
Permission required · Bluetooth disabled · Device unavailable
Connecting · Connected · Reconnecting · Unsupported device
Firmware mismatch · Syncing · Sync failed
```

Show data freshness where it matters:
```
Heart rate: 72 BPM
Updated 3 seconds ago      ← stale data should be visible
```

Do not pretend stale data is live.

---

## Permissions Are UI State

Design the UI for:
```
Permission not requested → Permission requested → Granted / Denied / Permanently denied → Feature unavailable
```

Request permissions when the user reaches the feature, not blindly on app startup.

---

## Partial Loading — Avoid Full-Screen Blocking

For partial operations:
```
Sync button → syncing indicator   ✅
Entire screen → loading overlay   ❌ (when only one action is executing)
```

---

## Accessibility Is Mandatory

Provide:
```kotlin
contentDescription · semantics · role · stateDescription
```

Ensure interactive targets are sufficiently large.
Do not add meaningless descriptions to decorative elements.
Consider TalkBack, Switch Access, large font sizes, and keyboard navigation during implementation.

---

## Localizable Strings

```kotlin
stringResource(R.string.device_connected)   // ✅
Text("Device Connected")                    // ❌
```

Do not concatenate user-facing strings — sentence structure changes with locale.

---

## Avoid Unnecessary Layout Tricks

Solve layout problems with proper Compose APIs:
```kotlin
Row · Column · Box · FlowRow · LazyLayout · weight · Arrangement · Alignment
```

Not with:
```kotlin
Spacer · offset · absoluteOffset · negative padding   // ← only when intentional
```

Use ConstraintLayout only when the design genuinely benefits from it.

---

## Pass Minimal State to Child Composables

```kotlin
// ✅
DeviceStatusCard(connectionState = state.connectionState, batteryLevel = state.batteryLevel, onReconnect = onReconnect)

// ❌
DeviceStatusCard(viewModel = viewModel)
```

Prefer immutable data models for UI state. Prefer `List<T>` over mutable collections.

---

## UI Architecture Scales With Complexity

### Simple feature
```
Route → Screen
```

### Moderate feature
```
Route → Screen → Header + Content + Actions
```

### Complex feature
```
Route → Screen → Header + ConnectionState + DataSection + RealTimeSection + Controls + Error/Status
```

Do not create a 20-file hierarchy for a two-component screen.
Do not force every screen into TopAppBar + LazyColumn + FAB when the feature does not need it.

---

## Testing

Tests remain **strictly opt-in**.
Do not generate Compose UI tests, screenshot tests, or test fixtures unless explicitly requested.
Structure production code so tests can be added later without major refactoring.

---

## Real Device Verification

For features involving BLE, USB, NFC, sensors, camera, microphone, location,
audio, or background processing — emulator verification is not sufficient.

Verify: permission flow · connection/disconnection · background/foreground ·
rotation · configuration changes · failure/recovery · hardware behavior.

---

## Final Quality Checklist

### Requirements
- [ ] Feature behavior matches the description
- [ ] All required states are represented
- [ ] Primary user action is obvious

### Architecture
- [ ] UI does not contain business logic
- [ ] Events flow upward; state flows downward
- [ ] No hardware calls from Composables
- [ ] ViewModel used when meaningful state exists

### Compose
- [ ] No side effects during composition
- [ ] No backward writes
- [ ] No unnecessary recomposition
- [ ] Lazy lists use stable keys
- [ ] Expensive calculations handled appropriately

### Adaptive UI
- [ ] Works with available window size
- [ ] Handles compact / medium / expanded layouts
- [ ] Supports rotation / resizing where applicable
- [ ] No hardcoded phone-only dimensions

### Real-Time / Hardware
- [ ] Connection state is explicit (not just a Boolean)
- [ ] Reconnection handled outside UI
- [ ] Rapid updates do not redraw entire screen unnecessarily
- [ ] Stale / offline data represented appropriately
- [ ] Real-device behavior considered

### UX
- [ ] Loading / empty / error / retry states exist
- [ ] User-facing errors are understandable
- [ ] Accessibility considered
- [ ] Strings are localizable

### Performance
- [ ] No expensive work in composition
- [ ] Images handled appropriately
- [ ] Performance optimizations are evidence-based

### Verification
- [ ] Project builds successfully
- [ ] Hardware features verified on real device where required
- [ ] Tests added only if explicitly requested

---

## Compose Rendering Pipeline, Stability & Recomposition Optimization

### 1. The Three Phases of Compose
Every frame rendered by Compose transitions through three distinct phases:
1. **Composition (What to show)**: Runs Composable functions to construct or update the Composable tree description.
2. **Layout (Where to place it)**: Measures children and places them within 2D constraints.
3. **Drawing (How to render it)**: Draws pixels to the canvas/screen.

> **Key Rule**: Avoid triggering Phase 1 (Composition) if the change only affects Phase 2 (Layout) or Phase 3 (Drawing). Use lambda-based modifiers (`Modifier.offset { ... }`, `Modifier.graphicsLayer { ... }`) to defer reading state until the layout or draw phase, skipping recomposition entirely during high-frequency animations or scrolling.

### 2. Difference Between Recomposition, Relayout, and Repaint
- **Recomposition**: Re-executing Composable functions when snapshot state changes. If a Composable's parameters have not changed and are **stable**, Compose skips recomposition.
- **Relayout**: Measuring and positioning elements. A recomposition might not cause a relayout if dimensions don't change.
- **Repaint / Redraw**: Drawing pixels onto the display. Changing an alpha or offset in `graphicsLayer` only repaints without recomposition or relayout.

### 3. Stability & Unstable Parameter Mitigation
Compose determines whether a Composable can be skipped based on parameter stability:
- **Stable Types**: Primitives (`Int`, `String`, etc.), functional types (`() -> Unit`), types annotated with `@Immutable` or `@Stable`.
- **Unstable Types**: Standard collection interfaces (`List<T>`, `Set<T>`, `Map<T>`) because the Kotlin runtime does not guarantee immutability (an underlying mutable list could be passed).
- **Techniques to Prevent Unnecessary Recompositions**:
  1. Use **`kotlinx.collections.immutable.ImmutableList<T>`** or **`PersistentList<T>`** in `UiState`.
  2. Annotate UI models with `@Immutable` or `@Stable` when all public properties are immutable `val`s.
  3. Avoid instantiating new lambda instances inline inside list items; use method references or hoist callbacks.

### 4. Stable Keys in Lazy Layouts
- Always provide an explicit, unique, and stable key for `LazyColumn`, `LazyRow`, and `LazyVerticalGrid` items:
  ```kotlin
  items(items = state.items, key = { item -> item.id }) { item ->
      ItemRow(item = item)
  }
  ```
- Without stable keys, adding, removing, or reordering an item forces Compose to recompose every single visible row because items are matched by position index rather than semantic identity.

---

## Golden Rule

> Write the **simplest correct Compose implementation** that remains reliable under
> the actual feature requirements, device behavior, lifecycle, window sizes,
> data frequency, and Android platform constraints.

Before implementing any Compose view, always consider:

```
Feature + State + Data + Lifecycle + Device + Network + Hardware
    + Window Size + Accessibility + Performance + Security
```

Never add architecture, abstractions, optimizations, dependencies, or complexity
unless the feature actually benefits from them.
