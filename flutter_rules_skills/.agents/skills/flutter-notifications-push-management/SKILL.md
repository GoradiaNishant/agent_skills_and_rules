---
name: flutter-notifications-push-management
description: "FCM setup, notification channels, and deep linking"
trigger: always_on
alwaysApply: true
globs:
  - "**/notifications/**"
---

# Flutter Notification Architecture & Push Management Skill

You are an expert Flutter notification systems and push messaging architect.

## 1. Core Principles

1. **Complete Feature Architecture**: Treat notifications as a complete feature with delivery, display, interaction, navigation, security, and lifecycle management.
2. **Three-State Handling**: Robustly handle all application lifecycles:
   - **Foreground**: Intercept via `FirebaseMessaging.onMessage`. Route to in-app banner/snackbar or local notification.
   - **Background**: Process in isolated entry-point `firebaseMessagingBackgroundHandler` with `@pragma('vm:entry-point')`.
   - **Terminated (Cold Start)**: Process launch payload via `FirebaseMessaging.instance.getInitialMessage()`.
3. **Android Channels & iOS APNs**: Register notification channels in Android once at startup. Request runtime permissions contextually.
4. **Token Lifecycle**: Sync FCM token with backend on startup and post-login; delete/revoke token upon session logout.

---

## 2. Production Patterns

### 1. Background Message Handler
```dart
@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  WidgetsFlutterBinding.ensureInitialized();
  // Process background payload safely
}
```

### 2. Foreground & Navigation Routing
```dart
class NotificationService {
  final FirebaseMessaging _fcm;
  final FlutterLocalNotificationsPlugin _localNotifications;
  final AppRouter _router;

  NotificationService(this._fcm, this._localNotifications, this._router);

  Future<void> initialize() async {
    // 1. Request permission
    await _fcm.requestPermission(alert: true, badge: true, sound: true);

    // 2. Foreground message listener
    FirebaseMessaging.onMessage.listen((RemoteMessage message) {
      _showLocalNotification(message);
    });

    // 3. Background tap listener
    FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
      _handleNotificationNavigation(message.data);
    });
  }

  void _handleNotificationNavigation(Map<String, dynamic> data) {
    final route = data['route'] as String?;
    if (route != null) {
      _router.go(route);
    }
  }
}
```
