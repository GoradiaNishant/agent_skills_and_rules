---
name: flutter-network-session-management
description: >-
  Design, review, and implement reusable centralized networking with Dio, API error handling,
  authentication refresh, session expiration, and global user notifications in Flutter projects.
  Use when building or modifying API clients, auth/session flows, network error policies,
  interceptors, or app-wide error messaging.
---

# Flutter Network & Session Management Skill

You are an expert Flutter networking, security, and session management architect.

## 1. Mission & Core Principles

Implement or review a production-ready network and session-handling system so API calls receive consistent transport error handling, authentication recovery, session expiration, and user-facing error reporting without repetitive logic in every BLoC, Cubit, or Repository.

### Core Principles
1. **Standardize Behavior, Not Implementation Details**: Discover existing project conventions and adapt.
2. **Never Log Out on Transient Errors**: Network unavailability, timeouts, HTTP 5xx, or HTTP 403 Forbidden must **NEVER** terminate the user's session.
3. **Bounded & Coordinated Recovery**: Token refresh must be strictly coordinated across concurrent failing requests using `Dio`'s `QueuedInterceptor`. Retries must be bounded (max 1 retry per request) with loop guards to prevent infinite 401 loops.
4. **Clean Architectural Boundaries**: Never make UI calls (`ScaffoldMessenger.showSnackBar`) or navigation decisions directly from Dio interceptors or repositories. Use decoupled reactive channels (`Messenger`, `SessionManager`).
5. **Fail Safe and Protect Sensitive Data**: Never log authorization tokens, credentials, or personal information. Clear tokens upon session termination.

---

## 2. Architecture & Logical Responsibilities

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        Flutter Root App UI                             │
│  - Listens to SessionState.expired -> routes to LoginScreen            │
│  - Listens to Messenger.message -> displays ScaffoldMessenger SnackBar │
└──────────────────────────────────▲─────────────────────────────────────┘
                                   │ (Streams / ValueNotifiers)
┌──────────────────────────────────┴─────────────────────────────────────┐
│                 Session & Error Coordination Layer                     │
│  - SessionManager: ValueNotifier<SessionState>, atomic logout          │
│  - Messenger: Stream<Message>, deduplication                           │
│  - TokenStorage: flutter_secure_storage (Keystore / Keychain)          │
└──────────────────────────────────▲─────────────────────────────────────┘
                                   │
┌──────────────────────────────────┴─────────────────────────────────────┐
│                       Dio Network Pipeline                             │
│  - AuthInterceptor: Injects "Authorization: Bearer <token>"            │
│  - AuthQueuedInterceptor: Handles 401, coordinates atomic refresh      │
│  - ErrorInterceptor: Maps DioException to typed NetworkResult         │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Production Implementation Patterns

### 1. Token Provider & Secure Storage Contract
```dart
// core/network/auth/token_provider.dart
abstract interface class TokenProvider {
  Future<String?> getAccessToken();
  Future<String?> getRefreshToken();
  Future<void> updateTokens({required String accessToken, String? refreshToken});
  Future<void> clearTokens();
}

// core/network/auth/secure_token_provider.dart
@lazySingleton
class SecureTokenProvider implements TokenProvider {
  final FlutterSecureStorage _storage;
  SecureTokenProvider(this._storage);

  @override
  Future<String?> getAccessToken() => _storage.read(key: 'access_token');

  @override
  Future<String?> getRefreshToken() => _storage.read(key: 'refresh_token');

  @override
  Future<void> updateTokens({required String accessToken, String? refreshToken}) async {
    await _storage.write(key: 'access_token', value: accessToken);
    if (refreshToken != null) {
      await _storage.write(key: 'refresh_token', value: refreshToken);
    }
  }

  @override
  Future<void> clearTokens() async {
    await _storage.delete(key: 'access_token');
    await _storage.delete(key: 'refresh_token');
  }
}
```

---

### 2. Dio QueuedInterceptor (Thread-Safe 401 Coordination & Loop Guard)

When multiple concurrent requests fail with 401, `QueuedInterceptor` locks request queuing so only **one** refresh call occurs:

```dart
// core/network/interceptor/auth_queued_interceptor.dart
@injectable
class AuthQueuedInterceptor extends QueuedInterceptor {
  final TokenProvider _tokenProvider;
  final SessionManager _sessionManager;
  final Dio _refreshDio; // Unauthenticated client to avoid recursive loops

  AuthQueuedInterceptor(
    this._tokenProvider,
    this._sessionManager,
    @Named('refreshDio') this._refreshDio,
  );

  @override
  Future<void> onError(DioException err, ErrorInterceptorHandler handler) async {
    if (err.response?.statusCode != 401) {
      return handler.next(err);
    }

    // 1. Loop guard: prevent infinite retry loops
    final requestOptions = err.requestOptions;
    final retryCount = requestOptions.extra['retry_count'] as int? ?? 0;
    if (retryCount >= 1) {
      return handler.next(err);
    }

    final refreshToken = await _tokenProvider.getRefreshToken();
    if (refreshToken == null || refreshToken.isEmpty) {
      _sessionManager.notifySessionExpired(SessionExpiredReason.refreshTokenMissing);
      return handler.next(err);
    }

    try {
      // 2. Coordinated atomic refresh call
      final response = await _refreshDio.post(
        '/api/v1/auth/refresh',
        data: {'refresh_token': refreshToken},
      );

      final newAccessToken = response.data['access_token'] as String;
      final newRefreshToken = response.data['refresh_token'] as String?;

      await _tokenProvider.updateTokens(
        accessToken: newAccessToken,
        refreshToken: newRefreshToken,
      );

      // 3. Retry original request once
      requestOptions.headers['Authorization'] = 'Bearer $newAccessToken';
      requestOptions.extra['retry_count'] = retryCount + 1;

      final clonedRequest = await _refreshDio.fetch(requestOptions);
      return handler.resolve(clonedRequest);
    } catch (refreshErr) {
      // Refresh failed: terminate session once
      _sessionManager.notifySessionExpired(SessionExpiredReason.refreshTokenExpired);
      return handler.next(err);
    }
  }
}
```

