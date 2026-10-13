# Flutter Code Generation & Annotation Rules

@../skills/flutter-code-generation-annotations/SKILL.md

## Code Generation & Modeling Governance

1. **Immutability by Default**:
   - Every domain entity, UI state class, and event class must be immutable.
   - Use `@freezed` to generate value equality, `copyWith`, and pattern matching constructors.
   - Never use mutable fields (`var`) inside state or entity classes.

2. **Resilient JSON Serialization**:
   - DTOs representing API payloads must use `@JsonSerializable(explicitToJson: true)`.
   - All remote DTO fields must be nullable (`String?`, `int?`, `double?`) to prevent parsing crashes when backend schemas change.
   - Run `dart run build_runner build --delete-conflicting-outputs` after altering models.

3. **Dependency Injection**:
   - Register dependencies using `@injectable`, `@singleton`, or `@lazySingleton`.
   - Prefer constructor injection over manual service locator calls.
