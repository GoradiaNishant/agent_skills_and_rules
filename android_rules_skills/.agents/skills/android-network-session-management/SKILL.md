---
name: android-network-session-management
description: "Retrofit, OkHttp, token auth, and network sessions"
trigger: always_on
alwaysApply: true
globs:
  - "**/data/remote/**"
  - "**/network/**"
---

# Android Network & Session Management Skill

You are an expert Android networking, security, and session management architect.

## 1. Mission & Core Principles

Implement or review a project-adapted, production-ready network and session-handling system so API calls receive consistent transport error handling, authentication recovery, session expiration, and user-facing error reporting without repetitive logic in every ViewModel, Use Case, or API call.

### Core Principles
1. **Standardize behavior, not implementation details**: Do not force an arbitrary architecture, library, class name, or UI framework onto a project. Discover the existing project conventions and adapt.
2. **Never log out on transient or ordinary errors**: Network unavailability, DNS failures, timeouts, HTTP 5xx server errors, serialization mismatches, and HTTP 403 Forbidden (permission denied) must **NEVER** terminate the user's session.
3. **Bounded & coordinated recovery**: Token refresh must be strictly coordinated across concurrent failing requests. Retries must be bounded (max 1 retry per request) with loop guards to prevent infinite 401 storms.
4. **Clean architectural boundaries**: Never make UI calls (Toasts, Snackbars) or navigation decisions (`navController.navigate()`) from OkHttp interceptors, authenticators, or repositories. Use decoupled reactive channels (`StateFlow`, `SharedFlow`).
5. **Fail safe and protect sensitive data**: Never log authorization tokens, credentials, or personal information. Clear sensitive credentials and in-memory caches upon session termination.

---

## 2. When to Use This Skill

Activate this skill when asked to:
- Build, refactor, or audit an Android networking and session layer.
- Implement access token refresh, automatic request retry, or token rotation.
- Prevent duplicate refresh requests during concurrent 401 API failures.
- Implement centralized session expiration and navigation back to the login screen.
- Centralize API error parsing, HTTP status code policies, and user-friendly messaging.
- Eliminate boilerplate `try/catch` blocks and notification logic from ViewModels and repositories.
- Review networking architecture for thread safety, deadlocks, and security compliance.
- Implement real-time WebSocket or Socket client architecture (ONLY when the user explicitly requests socket implementation).

---

## 3. Architecture & Logical Responsibilities

Create or reuse components matching these logical responsibilities:

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose Root UI                         │
│  - Observes SessionState.SessionExpired -> navigates to Login          │
│  - Observes UiMessageDispatcher -> displays Scaffold Snackbar          │
└──────────────────────────────────▲─────────────────────────────────────┘
                                   │ (Reactive Flows)
┌──────────────────────────────────┴─────────────────────────────────────┐
│                 Session & Error Coordination Layer                     │
│  - SessionManager: StateFlow<SessionState>, atomic logout              │
│  - UiMessageDispatcher: SharedFlow<UiMessage>, deduplication           │
│  - TokenProvider / TokenStorage: Keystore / Encrypted storage          │
└──────────────────────────────────▲─────────────────────────────────────┘
                                   │