---

### 3. Centralized Session Manager
```dart
// core/session/session_manager.dart
enum SessionExpiredReason {
  refreshTokenExpired,
  refreshTokenMissing,
  accountRevoked,
  userLogout,
}

sealed class SessionState {
  const SessionState();
  const factory SessionState.authenticated() = _Authenticated;
  const factory SessionState.unauthenticated() = _Unauthenticated;
  const factory SessionState.expired(SessionExpiredReason reason) = _Expired;
}

@singleton
class SessionManager {
  final TokenProvider _tokenProvider;
  final ValueNotifier<SessionState> sessionState =
      ValueNotifier(const SessionState.unauthenticated());

  SessionManager(this._tokenProvider);

  void notifySessionExpired(SessionExpiredReason reason) {
    if (sessionState.value is _Expired) return; // Idempotent
    _tokenProvider.clearTokens();
    sessionState.value = SessionState.expired(reason);
  }

  void logout() {
    _tokenProvider.clearTokens();
    sessionState.value = const SessionState.unauthenticated();
  }

  void markAuthenticated() {
    sessionState.value = const SessionState.authenticated();
  }
}
```

---

### 4. Decoupled Global User Messaging (`UiText` + `Message` + `Messenger`)

Templates available in `templates/`:
- [`templates/network_result.dart`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/flutter_rules_skills/.agents/skills/flutter-network-session-management/templates/network_result.dart)
- [`templates/ui_text.dart`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/flutter_rules_skills/.agents/skills/flutter-network-session-management/templates/ui_text.dart)
- [`templates/message.dart`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/flutter_rules_skills/.agents/skills/flutter-network-session-management/templates/message.dart)
- [`templates/messenger.dart`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/flutter_rules_skills/.agents/skills/flutter-network-session-management/templates/messenger.dart)

---

### 5. Real-Time WebSocket / Socket Architecture (Strictly When User Explicitly Requests)

> [!IMPORTANT]
> **User Request Gate**: NEVER implement or introduce WebSockets/Sockets proactively. Implement socket connections **ONLY when the user explicitly requests socket or real-time bi-directional communication**. Always default to standard REST HTTP methods.

When the user explicitly requests socket implementation:
1. Use `web_socket_channel` with dynamic Bearer token query parameter/header during handshake.
2. If the connection fails with 401/auth close code, coordinate with `sessionManager.notifySessionExpired()`.
3. Wrap incoming message stream with safe JSON parsing; never crash the app on unexpected message schemas.
4. Implement automatic reconnection using exponential backoff with jitter.
5. Deliver connection failures and drops via `messenger.deliver(Message.error(content, requestCode: RequestCode.socketError))`.

```dart
// core/network/socket/socket_manager.dart
@singleton
class AppWebSocketManager {
  final TokenProvider _tokenProvider;
  final SessionManager _sessionManager;
  final Messenger _messenger;
  WebSocketChannel? _channel;

  AppWebSocketManager(this._tokenProvider, this._sessionManager, this._messenger);

  Future<void> connect(String baseUrl) async {
    final token = await _tokenProvider.getAccessToken();
    final uri = Uri.parse('$baseUrl?token=$token');

    try {
      _channel = WebSocketChannel.connect(uri);
      await _channel?.ready;
    } catch (e) {
      _messenger.deliver(
        Message.error(
          UiText.dynamic('Real-time connection failed. Reconnecting...'),
          requestCode: RequestCode.socketError,
        ),
      );
    }
  }

  void disconnect() {
    _channel?.sink.close();
    _channel = null;
  }
}
```

---

## 4. Code Review & Implementation Checklist

- [ ] **No UI/Navigation in Interceptors**: Dio interceptors never reference `BuildContext`, `Navigator`, or `ScaffoldMessenger`.
- [ ] **Coordinated Refresh**: 401 token refresh uses `QueuedInterceptor` to lock concurrent requests.
- [ ] **Loop Guard**: `retry_count >= 1` prevents infinite 401 loops.
- [ ] **Dedicated Refresh Client**: The token refresh request uses an unauthenticated Dio instance to prevent deadlocks.
- [ ] **User-Requested Socket Only**: Sockets/WebSockets are implemented ONLY because the user explicitly requested them.
- [ ] **Safe Schema Evolution**: DTO deserialization tolerates unknown keys and null values without crashing.
