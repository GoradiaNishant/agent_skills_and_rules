# Reference 15 — Memory Leaks, Resource Cleanup & Performance Profiling Guide

## 1. Memory Leaks in Flutter: Anatomy & Common Causes

A memory leak occurs when Dart objects that are no longer needed remain reachable by the Garbage Collector (GC) because an active reference or closure retains them.

### Top Causes of Flutter Memory Leaks:
1. **Undisposed Controllers**: `TextEditingController`, `AnimationController`, `ScrollController`, and `PageController` hold internal listeners and window bindings.
2. **Orphaned Stream Subscriptions**: Creating a `.listen()` without cancelling it in `State.dispose()`.
3. **Active Timers**: `Timer.periodic()` that continues firing after the widget is popped off the navigation stack.
4. **Global Singletons Holding Listeners**: Registering callbacks on long-lived services (`ChangeNotifier`, `EventBus`) without unregistering upon widget disposal.
5. **Closures Capturing `BuildContext` or `State`**: Retaining references inside static or asynchronous closures.

---

## 2. Mandatory Resource Disposal Pattern

Every `StatefulWidget` allocating disposable resources **must** implement `dispose()`:

```dart
class SearchScreenState extends State<SearchScreen> with SingleTickerProviderStateMixin {
  late final TextEditingController _searchController;
  late final ScrollController _scrollController;
  late final AnimationController _animController;
  StreamSubscription<SearchEvent>? _eventSubscription;
  Timer? _debounceTimer;

  @override
  void initState() {
    super.initState();
    _searchController = TextEditingController();
    _scrollController = ScrollController();
    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 300),
    );
    _eventSubscription = eventBus.stream.listen(_handleEvent);
  }

  @override
  void dispose() {
    // 1. Cancel timers and active network debounce loops
    _debounceTimer?.cancel();

    // 2. Cancel stream subscriptions
    _eventSubscription?.cancel();

    // 3. Dispose UI controllers
    _searchController.dispose();
    _scrollController.dispose();
    _animController.dispose();

    // 4. Always invoke super.dispose() last
    super.dispose();
  }

  void _handleEvent(SearchEvent event) {
    if (!mounted) return;
    // Process event
  }
}
```

---

## 3. Asynchronous Gaps & The `mounted` Guard

When executing an asynchronous operation (`await`) inside a `StatefulWidget`, the widget might be unmounted and disposed before the future completes (e.g. user pressed Back while an API request was in flight).

> **Rule**: Never call `setState()`, read `context`, or invoke callbacks after an `await` without verifying `if (!mounted) return;`.

```dart
// ❌ WRONG: Can throw "setState() called after dispose()"
Future<void> _fetchUserProfile() async {
  final profile = await repository.getProfile();
  setState(() {
    _profile = profile;
  });
}

// ✅ CORRECT: Protected against unmounted state
Future<void> _fetchUserProfile() async {
  final profile = await repository.getProfile();
  if (!mounted) return; // Guard against execution after dispose()
  setState(() {
    _profile = profile;
  });
}
```

---

## 4. Flutter Performance Profiling & DevTools

### 1. Mandatory Profiling Mode
- **Never benchmark in Debug Mode**: Debug mode compiles JIT (Just-In-Time) with heavy debugging asserts, type checks, and observatory instrumentation. Frame rate in debug mode is completely unrepresentative of production.
- **Always benchmark in Profile Mode on a physical device**:
  ```bash
  flutter run --profile
  ```

### 2. DevTools Performance View: UI vs. Raster Thread
When diagnosing jank (dropped frames exceeding 16.6ms on 60Hz or 8.3ms on 120Hz), inspect the two primary threads:

- **UI Thread (Dart VM)**:
  - Executes Dart code, runs the event loop, evaluates `build()` methods, and computes layout constraints.
  - *If the UI bar is red*: You are doing heavy computation on the main isolate (e.g. JSON decoding, sorting, filtering) or rebuilding too many widgets. Move CPU tasks to an `Isolate.run()` and extract `const` widgets.
- **Raster Thread (formerly GPU Thread)**:
  - Takes layer trees from the UI thread and renders pixels via the graphics API (Impeller / Skia).
  - *If the Raster bar is red*: You are using expensive graphics operations (excessive `BackdropFilter` blurs, heavy clipping `Clip.antiAliasWithSaveLayer`, un-cached complex custom shaders, or rendering giant unscaled images).

### 3. DevTools Memory Profiler
- Take a **Heap Snapshot** before and after navigating through a feature flow (e.g. open feature $\rightarrow$ pop feature).
- If the retained instance count of your feature's `State`, `Controller`, or `Bloc` increases and never drops after a Garbage Collection pass, you have a memory leak!
- Filter by class name and inspect the **Retaining Path** to locate the unreleased reference.
