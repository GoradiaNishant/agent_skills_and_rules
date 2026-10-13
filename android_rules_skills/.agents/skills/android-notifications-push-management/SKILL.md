---
name: android-notifications-push-management
description: >-
  Comprehensive architecture, implementation patterns, and OS-compliance rules for Android local
  notifications, remote push notifications (FCM), notification channels, runtime permissions (Android 13+),
  deep link navigation, lock-screen privacy, and background/cold-start delivery lifecycle.
---

# Android Notification Architecture, Push Notifications & Notification Management Skill

You are an expert Android notification architecture, messaging systems, and OS lifecycle specialist.

## 1. Mission & Core Principle

> **Core Principle: Treat notifications as a complete application feature with defined delivery, presentation, interaction, navigation, security, and lifecycle behavior — not merely as isolated calls to `NotificationManager.notify()`.**

The goal is to design, implement, and maintain a production-ready notification system covering local notifications, remote push notifications (Firebase Cloud Messaging / FCM), notification channels, runtime permissions, foreground/background delivery, notification actions, type-safe deep links, notification state management, and Android OS compatibility.

---

## 2. Notification Types & Trigger Classification

Classify each notification requirement before choosing an implementation:

| Notification Type | Trigger & Source | Best-Suited Use Cases | Mechanism |
|---|---|---|---|
| **Local Notifications** | In-app event, local database, or local timer | Scheduled reminders, task completion alerts, local status alerts | `NotificationManagerCompat` + `AlarmManager` / `WorkManager` |
| **Remote Push (Data Payload)** | Server-initiated via FCM data message | Chat messages, order status updates, silent cache invalidation | `FirebaseMessagingService.onMessageReceived()` |
| **Remote Push (Notification Payload)** | Server-initiated via FCM notification message | Promotional broadcasts, general marketing announcements | Handled by Android System tray in background; `onMessageReceived()` in foreground |
| **In-App Messaging** | Event arriving while app is actively foregrounded | Toast, Snackbar, or in-app Banner | `Messenger` / in-app UI state (suppress system tray spam) |
| **Ongoing Notifications** | Active foreground operation | Media playback, turn-by-turn navigation, active workout tracking | `ForegroundService` + ongoing non-dismissible notification |

---

## 3. Notification Channels & Categories (Android 8.0+ / API 26+)

Notification channels must be defined centrally with stable IDs, clear user-visible names, and descriptions.

```kotlin
// core/notification/NotificationChannelManager.kt
object NotificationChannels {
    const val CHANNEL_CRITICAL_ALERTS = "channel_critical_alerts"
    const val CHANNEL_MESSAGES = "channel_messages"
    const val CHANNEL_REMINDERS = "channel_reminders"
    const val CHANNEL_UPDATES = "channel_general_updates"

    fun createChannels(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)

        val criticalChannel = NotificationChannelCompat.Builder(
            CHANNEL_CRITICAL_ALERTS,
            NotificationManagerCompat.IMPORTANCE_HIGH
        )
            .setName(context.getString(R.string.channel_critical_name))
            .setDescription(context.getString(R.string.channel_critical_description))
            .setVibrationEnabled(true)
            .setShowBadge(true)
            .build()

        val messagesChannel = NotificationChannelCompat.Builder(
            CHANNEL_MESSAGES,
            NotificationManagerCompat.IMPORTANCE_HIGH
        )
            .setName(context.getString(R.string.channel_messages_name))
            .setDescription(context.getString(R.string.channel_messages_description))
            .setShowBadge(true)
            .build()

        val remindersChannel = NotificationChannelCompat.Builder(
            CHANNEL_REMINDERS,
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName(context.getString(R.string.channel_reminders_name))
            .setDescription(context.getString(R.string.channel_reminders_description))
            .build()

        val updatesChannel = NotificationChannelCompat.Builder(
            CHANNEL_UPDATES,
            NotificationManagerCompat.IMPORTANCE_LOW
        )
            .setName(context.getString(R.string.channel_updates_name))
            .setDescription(context.getString(R.string.channel_updates_description))
            .build()

        notificationManager.createNotificationChannelsCompat(
            listOf(criticalChannel, messagesChannel, remindersChannel, updatesChannel)
        )
    }
}
```

### Channel Hard Rules:
1. **Never Recreate on Every Launch**: Use stable IDs and register once in `Application.onCreate()`.
2. **Respect User Customization**: Once created, Android user settings take precedence. Changing importance or sound in code does NOT override the user's manual adjustments.
3. **Channel Categories**: Keep the total channel count minimal (3–5 meaningful categories) so users have clear, fine-grained control.