┌──────────────────────────────────┴─────────────────────────────────────┐
│                      OkHttp Network Pipeline                           │
│  - AuthInterceptor: Injects "Authorization: Bearer <token>"            │
│  - TokenAuthenticator: Handles 401, coordinates thread-safe refresh    │
│  - ErrorInterceptor / ResultCallAdapter: Parses structured ApiError    │
└────────────────────────────────────────────────────────────────────────┘
```

1. **Network Configuration**: Shared OkHttp client, timeouts, SSL/TLS, and base URLs.
2. **Auth Header Interceptor**: Injects `Authorization: Bearer <token>` dynamically; supports bypass annotations/headers for public endpoints.
3. **Token Provider & Storage**: Thread-safe access to tokens backed by secure storage (`EncryptedSharedPreferences` or Keystore-backed DataStore).
4. **Token Refresh Coordinator & Authenticator**: Intercepts HTTP 401 challenges, coordinates a single refresh call across concurrent requests, and retries the original request once.
5. **Session Manager**: Manages `SessionState` (`Authenticated`, `Unauthenticated`, `SessionExpired`), invalidates credentials, and triggers one-time logout navigation.
6. **API Error Parser**: Safely parses backend error bodies without re-reading consumed streams or crashing on non-JSON payloads.
7. **Error Policy**: Maps status codes and transport exceptions to typed domain error representations.
8. **User Message Dispatcher**: Emits structured, lifecycle-safe notifications to the UI without holding `Activity` or `Context` references.

---

## 4. Production Implementation Patterns

### 1. Token Provider & Secure Storage Contract

```kotlin
// core/network/auth/TokenProvider.kt
interface TokenProvider {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun updateTokens(accessToken: String, refreshToken: String?)
    fun clearTokens()
}
```

Implementations must ensure atomic in-memory caching backed by secure persistent storage (`EncryptedSharedPreferences` or AndroidX Security Crypto).

---

### 2. OkHttp Auth Interceptor (Request Preparation & Bypass)

Attaches current credentials only where appropriate and allows public/auth endpoints to opt out:

```kotlin
// core/network/interceptor/AuthInterceptor.kt
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    companion object {
        const val HEADER_NO_AUTH = "No-Authentication"
        const val HEADER_NO_AUTH_VALUE = "true"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 1. Bypass endpoints marked with No-Authentication header
        if (originalRequest.header(HEADER_NO_AUTH) == HEADER_NO_AUTH_VALUE) {
            val sanitizedRequest = originalRequest.newBuilder()
                .removeHeader(HEADER_NO_AUTH)
                .build()
            return chain.proceed(sanitizedRequest)
        }

        // 2. Attach Bearer token if present
        val token = tokenProvider.getAccessToken()
        val request = if (!token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(request)
    }
}
```

---

### 3. OkHttp Authenticator (Thread-Safe 401 Coordination & Loop Guard)

When multiple concurrent requests fail with 401, OkHttp calls `Authenticator.authenticate()`.
**Mandatory Rules:**
1. **Loop Guard**: If the request has already been retried (`response.priorResponseCount() >= 2`), return `null` immediately.
2. **Fast Retry (Token Invalidation Race Guard)**: If the token in the failing request is different from the current stored token, another thread already refreshed it! Retry immediately with the current token without calling the refresh endpoint again.
3. **Synchronized Coordination**: Use a lock/mutex so only **one** thread executes the refresh API call while other concurrent threads wait.
4. **Idempotency**: Do not retry non-idempotent mutations if the backend contract cannot guarantee safe replay.

```kotlin
// core/network/auth/TokenAuthenticator.kt
class TokenAuthenticator @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val sessionManager: SessionManager,
    private val authApiProvider: Provider<AuthRefreshApi> // Lazy provider to break circular OkHttp dependency
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // 1. Loop guard: prevent infinite retry loops
        if (responseCount(response) >= 2) {
            return null
        }

        val request = response.request
        val failedAuthorizationHeader = request.header("Authorization")

        synchronized(lock) {
            val currentToken = tokenProvider.getAccessToken()
            val currentAuthorizationHeader = currentToken?.let { "Bearer $it" }

            // 2. Race condition check: If token was already refreshed by another thread, retry immediately
            if (currentAuthorizationHeader != null && currentAuthorizationHeader != failedAuthorizationHeader) {
                return request.newBuilder()
                    .header("Authorization", currentAuthorizationHeader)
                    .build()
            }

            // 3. Obtain refresh token
            val refreshToken = tokenProvider.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                sessionManager.notifySessionExpired(SessionExpiredReason.REFRESH_TOKEN_MISSING)
                return null
            }

            // 4. Perform synchronous refresh call (Authenticator runs on OkHttp background thread)
            val refreshResult = runCatching {
                authApiProvider.get().refreshTokenSync(RefreshTokenRequestDto(refreshToken))
            }.getOrNull()

            if (refreshResult != null && refreshResult.isSuccessful && refreshResult.body() != null) {
                val newTokens = refreshResult.body()!!
                tokenProvider.updateTokens(
                    accessToken = newTokens.accessToken,
                    refreshToken = newTokens.refreshToken ?: refreshToken
                )

                // 5. Retry the original request with new token
                return request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.accessToken}")
                    .build()
            } else {
                // Refresh failed or revoked: terminate session once
                sessionManager.notifySessionExpired(SessionExpiredReason.REFRESH_TOKEN_EXPIRED)
                return null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
```

> [!IMPORTANT]
> The `authApiProvider` MUST use an unauthenticated OkHttp client instance (or one configured without `TokenAuthenticator`) to avoid recursive deadlocks.

---

### 4. Centralized Session Manager & Reactive State

```kotlin
// core/session/SessionManager.kt
enum class SessionExpiredReason {
    REFRESH_TOKEN_EXPIRED,
    REFRESH_TOKEN_MISSING,
    ACCOUNT_REVOKED,
    USER_LOGOUT
}

sealed interface SessionState {
    data object Authenticated : SessionState
    data object Unauthenticated : SessionState
    data class Expired(val reason: SessionExpiredReason) : SessionState
}

@Singleton
class SessionManager @Inject constructor(
    private val tokenProvider: TokenProvider
) {
    private val _sessionState = MutableStateFlow<SessionState>(
        if (tokenProvider.getAccessToken() != null) SessionState.Authenticated else SessionState.Unauthenticated
    )
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    fun notifySessionExpired(reason: SessionExpiredReason) {
        if (_sessionState.value is SessionState.Expired) return // Idempotent: prevent duplicate triggers
        
        tokenProvider.clearTokens()
        _sessionState.value = SessionState.Expired(reason)
    }

    fun logout() {
        tokenProvider.clearTokens()
        _sessionState.value = SessionState.Unauthenticated
    }

    fun markAuthenticated() {
        _sessionState.value = SessionState.Authenticated
    }
}
```

---

### 5. Decoupled Global User Messaging (`UiText` + `Message` + `Messenger`)

**Anti-Pattern (Hardcoded Raw Strings):**
```kotlin
// ❌ NEVER do this: hardcoded string, breaks localization, and ignores backend error messages:
NetworkResult.Error("No internet connection. Please check your network.", code = null)
```

**Recommended Pattern (`UiText.from` + `messenger.deliver`):**
Repositories and ViewModels dispatch structured messages without holding Android `Context` or `Activity` references, leveraging `UiText` for dynamic API messages with localized fallback:

```kotlin
// In Repository or Network Error Policy:
val errorText = UiText.from(
    apiMessage = result.message,
    fallbackRes = R.string.no_connectivity_message
)

// Deliver globally to root UI:
messenger.deliver(
    Message.error(
        content = errorText,
        requestCode = requestCode
    )
)

// Return structured error to calling layer:
return NetworkResult.Error(message = errorText, code = null)
```

#### Core Components
- **`UiText`** (`templates/UiText.kt`): Polymorphic string model supporting `DynamicString` (API error payload) and `StringResource` (`R.string.<id>`). Smart factory `UiText.from(apiMessage, fallbackRes)` prioritizes API messages when present and non-blank.
- **`Message`** (`templates/Message.kt`): Typed notification model with `Type` (`SUCCESS`, `ERROR`, `WARNING`, `INFO`), optional `RequestCode`, and action callbacks.
- **`Messenger`** (`templates/Messenger.kt`): `@Singleton` / `@ActivityRetainedScoped` event pipeline exposing `SharedFlow<Message>` with built-in 2000ms duplicate suppression.

```kotlin
// core/ui/message/Messenger.kt
@Singleton
class Messenger @Inject constructor() {
    private val _message = MutableSharedFlow<Message>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val message: SharedFlow<Message> = _message.asSharedFlow()

    private var lastDispatchedKey: Any? = null
    private var lastDispatchedTime: Long = 0L

    fun deliver(message: Message, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && message.content == lastDispatchedKey && now - lastDispatchedTime < 2000L) {
            return
        }
        lastDispatchedKey = message.content
        lastDispatchedTime = now
        _message.tryEmit(message)
    }
}
```

*Templates available in `templates/` directory:*
- [`templates/UiText.kt`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/android_rules_skills/.agents/skills/android-network-session-management/templates/UiText.kt)
- [`templates/Message.kt`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/android_rules_skills/.agents/skills/android-network-session-management/templates/Message.kt)
- [`templates/Messenger.kt`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/android_rules_skills/.agents/skills/android-network-session-management/templates/Messenger.kt)
- [`templates/NetworkResult.kt`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/android_rules_skills/.agents/skills/android-network-session-management/templates/NetworkResult.kt)

---

### 6. Robust Error Parsing, Schema Resilience & Default Policy

#### Resilient Serialization Configuration
Always configure the application's JSON serializer (`Json` or `Moshi`) to absorb backend schema changes without crashing:
```kotlin
// core/di/NetworkModule.kt
@Provides
@Singleton
fun provideJson(): Json = Json {
    ignoreUnknownKeys = true    // CRITICAL: prevents crash when backend adds new fields
    coerceInputValues = true    // CRITICAL: prevents crash if null is returned for default values
    isLenient = true            // Allows relaxed JSON formatting (e.g. unquoted keys)
    explicitNulls = false       // Does not fail when omitted fields are serialized
}
```

```kotlin
// core/network/error/ApiError.kt
sealed interface ApiError {
    data class Network(val message: String) : ApiError
    data class Timeout(val message: String) : ApiError
    data class Serialization(val message: String) : ApiError
    data class BadRequest(val message: String) : ApiError
    data class Unauthorized(val message: String) : ApiError
    data class Forbidden(val message: String) : ApiError
    data class NotFound(val message: String) : ApiError
    data class Conflict(val message: String) : ApiError
    data class Validation(val message: String, val fieldErrors: Map<String, String>) : ApiError
    data class RateLimited(val message: String) : ApiError
    data class Server(val message: String) : ApiError
    data class Http(val code: Int, val message: String) : ApiError
    data class Unknown(val message: String) : ApiError
}

