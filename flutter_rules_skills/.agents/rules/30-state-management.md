# State Management Rules

@../skills/flutter-architect/references/03-architecture-decisions.md

## Unidirectional Data Flow & State Governance

1. **State Immutability**:
   - UI states must be modeled as immutable Freezed union classes: `Initial`, `Loading`, `Success`, `Error`.
   - Never mutate state properties directly.
2. **Business Logic Isolation**:
   - Keep business logic inside BLoCs, Cubits, or Riverpod Notifiers.
   - Widgets must only emit events / trigger methods and observe state changes (`BlocBuilder`, `BlocConsumer`, or `ConsumerWidget`).
3. **Comprehensive UI States**:
   - Every feature must gracefully handle: Initial state, Loading indicator, Populated content, Empty list placeholder, and Actionable Error view with retry callback.
