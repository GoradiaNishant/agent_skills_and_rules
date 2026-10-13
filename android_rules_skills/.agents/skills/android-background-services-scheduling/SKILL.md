---
name: android-background-services-scheduling
description: >-
  Architectural decision framework, implementation patterns, and OS-compliance rules for Android
  background execution, Services (Foreground, Background, Bound), WorkManager, AlarmManager,
  JobScheduler, Coroutines/Flow, and BroadcastReceivers. Enforces battery efficiency, Doze compliance,
  process-death survival, and strict Android 12–15+ platform restrictions.
---

# Android Background Execution, Services & Scheduling Skill

You are an expert Android background execution, system architecture, and OS lifecycle specialist.

## 1. Mission & Core Principle

> **Core Principle: Select the simplest Android-supported mechanism that satisfies the requirement.**
> Do not automatically reach for a Service, WorkManager, or AlarmManager without analyzing timing guarantees, persistence, user awareness, and battery impact.

The goal is to ensure the AI correctly analyzes, selects, implements, and maintains Android background execution mechanisms according to the application's functional requirements, lifecycle, reliability needs, battery usage, and Android platform restrictions.

---

## 2. Requirement Analysis Checklist (Evaluate Before Choosing)

Before selecting or implementing any mechanism, analyze:

1. **Timing & Trigger**: Immediate, periodic, at a specific clock time, or reactive (event/broadcast)?
2. **Lifecycle Scope**: Must it continue while the app is visible, in the background, or after the user swipes the app away?
3. **Persistence**: Must the work survive process death, device reboot, or app update?
4. **User Awareness**: Does the user need continuous awareness (e.g. active GPS navigation, workout, audio playback requiring an ongoing notification)?
5. **Bidirectional UI Communication**: Does a UI screen need to interact with or control an active component?
6. **Execution Constraints**: Does it depend on unmetered Wi-Fi, battery charging, idle device, or device storage?
7. **Timing Precision**: Must execution occur at an exact minute/second, or is a flexible window acceptable?
8. **Idempotency & Replay**: Is the operation safe to retry multiple times upon network or process failure?
9. **OS Restrictions (Android 12–15+)**:
   - Background service launch restrictions.
   - Foreground service types (`foregroundServiceType`) and mandatory permissions.
   - Exact alarm permissions (`SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`).
   - `POST_NOTIFICATIONS` runtime permission.
   - Standby buckets, Doze mode, and OEM battery killer restrictions.

---

## 3. Decision Matrix

| Requirement | Preferred Mechanism | Non-Compliant / Anti-Pattern Alternatives |
|---|---|---|
| **UI-bound asynchronous task** (API call, DB fetch while screen is open) | Kotlin Coroutines (`viewModelScope`, `lifecycleScope`) | ❌ Started Service, WorkManager, `GlobalScope` |
| **Deferred, persistent synchronization** (Upload analytics, sync DB, offline mutations) | **WorkManager** (`OneTimeWorkRequest` with Constraints & UniqueWork) | ❌ Background Service, `Thread`, coroutine alone |
| **Periodic, deferrable synchronization** (Daily cache refresh, hourly sync) | **Periodic WorkManager** (`PeriodicWorkRequest`, min interval 15m) | ❌ Repeating AlarmManager, infinite Service with `while(true)` |
| **User-visible ongoing operation** (Turn-by-turn navigation, active workout, audio playback) | **Foreground Service** with explicit `foregroundServiceType` + ongoing notification | ❌ Normal background service, silent foreground service without valid type |
| **UI needs live RPC interaction with long-running component** | **Bound Service** (`Binder` / AIDL) | ❌ Storing Service instance in static/singleton variable |
| **User reminder / Calendar alarm at exact clock time** | **AlarmManager** (`setExactAndAllowWhileIdle`) | ❌ WorkManager (cannot guarantee exact clock timing) |
| **Quick reaction to system broadcast** (Connectivity change, boot complete) | **BroadcastReceiver** (< 10s, delegating long work to WorkManager) | ❌ Launching blocking coroutines or network calls in `onReceive` |
| **Immediate user-initiated file upload/download** | **WorkManager Expedited Work** or `DownloadManager` | ❌ Regular Service starting from background |
| **Server-originated trigger** (New chat message, remote invalidation) | **Firebase Cloud Messaging (FCM)** (High/Normal priority data payload) | ❌ Polling loop service, continuous socket in background |