// core/network/error/ApiErrorParser.kt
object ApiErrorParser {

    fun parseError(response: retrofit2.Response<*>?): ApiError {
        if (response == null) {
            return ApiError.Unknown("No response received from server")
        }

        val code = response.code()
        val rawBody = runCatching { response.errorBody()?.string() }.getOrNull()

        // 1. Attempt JSON parsing of backend error payload
        val backendMessage = rawBody?.let { extractBackendErrorMessage(it) }

        return when (code) {
            400 -> ApiError.BadRequest(backendMessage ?: "Invalid request details")
            401 -> ApiError.Unauthorized(backendMessage ?: "Session expired. Please log in again.")
            403 -> ApiError.Forbidden(backendMessage ?: "You do not have permission to perform this action.")
            404 -> ApiError.NotFound(backendMessage ?: "Requested resource was not found.")
            409 -> ApiError.Conflict(backendMessage ?: "A conflict occurred with existing data.")
            422 -> ApiError.Validation(backendMessage ?: "Validation failed", extractFieldErrors(rawBody))
            429 -> ApiError.RateLimited("Too many requests. Please wait a moment.")
            in 500..599 -> ApiError.Server(backendMessage ?: "Server error. Please try again later.")
            else -> ApiError.Http(code, backendMessage ?: "Unexpected error occurred ($code)")
        }
    }

