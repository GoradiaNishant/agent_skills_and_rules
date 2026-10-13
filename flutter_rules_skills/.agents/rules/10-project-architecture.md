# Project Architecture Rules

@../skills/flutter-architect/references/03-architecture-decisions.md

## Layer Separation & Governance

1. **Three-Tier Project Structure**:
   - `lib/core/`: Application-wide infrastructural components: dependency injection (`di/`), Dio client & interceptors (`network/`), session management & secure tokens (`session/`), centralized telemetry (`monitoring/`), and global error handling (`errors/`).
   - `lib/shared/`: Cross-feature assets shared across multiple domains:
     - `data/models/`: Shared DTOs (`user_model.dart`, `address_model.dart`, `pagination_model.dart`).
     - `domain/entities/`: Shared domain entities (`user.dart`, `address.dart`, `paginated_result.dart`).
     - `widgets/`: Reusable design system UI components (buttons, text fields, loaders, dialogs).
     - `utils/`: Common formatters, extensions, and date/time helpers.
   - `lib/features/`: Feature-first modular packages (`auth/`, `profile/`, `customers/`, `orders/`). Each feature encapsulates its own `data/`, `domain/`, and `presentation/` layers.

2. **Clean Architecture Layer Hierarchy**:
   - Presentation (Widgets, BLoCs/Cubits/Notifiers) -> Domain (Entities, Repository Interfaces, Use Cases) <- Data (DataSources, DTOs, Repository Implementations).
   - The Domain layer must remain pure Dart with zero dependencies on Flutter UI (`package:flutter/widgets.dart`) or Dio.

3. **DTO & Entity Separation**:
   - Never expose raw remote DTOs (`UserDto` / `UserModel`) directly to presentation widgets. Map to clean domain entities (`User`) within the repository layer.

4. **Dependency Inversion Rule**:
   - Feature dependencies must point inward toward domain abstractions.
   - Use `get_it` and `injectable` to resolve implementations at runtime.

