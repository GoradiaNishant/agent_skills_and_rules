# Notification Architecture & Push Management Rules

@../skills/android-notifications-push-management/SKILL.md
@../skills/kotlin-compose-architect/references/03-architecture-decisions.md

## Notification Architecture Governance

1. **System & Channel Registration**:
   - Notification channels must be declared centrally with stable, permanent IDs.
   - Channel registration must occur once in `Application.onCreate()`, never repeatedly generated with dynamic IDs.
   - Channel categories must remain concise (3–5 user-facing categories) with clear names and descriptions.

2. **Runtime Permissions (Android 13+ / API 33+)**:
   - `POST_NOTIFICATIONS` must be requested contextually within a relevant user flow, never unconditionally on initial cold start.
   - Handled as a supported graceful degradation: denial must never crash the app or block user progress.

3. **Firebase Cloud Messaging (FCM)**:
   - Prefer FCM `data` messages (`"data": { ... }`) for mission-critical events to guarantee `onMessageReceived()` execution in foreground, background, and terminated states.
   - Incoming payload maps must be parsed into strongly-typed `PushPayload` models using safe JSON deserializers (`ignoreUnknownKeys = true`).
   - Token Lifecycle: Synchronize device tokens with the backend on `onNewToken()` and post-login; revoke and purge tokens on logout.

4. **Navigation & Deep Linking**:
   - Notification `PendingIntent`s must always include `PendingIntent.FLAG_IMMUTABLE` (unless explicit mutability is required for direct reply).
   - Use `TaskStackBuilder` to construct parent backstacks for deep-link intents, ensuring the Back button returns to the parent screen rather than exiting the application.
   - Deep-link destinations must be validated against current session authentication before navigation.

5. **Presentation & Security**:
   - When the app is actively foregrounded, suppress redundant system tray notifications; route events to in-app banners/snackbars via `Messenger`.
   - Small notification icons must be monochrome alpha stencil drawables.
   - Lock-screen visibility for notifications containing sensitive or personal information must be set to `NotificationCompat.VISIBILITY_PRIVATE`.
   - Never include access tokens, passwords, or full payment credentials in push notification payloads.
