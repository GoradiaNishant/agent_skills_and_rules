# Flutter Architecture Decision Guide

## 1. Clean Architecture Layers

Structure Flutter applications using clean layer separation (Feature-first or Layer-first):

```text
lib/
├── core/
│   ├── di/                 # Dependency injection (get_it / injectable)
│   ├── network/            # Dio client, interceptors, NetworkResult
│   ├── session/            # SessionManager, TokenProvider
│   ├── monitoring/         # AppLogger, CrashReporter, AnalyticsTracker
│   └── errors/             # GlobalErrorHandler, failure definitions
│
├── shared/
│   ├── data/
│   │   └── models/         # Cross-feature DTO models (user_model.dart, address_model.dart, pagination_model.dart)
│   ├── domain/
│   │   └── entities/       # Cross-feature domain entities (user.dart, address.dart, paginated_result.dart)
│   ├── widgets/            # Reusable design system widgets (buttons, text fields, loaders, dialogs)
│   └── utils/              # General formatters, extensions, date/time helpers
│
└── features/
    ├── auth/
    │   ├── data/           # Remote data sources, auth DTOs, repository implementations
    │   ├── domain/         # Auth repository interfaces, auth entities, use cases
    │   └── presentation/   # Auth screens, widgets, BLoCs / Cubits / Notifiers
    │
    ├── profile/
    │   ├── data/
    │   ├── domain/
    │   └── presentation/
    │
    ├── customers/
    │   ├── data/
    │   ├── domain/
    │   └── presentation/
    │
    └── orders/
        ├── data/
        ├── domain/
        └── presentation/
```

## 2. State Management Selection Framework

Select the appropriate state management paradigm based on architectural scope:

| Mechanism | Scope | Learning Curve | Best For | Trade-offs |
|---|---|---|---|---|
| **`setState`** | Single widget | Minimal | Local UI toggles (checkbox, password visibility, expansion tile). | Does not scale across widgets; couples logic with UI. |
| **`ValueNotifier` / `ChangeNotifier`** | Subtree / Screen | Low | Small, focused observable variables (e.g. cart badge counter). | Lacks structured event dispatching; easy to trigger broad rebuilds. |
| **`Provider`** | Feature / App | Moderate | Scoped dependency injection and shared state across routes. | Service-locator feel; context-dependent lookups. |
| **`Riverpod`** | Feature / App | Moderate | Compile-safe reactive caching, auto-disposal, no `BuildContext` requirement. | Requires Riverpod-specific `WidgetRef` and provider definitions. |
| **`BLoC` / `Cubit`** | Enterprise / App | High | Strict unidirectional data flow, explicit state machines, audit trails, complex business logic. | Requires more boilerplate (Events, States, Freezed unions). |

- **Golden Rule**: Always use **Freezed** union classes for states (`initial`, `loading`, `success`, `error`) to enforce exhaustive compile-time pattern matching with `when`/`map`.

---

## 3. Dart Language Fundamentals & Senior-Level Gotchas

### 1. `var` vs `final` vs `const`
- **`var`**: Declares a variable with type inference. The variable **can be reassigned** to another value of the same inferred type:
  ```dart
  var name = 'Flutter';
  name = 'Dart'; // Valid
  ```
- **`final`**: A single-assignment variable whose value is determined **at runtime**. It cannot be reassigned:
  ```dart
  final timestamp = DateTime.now(); // Evaluated at runtime
  ```
  > **Crucial Gotcha (Shallow Immutability)**: `final` only restricts variable reassignment; it does NOT make the referenced object immutable!
  ```dart
  final items = [1, 2, 3];
  items.add(4); // Valid Dart! The list contents are mutated.
  // items = [5, 6]; // Error: A final variable can only be set once.
  ```
- **`const`**: A **compile-time constant**. The value must be known at compile time and is deeply, genuinely immutable:
  ```dart
  const pi = 3.14159;
  const numbers = [1, 2, 3];
  // numbers.add(4); // Throws UnsupportedError (cannot modify an unmodifiable list)
  ```
- **Why `const` Widgets Matter in Flutter**:
  - `const` widget constructors create canonical instances in memory that Flutter can reuse across rebuilds.
  - When Flutter evaluates the widget tree during a parent rebuild, encountering an identical `const` widget instance allows Flutter to **short-circuit** the rebuild of that entire subtree.

### 2. Dart `late` vs. Kotlin `lateinit`
- **Dart `late`**:
  - Defers initialization of a non-nullable variable until first access (lazy) or allows assigning a non-nullable variable after declaration:
    ```dart
    late final TextEditingController controller;
    ```
  - **The Big Gotcha**: Dart has **NO built-in reflection property** like Kotlin's `::property.isInitialized`. If you access a `late` variable before initializing it, Dart throws `LateInitializationError`.
  - **Alternative**: If you must check whether a value is initialized, use a nullable variable (`String? value;`) or an explicit boolean flag (`bool _isInitialized = false;`).
  - Dart `late` works on **all types**, including primitive types (`int`, `double`, `bool`) and function types.
- **Kotlin `lateinit`**:
  - Supports `::property.isInitialized` reflection check.
  - Cannot be used on primitive types (`Int`, `Boolean`, `Float`) or nullable types. Throws `UninitializedPropertyAccessException` if uninitialized.

### 3. Functional Collection Operations
- **`map()` vs `forEach()`**:
  - `map()` lazily transforms each element of an `Iterable` into a new iterable. Use `map()` when converting data models (e.g. `items.map((e) => e.toDomain()).toList()`).
  - `forEach()` applies a side-effect function to each element and returns `void`. Never use `forEach()` to construct a new collection.
- **`where()` vs `firstWhere()`**:
  - `where()` filters an iterable, returning all matching elements.
  - `firstWhere((e) => ..., orElse: () => null)` returns the first matching element. If no element satisfies the predicate and `orElse` is omitted, it throws `StateError (No element)`.

---

## 4. State Survival & Persistence Matrix in Flutter

| Storage Layer | Survives Widget Rebuild? | Survives Navigation (Push/Pop)? | Survives App Suspension? | Survives App Terminate / Reboot? |
|---|---|---|---|---|
| **Local Widget State (`State.widget`)** | ✅ Yes | ❌ No (destroyed on pop) | ⚠️ System-dependent | ❌ No |
| **`AutomaticKeepAliveClientMixin`** | ✅ Yes | ❌ No (keeps alive in `PageView`/`TabBarView`) | ❌ No | ❌ No |
| **`PageStorageKey`** | ✅ Yes | ✅ Yes (retains scroll position) | ❌ No | ❌ No |
| **`HydratedBloc` / Cached Riverpod** | ✅ Yes | ✅ Yes | ✅ Yes (saved to local disk) | ✅ Yes |
| **`flutter_secure_storage`** | ✅ Yes | ✅ Yes | ✅ Yes (Hardware Keystore/Keychain) | ✅ Yes |