---

## 4. Mechanisms & Architectural Patterns

### A. WorkManager (The Default for Deferrable Persistent Work)

WorkManager is the standard for deferrable, guaranteed background work that must survive process death and device restarts.

#### Standard Periodic / One-Time Worker (`@HiltWorker`)
```kotlin
// feature/sync/worker/SyncDataWorker.kt
@HiltWorker
class SyncDataWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val messenger: Messenger
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val syncSuccess = syncRepository.synchronizeData()
            if (syncSuccess) {
                Result.success()
            } else {
                // Retry with exponential backoff if transient failure
                if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }
        } catch (e: CancellationException) {
            throw e // Preserve coroutine cancellation
        } catch (e: IOException) {
            // Network/IO failure: retry later when constraints allow
            Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "sync_data_worker_unique"
        private const val MAX_RETRIES = 3

        fun enqueue(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncDataWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            // KEEP or REPLACE to prevent duplicate jobs
            workManager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
```

#### WorkManager Hard Rules:
1. **Periodic Work Minimum**: `PeriodicWorkRequest` minimum interval is **15 minutes**. It is inexact by design and subject to Doze.
2. **Unique Work**: Always use `enqueueUniqueWork` / `enqueueUniquePeriodicWork` (`KEEP` or `REPLACE`) to eliminate duplicate parallel executions.
3. **Expedited Work**: Use `setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)` only for time-sensitive, user-initiated work.
4. **Idempotency**: All workers must be idempotent—re-running the worker after partial completion must not corrupt database state.

---

### B. Foreground Service (User-Visible Ongoing Operations)

Use **only** when the user actively expects the app to remain running independently of the UI and the task is visible (audio, GPS workout, active call, file transfer).

#### Requirements (Android 14+ / API 34 Compliance):
1. **Declare Type**: Manifest must specify `android:foregroundServiceType` (e.g., `location`, `mediaPlayback`, `dataSync`, `connectedDevice`).
2. **Declare Permissions**: Declare both `<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />` AND the type-specific permission (e.g., `FOREGROUND_SERVICE_LOCATION`).
3. **Start Deadline**: Call `startForeground()` within **5 seconds** of `onStartCommand()`, or Android will crash the app with `ForegroundServiceDidNotStartInTimeException`.
4. **Stop Promptly**: Call `stopForeground(STOP_FOREGROUND_REMOVE)` and `stopSelf()` immediately once the user stops the action.

```kotlin
// feature/tracking/service/LocationTrackingService.kt
@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject lateinit var notificationHelper: NotificationHelper
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notificationHelper.createTrackingNotification()

        // Android 14+ requires passing foregroundServiceType in ServiceCompat
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )

        startTracking()
        return START_NOT_STICKY
    }

    private fun startTracking() {
        serviceScope.launch {
            // Perform location collection
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
```

---

### C. AlarmManager (Exact & Inexact Clock-Based Scheduling)

Use AlarmManager **only** when execution must be pinned to a specific clock time (reminders, calendar alerts).

#### Rules for Exact Alarms:
1. **Modern Restrictions (Android 12–14+)**:
   - `SCHEDULE_EXACT_ALARM` can be revoked by the user or restricted by the OS.
   - Always check `alarmManager.canScheduleExactAlarms()` before calling `setExactAndAllowWhileIdle()`.
   - Fall back to inexact alarms or redirect user to system settings if exact alarm permission is denied.
2. **PendingIntent Flag**: Always include `PendingIntent.FLAG_IMMUTABLE` (or `FLAG_MUTABLE` if needed for notifications).
3. **Device Reboot**: Alarms are cleared on device reboot. Must register a `BOOT_COMPLETED` receiver to reschedule active user alarms if persistence across reboots is required.