---

## 4. Runtime Notification Permissions (Android 13+ / API 33+)

Starting in Android 13, posting notifications requires the runtime permission `android.permission.POST_NOTIFICATIONS`.

### Manifest Declaration
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### Contextual Permission Request Flow
- **Never prompt immediately on cold launch**: Explain the concrete user benefit first in an onboarding screen or contextually when the user enables a reminder/notification feature.
- **Graceful Degradation**: If permission is denied, fallback gracefully. Treat disabled notifications as a supported state—never crash or block core app functionality.

```kotlin
// feature/notification/permission/NotificationPermissionHandler.kt
class NotificationPermissionHandler @Inject constructor(
    private val context: Context
) {
    fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun isChannelEnabled(channelId: String): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val channel = manager.getNotificationChannelCompat(channelId) ?: return true
        return channel.importance != NotificationManagerCompat.IMPORTANCE_NONE
    }
}
```

---

## 5. Firebase Cloud Messaging (FCM) Integration

### A. Message Type Distinctions (Critical Android Behavior)
1. **Data Message (`"data": { ... }`)**:
   - Delivered to `onMessageReceived()` in **all** application states (Foreground, Background, and Terminated/Killed).
   - Gives the app complete control over local presentation, parsing, and navigation routing.
   - **Recommended for all production business events.**
2. **Notification Message (`"notification": { "title": "...", "body": "..." }`)**:
   - If app is in **Background/Terminated**: The Android System Tray renders the notification automatically using default launcher intent. `onMessageReceived()` is **BYPASSED**.
   - If app is in **Foreground**: `onMessageReceived()` is invoked.

### B. Production `FirebaseMessagingService` Implementation
```kotlin
// core/notification/fcm/AppFirebaseMessagingService.kt
@AndroidEntryPoint
class AppFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var tokenSyncCoordinator: FcmTokenSyncCoordinator
    @Inject lateinit var pushNotificationManager: PushNotificationManager
    @Inject lateinit var inAppMessenger: Messenger

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Hand off token rotation to persistent sync coordinator
        tokenSyncCoordinator.onTokenRefreshed(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. Parse and validate raw data payload safely
        val payload = PushPayloadParser.parse(remoteMessage.data) ?: return

        // 2. Determine presentation based on foreground/background state
        if (isAppInForeground()) {
            // Deliver in-app banner/snackbar to active screen without polluting system tray
            inAppMessenger.deliver(
                Message.info(
                    content = UiText.str(payload.title ?: payload.body),
                    actionLabel = UiText.res(R.string.action_view),
                    onAction = { /* navigate */ }
                )
            )
        } else {
            // Post structured system notification
            pushNotificationManager.displayNotification(payload)
        }
    }

    private fun isAppInForeground(): Boolean {
        // Evaluate ProcessLifecycleOwner or activity tracker
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    }
}
```

### C. FCM Token Synchronization & Lifecycle
- **Never hardcode or treat an FCM token as an authentication credential.**
- Synchronize token on:
  1. `onNewToken()` callback.
  2. Successful user login (associating device token with user session ID).
  3. Logout (unregistering device token from backend to prevent cross-account push leakage).
- Use `WorkManager` to retry token registration with exponential backoff if the network fails during `onNewToken()`.

---

## 6. Structured Payload Design & Safe Parsing

Never access raw `remoteMessage.data["key"]` haphazardly across UI or repository code. Define a strict data model:

```kotlin
// core/notification/model/PushPayload.kt
@Serializable
data class PushPayload(
    @SerialName("type") val type: String? = null,
    @SerialName("entity_id") val entityId: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("deep_link") val deepLink: String? = null,
    @SerialName("schema_version") val schemaVersion: Int? = 1
)

object PushPayloadParser {
    fun parse(data: Map<String, String>): PushPayload? {
        if (data.isEmpty()) return null
        return runCatching {
            val jsonString = Json.encodeToString(data)
            Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromString<PushPayload>(jsonString)
        }.getOrNull()
    }
}
```

---

## 7. Deep Linking & Cold-Start Navigation

Notification taps must reliably navigate to the target screen—whether the app is closed (cold start) or already running.

### PendingIntent Construction with `TaskStackBuilder`
Always use `TaskStackBuilder` so pressing the system Back button navigates to the app's parent screen rather than exiting to the launcher home screen:

