# Background Execution, Services & Scheduling Rules

@../skills/android-background-services-scheduling/SKILL.md
@../skills/kotlin-compose-architect/references/03-architecture-decisions.md

## Core Selection & Execution Rules

1. **Principle of Minimum Sufficient Power**:
   - Always choose the simplest platform-supported mechanism for background work.
   - For UI-bound work while the screen is open: Use Kotlin Coroutines in `viewModelScope` or `lifecycleScope`.
   - For persistent, deferrable background sync: Use **WorkManager**.
   - For user-visible ongoing tasks (audio playback, active GPS navigation): Use **Foreground Service**.
   - For exact clock alarms (reminders): Use **AlarmManager**.
   - **NEVER** launch a full Android Service for an ordinary API call or database query.

2. **WorkManager Standards**:
   - Background workers must use `@HiltWorker` and `@AssistedInject`.
   - All worker enqueues must use unique work policies (`enqueueUniqueWork` or `enqueueUniquePeriodicWork`) to avoid duplicate execution.
   - Workers must be idempotent: design them so retrying after a mid-execution crash never causes duplicate or corrupted data.
   - Set network, battery, and storage constraints on WorkRequests to respect Android battery and Doze limits.

3. **Foreground Service Governance**:
   - Must specify `android:foregroundServiceType` in the manifest and pass the type to `ServiceCompat.startForeground()`.
   - Must call `startForeground()` within **5 seconds** of `onStartCommand()` to prevent OS ANR crashes.
   - Must stop promptly with `stopForeground(STOP_FOREGROUND_REMOVE)` and `stopSelf()` when the task finishes.
   - Must never be used as a loophole to keep an app process alive indefinitely.

4. **AlarmManager Rules**:
   - Always verify `alarmManager.canScheduleExactAlarms()` on Android 12+ before scheduling exact alarms. Fall back to inexact alarms or prompt the user.
   - Always specify `PendingIntent.FLAG_IMMUTABLE` (or `FLAG_MUTABLE` if explicitly required).
   - If alarms must survive reboot, register a `BOOT_COMPLETED` receiver that delegates rescheduling to WorkManager.

5. **BroadcastReceivers**:
   - Keep `onReceive()` execution under a few milliseconds.
   - Never run blocking coroutines, network calls, or heavy disk I/O in `onReceive()`. Delegate work lasting longer than a quick flag check to WorkManager.
