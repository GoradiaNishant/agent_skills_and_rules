---
name: 80-testing-validation
description: "Unit tests, widget tests, and integration tests"
trigger: always_on
alwaysApply: true
globs:
  - "test/**"
---

# Testing and Quality Validation Rules

@../skills/flutter-architect/references/03-architecture-decisions.md

## Testing Strategy & Validation Governance

1. **Unit Testing Strategy**:
   - Write unit tests for BLoCs/Cubits/Notifiers using `bloc_test` or standard stream matchers.
   - Test repository mapping logic, exception conversion to `NetworkResult`, and validator functions.
   - Use `mocktail` for null-safe mocking without code generation.
2. **Widget Testing**:
   - Write widget tests (`testWidgets`) for critical user interactions, form submissions, and loading/error state displays.
   - Ensure widgets are pumped inside test harnesses providing `MaterialApp` and required dependencies (`MultiBlocProvider` or Riverpod `ProviderScope`).
3. **Empirical Verification Requirement**:
   - Never claim tests passed without executing `flutter test` via the terminal.
   - Fix all warnings identified by `flutter analyze` prior to committing.
