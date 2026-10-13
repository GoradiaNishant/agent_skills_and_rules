# Flutter API & Data Layer Patterns

1. **Dio Client Configuration**:
   - Centralize Dio initialization with connection timeouts (15s), receive timeouts (15s).
   - Use `QueuedInterceptor` for handling HTTP 401 challenges and coordinating token refresh.
2. **Safe DTO Serialization**:
   - Model all remote fields as nullable (`String?`, `int?`) to handle schema evolution.
   - Separate remote DTOs from domain entities with explicit mapper functions (`dto.toDomain()`).
3. **Repository Error Boundary**:
   - Repositories must catch `DioException`, `FormatException`, and map to typed `NetworkResult<T>` with `UiText`.
   - Never allow uncaught network exceptions to crash UI widgets.