```kotlin
// core/notification/NotificationIntentFactory.kt
object NotificationIntentFactory {

    fun createPendingIntent(
        context: Context,
        notificationId: Int,
        destinationUri: Uri
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = destinationUri
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(
                notificationId,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )!!
        }
    }
}
```

### Handling Navigation in Jetpack Compose
In `MainActivity.kt` and `RootAppNavHost`:
- Capture incoming deep-link URIs in `onCreate(savedInstanceState)` and `onNewIntent(intent)`.
- **Validation Gate**: Validate the destination URI and authenticate the user before executing the route. If the session is unauthenticated, redirect to the Auth flow and preserve the destination pending post-login redirection.

---

## 8. Notification Builder & Presentation Standards

```kotlin
// core/notification/PushNotificationManager.kt
@Singleton
class PushNotificationManager @Inject constructor(
    private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    fun displayNotification(payload: PushPayload) {
        val channelId = when (payload.type) {
            "CRITICAL_ALERT" -> NotificationChannels.CHANNEL_CRITICAL_ALERTS
            "CHAT_MESSAGE" -> NotificationChannels.CHANNEL_MESSAGES
            "REMINDER" -> NotificationChannels.CHANNEL_REMINDERS
            else -> NotificationChannels.CHANNEL_UPDATES
        }

        val notificationId = payload.entityId?.hashCode() ?: System.currentTimeMillis().toInt()
        val deepLinkUri = payload.deepLink?.toUri() ?: "app://home".toUri()
        val pendingIntent = NotificationIntentFactory.createPendingIntent(context, notificationId, deepLinkUri)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Must be monochrome stencil
            .setContentTitle(payload.title ?: context.getString(R.string.app_name))
            .setContentText(payload.body.orEmpty())
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE) // Lock screen privacy

        // Support BigTextStyle for expanded text
        payload.body?.let {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(it))
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, builder.build())
        }
    }
}
```

---

## 9. Notification Actions (Direct Reply & Quick Actions)

When providing action buttons on notifications:
- Use explicit BroadcastReceivers (`NotificationActionReceiver`) with `PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE`.
- Authenticate and authorize protected actions.
- Perform network operations through the centralized `NetworkResult` pipeline; never initiate raw unauthenticated HTTP requests.
- Dismiss or update the active notification immediately upon action trigger.

```kotlin
// core/notification/action/NotificationActionReceiver.kt
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var orderRepository: OrderRepository
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val orderId = intent.getStringExtra("EXTRA_ORDER_ID") ?: return

        if (action == ACTION_CONFIRM_ORDER) {
            // Dismiss notification immediately for responsive UX
            NotificationManagerCompat.from(context).cancel(orderId.hashCode())

            scope.launch {
                orderRepository.confirmOrder(orderId)
            }
        }
    }

    companion object {
        const val ACTION_CONFIRM_ORDER = "com.app.action.CONFIRM_ORDER"
    }
}
```

---

## 10. Security & Lock-Screen Privacy

1. **Lock-Screen Visibility**: Always set `NotificationCompat.VISIBILITY_PRIVATE` for notifications containing personal, financial, or account data.
2. **Never Put Credentials in Payloads**: Do not send access tokens, passwords, or full credit card numbers in FCM push payloads.
3. **Session Cleared on Logout**: Upon logout, unregister the device FCM token on the backend and dismiss all active app notifications via `NotificationManagerCompat.cancelAll()`.

---

## 11. Review & Verification Checklist

When reviewing notification code, verify:
- [ ] **Channels Registered Once**: Channels registered centrally in `Application.onCreate()` with stable IDs.
- [ ] **Android 13+ Runtime Permission**: `POST_NOTIFICATIONS` requested contextually with graceful denial fallback.
- [ ] **Data Payload Priority**: Remote push uses FCM `data` messages to ensure reliable background execution in `onMessageReceived()`.
- [ ] **Foreground Suppression**: App does not spam the system tray with notifications when the relevant screen is already open.
- [ ] **PendingIntent Immutability**: All Notification PendingIntents specify `PendingIntent.FLAG_IMMUTABLE`.
- [ ] **Backstack Preservation**: `TaskStackBuilder` used so tapping a deep-link notification preserves the parent navigation backstack.
- [ ] **Token Rotation & Logout**: FCM token is synchronized on login and revoked on logout.
- [ ] **Lock-Screen Privacy**: Sensitive notifications use `VISIBILITY_PRIVATE`.
- [ ] **Monochrome Small Icon**: Small notification icon is a valid monochrome stencil drawable.