```kotlin
// core/scheduler/AlarmScheduler.kt
class AlarmScheduler @Inject constructor(
    private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(triggerAtMillis: Long, reminderId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("EXTRA_REMINDER_ID", reminderId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                // Fallback to inexact or handle permission request
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}
```

---

### D. Bound Service (Client-Service RPC Interaction)

Use when an Activity, Fragment, or component needs an ongoing two-way interface with a local service:

```kotlin
// feature/player/service/PlaybackBoundService.kt
class PlaybackBoundService : Service() {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackBoundService = this@PlaybackBoundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun playTrack(trackId: String) { /* player logic */ }
    fun pauseTrack() { /* player logic */ }
}
```

In the UI / ViewModel component:
- Call `bindService(intent, connection, Context.BIND_AUTO_CREATE)` on start.
- **Always unbind** in `onStop()` / `onDestroy()` via `unbindService(connection)` to prevent service and context leaks.

---

### E. BroadcastReceivers

- **Execution Limit**: `onReceive()` runs on the main thread and has a strict **10-second** timeout before ANR.
- **No Blocking Operations**: Never perform network requests, disk I/O, or blocking coroutines inside `onReceive()`.
- **Handoff**: For work lasting longer than a few milliseconds, hand off execution to WorkManager:
  ```kotlin
  class BootReceiver : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
          if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
              val workManager = WorkManager.getInstance(context)
              RescheduleAlarmsWorker.enqueue(workManager)
          }
      }
  }
  ```

---

## 5. Architectural & Clean Code Rules

1. **Keep Framework Schedulers Out of the Domain Layer**:
   - The domain layer defines business interfaces (`interface SyncScheduler { fun scheduleSync() }`).
   - The data/framework layer implements it using WorkManager (`class WorkManagerSyncScheduler @Inject constructor(...) : SyncScheduler`).
2. **Never Keep Processes Alive Unnecessarily**:
   - Never run an infinite loop in a service.
   - Never abuse Foreground Services to bypass background limitations.
3. **Account for Process Death**:
   - Background threads, Coroutines, and static variables die when Android kills the process in the background.
   - Persist necessary state in Room SQLite or DataStore before background execution starts.

---

## 6. Manifest & Permissions Governance

Ensure the AndroidManifest.xml matches the selected mechanism:

```xml
<!-- 1. Foreground Service Permissions (Declare specific types for Android 14+) -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

<!-- 2. Notifications (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- 3. Exact Alarm (Android 12+) -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

<!-- 4. Boot Receiver -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Service Declaration with explicit type -->
<service
    android:name=".feature.tracking.LocationTrackingService"
    android:foregroundServiceType="location"
    android:exported="false" />

<!-- Receiver Declaration -->
<receiver
    android:name=".core.receiver.BootReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

---

## 7. Review & Verification Checklist

When reviewing background execution code, verify:
- [ ] **Simplest Mechanism Used**: A full Service was not introduced when a Coroutine in `viewModelScope` or WorkManager would suffice.
- [ ] **WorkManager Uniqueness**: All recurring or one-time workers use `enqueueUniqueWork` to prevent duplicate concurrent jobs.
- [ ] **Foreground Service Type Declared**: Every Foreground Service declares `android:foregroundServiceType` in manifest and passes type in `startForeground()`.
- [ ] **Foreground Deadline**: `startForeground()` is called within 5 seconds of service launch.
- [ ] **Exact Alarm Guard**: `canScheduleExactAlarms()` is checked before invoking exact alarms on Android 12+.
- [ ] **BroadcastReceiver Non-Blocking**: `onReceive()` contains no database/network operations and delegates work to WorkManager.
- [ ] **Process Death Survival**: Deferrable tasks rely on persistent storage + WorkManager, NOT unmanaged background Coroutine scopes.
- [ ] **Proper Unbinding**: Bound Services are safely unbound in `onStop()` / `onDestroy()`.
- [ ] **Battery & Doze Respect**: Work constraints (unmetered network, battery not low) are applied to background jobs.
