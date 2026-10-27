---
name: 40-api-data-layer
description: "Retrofit, OkHttp, DTOs, network interceptors, and repositories"
trigger: always_on
alwaysApply: true
globs:
  - "**/data/**"
  - "**/remote/**"
  - "**/api/**"
  - "**/dto/**"
---

# API and Data Layer Rules

@../skills/kotlin-compose-architect/references/03-architecture-decisions.md
@../skills/kotlin-compose-architect/references/07-security.md
@../skills/ui-first-mock-api/SKILL.md
@../skills/android-network-session-management/SKILL.md

## API & Response Pattern Conventions

1. **Endpoint & Retrofit Service Declarations**:
   - Declare Retrofit endpoints in `data/remote/api/` using Kotlin `suspend` functions.
   - Base URLs and sensitive API keys must come from `BuildConfig` and `local.properties`—never hardcoded.
   - Group related endpoints logically within cohesive API interfaces.

2. **Safe Serialization & Data Transfer Objects (DTOs)**:
   - Configure the JSON serializer (`Json` or `Moshi`) for schema-evolution resilience:
     - Always set `ignoreUnknownKeys = true` (prevents crashes when backend adds new properties).
     - Always set `coerceInputValues = true` and `isLenient = true` (tolerates nulls and relaxed formats).
   - Every JSON field in remote DTOs must be nullable (`String?`, `Int?`) with `= null` default to prevent parsing crashes when API contracts change.
   - Avoid raw enums in DTOs (parse as `String?` and map safely to domain enums with an `UNKNOWN` fallback).
   - Separate remote DTOs (`UserResponseDto`) from domain models (`User`) via explicit extension mappers.

3. **Result Wrapper & Repository Methods**:
   - Repositories must execute network requests on `Dispatchers.IO` and return a typed result wrapper: `NetworkResult<T>` carrying localized `UiText` messages:
     - `NetworkResult.Success(val data: T, val message: UiText? = null)`
     - `NetworkResult.Error(val message: UiText, val code: Int? = null)`
     - `NetworkResult.Exception(val throwable: Throwable)`
   - Catch specific exceptions (`IOException`, `HttpException`, `SerializationException`) and convert them into domain-appropriate `UiText` representations.
   - **Crash Protection on Schema Failure**: Always catch `SerializationException` (Kotlinx) or `JsonDataException` (Moshi) and map to `NetworkResult.Error(UiText.res(R.string.error_response_malformed))`. The app must **NEVER** crash on response schema changes.
   - Always re-throw `CancellationException` to preserve coroutine structured cancellation.

4. **Error Mapping & Messenger Delivery**:
   - **NEVER** hardcode raw English strings (e.g. `NetworkResult.Error("No internet connection...")`).
   - **ALWAYS** deliver user notifications via `Messenger` and `UiText`:
     ```kotlin
     val errorText = UiText.from(result.message, R.string.no_connectivity_message)
     messenger.deliver(Message.error(errorText))
     return NetworkResult.Error(errorText, code = null)
     ```
   - Technical error mappings:
     - `IOException` / `UnknownHostException` / `ConnectException` → `UiText.res(R.string.no_connectivity_message)`
     - `SocketTimeoutException` → `UiText.res(R.string.timeout_message)`
     - `SerializationException` / `JsonDataException` → `UiText.res(R.string.error_response_malformed)`
     - `HttpException(401)` → Unauthorized / session expired (handled via `TokenAuthenticator` & `SessionManager`).
     - `HttpException(403)` → `UiText.res(R.string.error_permission_denied)` (never logs out).
     - `HttpException(5xx)` → `UiText.from(backendMessage, R.string.error_server_problem)`.
   - Never leak raw stack traces or SQL/HTTP exception messages directly to the UI.

5. **Authentication & Token Management**:
   - Adhere to `@../skills/android-network-session-management/SKILL.md` for thread-safe token refresh, loop prevention, and decoupled session expiration.
   - Use an OkHttp `Interceptor` to attach `Authorization: Bearer <token>` to authenticated endpoints, with support for public endpoint bypass.
   - Use an OkHttp `Authenticator` with thread synchronization and retry loop guards to handle HTTP 401 responses and coordinate single-token refresh without deadlock.
   - Store access and refresh tokens securely in encrypted storage (EncryptedSharedPreferences or DataStore with Android Keystore).

6. **UI-First Development When Backend Is Pending**:
   - Never block UI construction on backend availability or hardcode dummy fixtures in screens.
   - Interpose a repository interface and implement a `FakeRepository` with simulated network delay (500–1200ms) and scenario triggers (Happy, Empty, Error, 401, 500).
   - Swap between fake and real repositories via DI (`@Binds`) or a configuration flag (`BuildConfig.USE_MOCK_DATA`) without touching UI or ViewModel code.
   - Track all assumed endpoints, payload structures, and ambiguities in `API_ASSUMPTIONS.md`. (See `@../skills/ui-first-mock-api/SKILL.md`).

7. **Real-Time WebSocket & Socket Rules (Strictly When User Explicitly Requests)**:
   - **User Request Gate**: **NEVER** implement sockets or WebSockets unless the user **explicitly asks** for a socket implementation. Always default to standard REST suspend functions.
   - **Mandatory Standards (Followed ONLY When Required by Explicit User Request)**:
     - **Lifecycle Awareness**: Sockets must be connected lazily and managed with app/screen lifecycle awareness. Never keep idle sockets open in the background without a Foreground Service.
     - **Handshake Authentication**: Dynamically attach access tokens during the socket handshake (via header or query parameter). If the socket receives an authentication failure (HTTP 401/403 or server auth close code), coordinate with `SessionManager.notifySessionExpired()`.
     - **Resilient Frame Serialization**: Safely deserialize incoming text/binary frames with schema-evolution resilience (`Json { ignoreUnknownKeys = true }`). Catch serialization errors per frame; never allow an unexpected socket payload to crash the connection or the application.
     - **Connection Monitoring & Backoff**: Implement heartbeat ping/pong to detect silent drops. Implement automatic reconnection using exponential backoff with jitter up to a bounded ceiling.
     - **Messenger & UiText Error Delivery**: When socket connection drops, reconnect attempts fail, or terminal socket errors occur, deliver notifications via `Messenger` using `RequestCode` (e.g. `RequestCode.SOCKET_CONNECT`, `RequestCode.SOCKET_ERROR`, `RequestCode.SOCKET_DISCONNECT`) and localized `UiText`:
       ```kotlin
       messenger.deliver(
           Message.error(
               content = UiText.res(R.string.socket_connection_lost),
               requestCode = RequestCode.SOCKET_DISCONNECT
           )
       )
       ```


