---
name: 65-notifications-push-management
description: "Firebase Messaging, local notifications, and payload routing"
trigger: always_on
alwaysApply: true
globs:
  - "**/notifications/**"
  - "**/fcm/**"
---

# Notification Architecture & Push Management Rules

@../skills/flutter-notifications-push-management/SKILL.md
@../skills/flutter-architect/references/03-architecture-decisions.md

## Notification Architecture Governance

1. **System & Channel Registration**:
   - Register Android notification channels centrally with stable, permanent IDs upon app launch.
   - Category channels must remain concise (3–5 user-facing categories) with clear names and descriptions.
2. **Runtime Permissions (Android 13+ & iOS)**:
   - Request notification permissions contextually during an onboarding or relevant feature step, never unconditionally on first cold start.
   - Graceful degradation: Permission denial must never crash the app or block user progress.
3. **Firebase Cloud Messaging (FCM)**:
   - Prefer FCM `data` messages for background reliability across Android and iOS.
   - Incoming payloads must be parsed safely using strongly-typed models with `ignoreUnknownKeys`.
   - Token Lifecycle: Sync device tokens with backend post-login; revoke and purge tokens on logout.
4. **Foreground Delivery & Presentation**:
   - When the app is foregrounded, suppress redundant system tray notifications; route in-app notifications to SnackBars/Banners via `Messenger`.
5. **Security**:
   - Never include access tokens, passwords, or full credit card numbers in push notification payloads.
