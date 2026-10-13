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

## 2. State Management Selection

- **BLoC / Cubit**: Recommended for complex event-driven business flows, strict audit trails, and multi-step workflows.
- **Riverpod**: Recommended for lightweight reactive dependency injection, declarative state caching, and auto-dispose patterns.
- Always use **Freezed** union classes for states (`initial`, `loading`, `success`, `error`) to enforce compile-time pattern matching.
