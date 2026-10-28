---
name: 30-state-management
description: "BLoC, Cubit, and reactive state management"
trigger: always_on
alwaysApply: true
globs:
  - "**/bloc/**"
  - "**/cubit/**"
  - "**/*state.dart"
---

# State Management Rules

@../skills/flutter-architect/references/03-architecture-decisions.md

## Unidirectional Data Flow & State Governance

1. **State Management Selection Matrix**:
   Select the appropriate state management mechanism based on the problem scope:
   - **`setState`**: Strictly for ephemeral, local, single-widget UI state (e.g. toggling password visibility, local tab switching, checkbox toggles). Never elevate to a global state manager for purely local UI behaviors.
   - **`ValueNotifier` / `ChangeNotifier`**: Lightweight observable state for isolated, simple features without full framework overhead.
   - **`Provider` / `Riverpod`**: Shared reactive state, dependency injection, declarative state caching, and auto-disposal (`ref.watch`, `autoDispose`).
   - **`BLoC` / `Cubit`**: Explicit state machines, event-driven architectures, strict audit trails, and complex multi-step workflows (e.g. authentication, payments, order checkout).

2. **State Immutability**:
   - UI states must be modeled as immutable Freezed union classes: `Initial`, `Loading`, `Success`, `Error`.
   - Never mutate state properties directly.

3. **Business Logic Isolation**:
   - Keep business logic inside BLoCs, Cubits, or Riverpod Notifiers.
   - Widgets must only emit events / trigger methods and observe state changes (`BlocBuilder`, `BlocConsumer`, or `ConsumerWidget`).

4. **Resource Disposal & Memory Leak Prevention**:
   - Every disposable controller, stream subscription, or animation listener created in a `StatefulWidget` MUST be released in `dispose()`:
     ```dart
     @override
     void dispose() {
       _textController.dispose();
       _scrollController.dispose();
       _animationController.dispose();
       _streamSubscription?.cancel();
       _timer?.cancel();
       super.dispose();
     }
     ```
   - Failing to dispose controllers or cancel streams keeps closures alive in memory, causing memory leaks.

5. **Async Gap Safety (`mounted` Checks)**:
   - Always check `if (!mounted) return;` after any asynchronous operation (`await`) before calling `setState()`, reading `BuildContext`, or displaying dialogs/SnackBars:
     ```dart
     Future<void> _submitForm() async {
       final result = await authService.login();
       if (!mounted) return; // Prevent setState() after widget disposed
       setState(() { _status = result; });
     }
     ```

6. **Comprehensive UI States**:
   - Every feature must gracefully handle: Initial state, Loading indicator, Populated content, Empty list placeholder, and Actionable Error view with retry callback.

