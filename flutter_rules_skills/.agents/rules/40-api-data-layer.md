# API and Data Layer Rules

@../skills/flutter-network-session-management/SKILL.md
@../skills/flutter-architect/references/07-security.md
@../skills/flutter-architect/references/13-api-data-patterns.md
@../skills/ui-first-mock-api/SKILL.md

## API & Response Pattern Conventions

1. **Dio Client Configuration**:
   - Centralize Dio client initialization with connection timeouts (15s) and receive timeouts (15s).
   - Sensitive base URLs and API keys must come from `--dart-define` or environment files—never hardcoded.

2. **Safe Serialization & Data Transfer Objects (DTOs)**:
   - DTOs must use `@JsonSerializable(explicitToJson: true)`.
   - Every field in remote DTOs must be nullable (`String?`, `int?`, `double?`) to prevent parsing crashes when backend contracts evolve.
   - Separate remote DTOs (`UserDto`) from domain entities (`User`) via explicit extension mappers.

3. **Result Wrapper & Repository Methods**:
   - Repositories must catch `DioException`, `FormatException`, `TypeError` and return a typed `NetworkResult<T>`:
     - `NetworkResult.success(data, message)`
     - `NetworkResult.error(message, code)`
     - `NetworkResult.exception(throwable)`
   - Never allow backend schema mismatches or non-JSON responses to crash the application.

4. **Error Mapping & Messenger Delivery**:
   - Map technical errors to localized `UiText`:
     - Connection timeout / SocketException → Network connection unavailable.
     - HTTP 401 → Handled centrally via `AuthQueuedInterceptor` & `SessionManager`.
     - HTTP 403 → Permission denied (never logs out).
     - HTTP 5xx → Server error message.

5. **Authentication & Token Management**:
   - Adhere to `@../skills/flutter-network-session-management/SKILL.md` for thread-safe token refresh, loop prevention, and decoupled session expiration.
   - Use Dio `QueuedInterceptor` to handle HTTP 401 challenges, refresh the token, and retry the request once without race conditions.
   - Store access and refresh tokens securely in `flutter_secure_storage`.

6. **UI-First Development When Backend Is Pending**:
   - Never block UI construction on backend availability.
   - Implement `FakeRepository` with simulated latency (500–1000ms) and triggerable states (Happy, Empty, Error).
   - Track all endpoint assumptions in `API_ASSUMPTIONS.md`.

7. **Real-Time WebSocket & Socket Rules (Strictly When User Explicitly Requests)**:
   - **User Request Gate**: **NEVER** implement sockets or WebSockets unless the user **explicitly asks** for a socket implementation. Always default to standard REST HTTP methods.
   - **Mandatory Standards (Followed ONLY When Required by Explicit User Request)**:
     - **Lifecycle Awareness**: Sockets must be connected lazily and managed with app/screen lifecycle awareness. Disconnect when the screen is disposed or app enters the background unless a dedicated background service is running.
     - **Handshake Authentication**: Dynamically attach access tokens during the socket handshake (via header or query parameter). Coordinate auth errors with `SessionManager.notifySessionExpired()`.
     - **Resilient Frame Serialization**: Safely deserialize incoming frames using `jsonDecode`. Catch format exceptions individually so unexpected payloads never crash the socket loop.
     - **Connection Monitoring & Backoff**: Implement heartbeat ping/pong to detect drops. Reconnect using exponential backoff with random jitter.
     - **Messenger & UiText Error Delivery**: Deliver socket drops and errors via `Messenger` and `RequestCode` (e.g. `RequestCode.socketError`, `RequestCode.socketDisconnect`):
       ```dart
       messenger.deliver(
         Message.error(
           UiText.dynamic('Real-time connection lost. Reconnecting...'),
           requestCode: RequestCode.socketDisconnect,
         ),
       );
       ```