    fun parseException(throwable: Throwable): ApiError {
        return when (throwable) {
            is UnknownHostException, is ConnectException -> 
                ApiError.Network("No internet connection. Please check your network.")
            is SocketTimeoutException -> 
                ApiError.Timeout("Connection timed out. Please try again.")
            is kotlinx.serialization.SerializationException, is com.squareup.moshi.JsonDataException -> 
                ApiError.Serialization("Unable to process server response. Please update the app or try again.")
            is CancellationException -> 
                throw throwable // Re-throw coroutine cancellation
            else -> 
                ApiError.Unknown(throwable.localizedMessage ?: "An unexpected error occurred.")
        }
    }

    private fun extractBackendErrorMessage(json: String): String? {
        // Safe extraction avoiding crash if response is HTML (e.g. 502 Bad Gateway)
        return runCatching {
            val jsonElement = Json.parseToJsonElement(json).jsonObject
            jsonElement["message"]?.jsonPrimitive?.contentOrNull
                ?: jsonElement["error"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()
    }

    private fun extractFieldErrors(json: String?): Map<String, String> {
        if (json == null) return emptyMap()
        return runCatching {
            val jsonElement = Json.parseToJsonElement(json).jsonObject
            val errors = jsonElement["errors"]?.jsonObject ?: return emptyMap()
            errors.mapValues { it.value.jsonPrimitive.content }
        }.getOrDefault(emptyMap())
    }
}
```

---

### 7. Jetpack Compose Root UI Session & Message Integration

Connect session state and messages safely at the app root composable:

```kotlin
// ui/root/RootAppContent.kt
@Composable
fun RootAppContent(
    navController: NavHostController,
    sessionManager: SessionManager = hiltViewModel<RootViewModel>().sessionManager,
    messageDispatcher: UiMessageDispatcher = hiltViewModel<RootViewModel>().messageDispatcher
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val sessionState by sessionManager.sessionState.collectAsStateWithLifecycle()

    // 1. Observe session expiration and navigate safely
    LaunchedEffect(sessionState) {
        when (val state = sessionState) {
            is SessionState.Expired -> {
                snackbarHostState.showSnackbar(
                    message = "Your session has expired. Please log in again.",
                    duration = SnackbarDuration.Short
                )
                // Clear backstack and route to Auth
                navController.navigate("auth_route") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
            is SessionState.Unauthenticated -> {
                navController.navigate("auth_route") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
            is SessionState.Authenticated -> { /* Normal app flow */ }
        }
    }

    // 2. Observe global UI messages via Messenger
    val context = LocalContext.current
    LaunchedEffect(messenger) {
        messenger.message.collect { message ->
            val resolvedText = message.content.asString(context) ?: return@collect
            val actionText = message.actionLabel?.asString(context)
            val result = snackbarHostState.showSnackbar(
                message = resolvedText,
                actionLabel = actionText,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                message.onAction?.invoke()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
```

---

### 8. Real-Time WebSocket / Socket Architecture (Strictly When User Explicitly Requests)

> [!IMPORTANT]
> **User Request Gate**: NEVER implement or introduce WebSockets/Sockets proactively. Implement socket connections **ONLY when the user explicitly requests socket or real-time bi-directional communication**. Always default to standard REST/HTTP suspend functions.

When the user explicitly requests socket implementation, adhere to the following architecture:

#### Core Socket Responsibilities
1. **Handshake Authentication**: Inject the current Bearer token dynamically during the handshake (via header or query parameter). If the handshake or socket connection returns an authentication error (HTTP 401/403 or server auth close code `4401`/`4403`), invoke `sessionManager.notifySessionExpired()`.
2. **Lifecycle-Aware Connection**: Bind the socket lifecycle to the UI/Screen lifecycle (or Foreground Service if continuous background streaming is explicitly required). Disconnect or pause listening when the app enters the background to conserve battery and bandwidth.
3. **Resilient Frame Parsing**: Incoming text/binary messages must be deserialized with `Json { ignoreUnknownKeys = true }`. Always catch frame serialization exceptions individually; an unexpected payload must never crash the socket loop or the app.
4. **Heartbeat & Exponential Backoff Reconnection**: Implement ping/pong frames to detect dropped connections. On disconnection, attempt reconnection using exponential backoff with random jitter up to a bounded maximum ceiling.
5. **Decoupled User Messaging (`Messenger` + `RequestCode`)**: Deliver connection lost, heartbeat failure, or reconnection error notifications globally through `Messenger` and `RequestCode`:

```kotlin
// core/network/socket/WebSocketManager.kt
@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenProvider: TokenProvider,
    private val sessionManager: SessionManager,
    private val messenger: Messenger,
    private val json: Json
) {
    private var webSocket: WebSocket? = null
    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

    fun connect(url: String) {
        val token = tokenProvider.getAccessToken()
        val request = Request.Builder()
            .url(url)
            .apply {
                if (!token.isNullOrBlank()) {
                    header("Authorization", "Bearer $token")
                }
            }
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _events.tryEmit(SocketEvent.Connected)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Safely parse frame without crashing on schema changes
                runCatching {
                    val frame = json.decodeFromString<SocketFrameDto>(text)
                    _events.tryEmit(SocketEvent.MessageReceived(frame))
                }.onFailure { e ->
                    // Log error safely without crashing
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (response?.code == 401 || response?.code == 403) {
                    sessionManager.notifySessionExpired(SessionExpiredReason.REFRESH_TOKEN_EXPIRED)
                    return
                }

                messenger.deliver(
                    Message.error(
                        content = UiText.res(R.string.socket_connection_lost),
                        requestCode = RequestCode.SOCKET_ERROR
                    )
                )
                _events.tryEmit(SocketEvent.Error(t))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _events.tryEmit(SocketEvent.Disconnected(code, reason))
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal Closure")
        webSocket = null
    }
}
```

---

## 5. Security & Sensitive Data Handling

1. **Storage**: Never store tokens in plain `SharedPreferences`. Use `EncryptedSharedPreferences` or Android Keystore.
2. **Logging**: Never log HTTP `Authorization` headers, bearer tokens, passwords, or refresh tokens. Use an OkHttp `HttpLoggingInterceptor` with `Level.BODY` only in `DEBUG` builds, and redact headers:
   ```kotlin
   loggingInterceptor.redactHeader("Authorization")
   loggingInterceptor.redactHeader("Cookie")
   ```
3. **Session Purge**: On session expiration or logout, clear:
   - Access & refresh tokens from secure storage.
   - User profile and permissions in-memory caches.
   - Sensitive local Room/DataStore user data if partitioned by user.

---

## 6. Code Review & Implementation Checklist

When reviewing or creating networking and session management code, verify:

- [ ] **No UI/Navigation in Interceptors**: Interceptors and authenticators do not reference `Activity`, `Context`, `Toast`, or `NavController`.
- [ ] **Thread-Safe Authenticator**: Token refresh in OkHttp `Authenticator` is synchronized to prevent multiple concurrent refresh calls.
- [ ] **Double-Check Race Guard**: Authenticator checks if another thread already refreshed the token before making an API call.
- [ ] **Loop Guard**: `response.priorResponseCount() >= 2` terminates retry attempts with `null`.
- [ ] **No Infinite Recursion**: The refresh endpoint itself uses a client without `TokenAuthenticator`.
- [ ] **Public Endpoints Bypassed**: Public and authentication endpoints do not attach stale tokens or trigger authenticators.
- [ ] **403 Does Not Log Out**: HTTP 403 displays a permission message and does NOT clear session or redirect to login.
- [ ] **Network Errors Do Not Log Out**: Offline, timeouts, and 5xx errors display user messages and do NOT terminate session.
- [ ] **Safe Error Body Consumption**: `response.errorBody()?.string()` is read safely once and does not crash on HTML or malformed JSON.
- [ ] **Message Deduplication**: Rapid simultaneous API failures do not flood the user with duplicate Snackbars.
- [ ] **Backstack Cleared on Logout**: Session expiration navigation uses `popUpTo(0) { inclusive = true }`.
- [ ] **Schema Evolution & Crash Protection**: JSON serializer configured with `ignoreUnknownKeys = true` and `coerceInputValues = true`; `SerializationException` / `JsonDataException` caught and mapped to `ApiError.Serialization` / `NetworkResult.Error` so unexpected backend response changes never crash the app.
- [ ] **Tokens Redacted in Logs**: `Authorization` and sensitive headers are redacted from logging interceptors.
- [ ] **User-Requested Socket Only**: Sockets/WebSockets are implemented ONLY because the user explicitly requested them (never assumed or introduced proactively).
- [ ] **Socket Authentication & Lifecycle**: Socket handshakes attach dynamic auth tokens, coordinate 401/auth failures with `SessionManager`, and respect Android lifecycle (disconnecting in background unless in a foreground service).
- [ ] **Socket Frame Resilience & Messenger Delivery**: Incoming socket frames are parsed with schema evolution guards, and connection drops/failures notify the UI via `Messenger` and `RequestCode.SOCKET_*`.

---

## 7. Required Final Report

When completing a network and session implementation task, provide a report containing:
1. **Inspected Architecture**: Detected HTTP client, serialization, DI, and storage framework.
2. **Components Created/Updated**: AuthInterceptor, Authenticator, SessionManager, ErrorParser, MessageDispatcher.
3. **Concurrency & Refresh Strategy**: How concurrent 401s are synchronized and bounded.
4. **Session Termination Path**: How unrecoverable sessions transition to the login screen and clean up state.
5. **Verification**: Executed unit/integration tests and manual scenario checks.
