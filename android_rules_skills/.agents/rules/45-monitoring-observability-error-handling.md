# App Monitoring, Observability & Global Error Handling Rules

@../skills/android-monitoring-observability-error-handling/SKILL.md
@../skills/kotlin-compose-architect/references/03-architecture-decisions.md
@../skills/kotlin-compose-architect/references/07-security.md

## Monitoring & Telemetry Architecture Governance

1. **Single Source of Truth**:
   - All logging, Crashlytics reporting, analytics event tracking, and global error coordination must pass through the centralized monitoring interfaces: `AppLogger`, `CrashReporter`, `AnalyticsTracker`, and `GlobalErrorHandler`.
   - Direct calls to `android.util.Log`, `timber.log.Timber`, `FirebaseCrashlytics`, or `FirebaseAnalytics` from feature code (Activities, Composables, ViewModels, Repositories, UseCases) are **strictly forbidden**.

2. **Execution Timing & Benchmarking for Logs**:
   - Heavy operations (e.g. database transactions, repository sync, image/data batch processing, complex calculations) must be benchmarked using execution timers:
     ```kotlin
     val result = logger.timed(TAG, "operationName") {
         // critical operation
     }
     ```
   - For long-running asynchronous pipelines, use `val timer = logger.startTimer(TAG, "operationName")` and complete with `timer.stop()`.
   - Use `logger.logThrottled(...)` to suppress duplicate logcat and telemetry spam for rapid reconnect loops or repetitive event streams.

3. **Crashlytics Reporting & Issue Diagnosis**:
   - Non-fatal exceptions reported to Crashlytics must include rich, contextual diagnostic metadata: operation name, feature/screen tag, and request code.
   - Attach diagnostic breadcrumbs before recording non-fatal exceptions to preserve the user journey leading to an error.
   - **Distinguish Expected Failures from Defects**: Handled domain validation errors and expected HTTP 404/400 errors must **NEVER** be recorded as Crashlytics non-fatal crashes. Reserve Crashlytics for unexpected runtime exceptions, transport failures, and unhandled system bugs.

4. **Analytics Governance**:
   - All analytics events and screen views must route through `AnalyticsTracker`.
   - Event names must strictly follow lowercase `snake_case` naming conventions (e.g., `item_selected`, `order_submitted`).
   - Isolate analytics calls so analytics failures never compromise app stability or crash feature workflows.

5. **Global Error Handling & User Message Separation**:
   - Coordinate unexpected coroutine errors centrally via `GlobalErrorHandler.createCoroutineExceptionHandler()`.
   - Never leak raw stack traces, database exceptions, or technical server error messages to users.
   - User notifications must be displayed via `Messenger` using localized `UiText` (`Message.error(...)`) at the UI layer. Technical diagnostic context remains strictly inside `AppLogger` and `CrashReporter`.
   - Always preserve coroutine structured concurrency: **NEVER** catch or swallow `CancellationException`.

6. **Privacy & Sensitive Data Redaction**:
   - Authorization tokens, bearer headers, user passwords, credit card numbers, and raw PII (emails, phone numbers) must be sanitized via `DataSanitizer` before logging or dispatching to telemetry.
   - User identifiers passed to Crashlytics must be anonymized or hashed—never raw personal identifiers.
