---
name: android-monitoring-observability-error-handling
description: "Telemetry, logging, and error handling patterns"
trigger: always_on
alwaysApply: true
globs:
  - "**/*.kt"
---

# Android Centralized App Monitoring, Observability & Global Error Handling Skill

You are an expert Android monitoring, observability, telemetry, and error management architect.

## 1. Mission & Core Principles

Design and maintain a centralized, reusable architecture that manages all application logging, execution timing, Firebase Crashlytics, analytics event tracking, global error handling, and user messaging from a single, well-governed layer.

### Core Principles
1. **Single Source of Truth**: All logging, Crashlytics reporting, analytics tracking, and global error coordination must pass through dedicated centralized contracts (`AppLogger`, `CrashReporter`, `AnalyticsTracker`, `GlobalErrorHandler`). Direct calls to `Log`, `Timber`, `FirebaseCrashlytics`, or `FirebaseAnalytics` from feature code are strictly prohibited.
2. **Vendor Abstraction Behind Interfaces**: Keep third-party SDKs isolated behind clean domain/core interfaces so vendors can be replaced or mocked without modifying business logic.
3. **Execution Timing & Benchmarking for Logs**: Built-in stopwatch timers (`AppLogger.timed` and `startTimer`) measure operation durations in milliseconds, pinpointing slow network calls, heavy database transactions, and frame drops.
4. **Distinguish Expected Errors from Defects**: Do not report normal, expected domain/validation failures (e.g. invalid form input, 404 Not Found) as non-fatal crashes. Record non-fatal exceptions only for unexpected states, transport failures, and unhandled system exceptions.
5. **Decoupled User Messaging**: Never leak technical diagnostics or stack traces to end users. User-facing notifications must use localized `UiText` and `Messenger`, decoupled from logging and Crashlytics dispatch.
6. **Zero-Crash & Non-Blocking Guarantee**: Telemetry, logging, or analytics failures must **NEVER** crash the application or block critical execution paths. Every SDK call is safely isolated.
7. **Privacy & Redaction First**: Never log or transmit authentication tokens, passwords, credit card numbers, or personally identifiable information (PII).

---

## 2. Architecture & Logical Responsibilities

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        Feature / Presentation Layer                    │
│   Composables, ViewModels, UseCases, Repositories, Coroutines, OkHttp │
└───────────────┬───────────────────┬───────────────────┬────────────────┘
                │                   │                   │
                ▼                   ▼                   ▼
┌───────────────────────┐ ┌───────────────────┐ ┌────────────────────────┐
│       AppLogger       │ │   CrashReporter   │ │    AnalyticsTracker    │
│  - Verbose Console    │ │  - Non-fatals     │ │  - Standardized Events │
│  - Execution Timers   │ │  - Breadcrumbs    │ │  - Screen Views        │
│  - Log Throttling     │ │  - Custom Keys    │ │  - User Properties     │
└───────────┬───────────┘ └─────────┬─────────┘ └───────────┬────────────┘
            │                       │                       │
            └───────────────────────┼───────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          GlobalErrorHandler                            │
│   - Catches unhandled coroutine exceptions                             │
│   - Enriches diagnostic context (screen, request code, timing)        │
│   - Dispatches user-friendly UI notifications via Messenger + UiText   │
└────────────────────────────────────────────────────────────────────────┘
```

1. **`AppLogger`**: Unified logging interface providing debug/info/warn/error levels, execution timer benchmarking (`timed()`, `startTimer()`), and timer-based log throttling.
2. **`CrashReporter`**: Manages Crashlytics non-fatal exception recording, breadcrumb sequencing, and diagnostic key-value pairs.
3. **`AnalyticsTracker`**: Single gateway for tracking user actions, screen views, and conversion funnels with strict parameter sanitization.
4. **`GlobalErrorHandler`**: Centralized coordinator bridging unhandled exceptions, CoroutineExceptionHandlers, telemetry recording, and user-facing message dispatch (`Messenger`).
5. **`DataSanitizer`**: Utility that scrubs bearer tokens, secrets, credentials, and emails before telemetry dispatch.

---

## 3. Production Implementation Patterns

### 1. Centralized Logging with Execution Timers & Throttling

```kotlin
// In Feature, ViewModel, or Repository:
logger.d(TAG, "Initiating product catalog fetch")

// Benchmarking heavy operations with built-in execution timer:
val catalog = logger.timed(TAG, "loadCachedProducts") {
    productDao.getAllProducts()
}

// Manual start/stop timer for asynchronous pipelines:
val timer = logger.startTimer(TAG, "imageBatchProcessing")
// ... perform background work ...
timer.stop("processedCount = 42")

// Throttled logging to prevent rapid logcat/telemetry flood:
logger.logThrottled(
    tag = TAG,
    throttleKey = "reconnect_retry",
    message = "Socket retry attempt triggered",
    level = Log.WARN,
    throttleWindowMs = 2000L
)
```

---

### 2. Firebase Crashlytics & Enriched Diagnostic Reporting

When unexpected defects occur, enrich the crash report with contextual metadata before recording:

```kotlin
try {
    syncUserData()
} catch (e: Exception) {
    if (e is CancellationException) throw e

    // 1. Leave breadcrumb to recreate user journey
    crashReporter.logBreadcrumb("SyncUserData failed during checkout flow")

    // 2. Set contextual diagnostic keys
    crashReporter.setCustomKey("feature", "checkout")
    crashReporter.setCustomKey("cart_item_count", cart.size)

    // 3. Record non-fatal exception
    crashReporter.recordException(
        throwable = e,
        metadata = mapOf("operation" to "syncUserData", "network_type" to "wifi")
    )
}
```

---

### 3. Centralized Analytics Event Tracking

Standardize event names (lowercase snake_case) and enforce parameter sanitization:

```kotlin
// Track screen view navigation
analyticsTracker.trackScreen("checkout_confirmation_screen", "CheckoutActivity")

// Track conversion event with sanitized attributes
analyticsTracker.trackEvent(
    eventName = "purchase_completed",
    params = mapOf(
        "order_id" to order.id,
        "item_count" to order.items.size,
        "currency" to "USD",
        "value" to order.totalAmount
    )
)
```

---

### 4. Global Error Coordinator & UI Message Dispatching

Centralize unexpected error capture and decouple technical diagnostics from UI notifications:

```kotlin
class ProductViewModel @Inject constructor(
    private val globalErrorHandler: GlobalErrorHandler,
    private val productRepository: ProductRepository
) : ViewModel() {

    fun loadProducts() {
        val coroutineExceptionHandler = globalErrorHandler.createCoroutineExceptionHandler(TAG)

        viewModelScope.launch(coroutineExceptionHandler) {
            when (val result = productRepository.getProducts()) {
                is NetworkResult.Success -> { /* render state */ }
                is NetworkResult.Error -> {
                    // Handled domain error: notify user, DO NOT record as crash
                    globalErrorHandler.handleError(
                        throwable = IllegalStateException("API error: ${result.code}"),
                        contextTag = TAG,
                        userMessage = result.message,
                        requestCode = RequestCode.DATA_SYNC
                    )
                }
                is NetworkResult.Exception -> {
                    // Unexpected transport error: record non-fatal + notify user
                    globalErrorHandler.handleError(
                        throwable = result.throwable,
                        contextTag = TAG,
                        userMessage = UiText.res(R.string.generic_error_message),
                        requestCode = RequestCode.DATA_SYNC
                    )
                }
            }
        }
    }
}
```

---

## 4. Reusable Templates & Reference Implementations

The following production-ready templates are available in the `templates/` directory:

- [`templates/AppLogger.kt`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/android_rules_skills/.agents/skills/android-monitoring-observability-error-handling/templates/AppLogger.kt): Full logger contract, execution timing stopwatch, and throttle cache.
- [`templates/CrashReporter.kt`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/android_rules_skills/.agents/skills/android-monitoring-observability-error-handling/templates/CrashReporter.kt): Firebase Crashlytics abstraction with breadcrumbs and custom keys.
- [`templates/AnalyticsTracker.kt`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/android_rules_skills/.agents/skills/android-monitoring-observability-error-handling/templates/AnalyticsTracker.kt): Firebase Analytics contract with parameter scrubbers.
- [`templates/GlobalErrorHandler.kt`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/android_rules_skills/.agents/skills/android-monitoring-observability-error-handling/templates/GlobalErrorHandler.kt): Error coordinator connecting Coroutines, CrashReporter, and Messenger.
- [`templates/DataSanitizer.kt`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/android_rules_skills/.agents/skills/android-monitoring-observability-error-handling/templates/DataSanitizer.kt): Sensitive field and header redaction engine.
- [`templates/MonitoringModule.kt`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/android_rules_skills/.agents/skills/android-monitoring-observability-error-handling/templates/MonitoringModule.kt): Hilt DI module binding monitoring contracts.

---

## 5. Security & Sensitive Data Handling

1. **Header Redaction**: `Authorization`, `Cookie`, `X-Api-Key`, and bearer tokens must **NEVER** appear in logs or Crashlytics keys.
2. **User Identity Masking**: Never pass raw email addresses, full names, or phone numbers to `crashReporter.setUserId()` or analytics user properties. Use hashed or anonymized IDs.
3. **Payload Sanitization**: Request bodies, response bodies, and JSON strings passed to loggers must route through `DataSanitizer` to scrub credit cards, passwords, and tokens.

---

## 6. Code Review & Implementation Checklist

When reviewing or implementing monitoring and error handling, verify:

- [ ] **Single Source of Truth**: Feature code does not import `android.util.Log`, `timber.log.Timber`, `FirebaseCrashlytics`, or `FirebaseAnalytics`. All calls route via `AppLogger`, `CrashReporter`, `AnalyticsTracker`, or `GlobalErrorHandler`.
- [ ] **Execution Timers Used**: Heavy operations (database queries, network sync, data parsing) use `AppLogger.timed()` or `startTimer()` to benchmark elapsed duration.
- [ ] **Log Throttling Applied**: Repeated loop or reconnection logs use `logThrottled()` to prevent logcat and telemetry spam.
- [ ] **Non-Fatal vs Handled Error Distinction**: Validation errors and expected 404 responses are NOT logged to Crashlytics as non-fatal crashes.
- [ ] **CancellationException Preserved**: Coroutine cancellation exceptions are never caught and swallowed by monitoring or logging handlers.
- [ ] **No Raw Credentials**: `Authorization` tokens, passwords, and PII are redacted via `DataSanitizer`.
- [ ] **Release Safe**: Debug logs are suppressed in release builds; release errors only route through privacy-safe Crashlytics breadcrumbs.
- [ ] **Decoupled User Messaging**: Technical error messages are kept in telemetry; user-facing notifications use localized `UiText` via `Messenger`.

---

## 7. Required Final Report

When completing a monitoring and observability implementation task, report:
1. **Registered Contracts**: `AppLogger`, `CrashReporter`, `AnalyticsTracker`, `GlobalErrorHandler`.
2. **DI Modules Configured**: Bindings and provider configurations in `MonitoringModule`.
3. **Execution Benchmarking**: Operations instrumented with `AppLogger.timed` / `startTimer`.
4. **Error Routing Boundaries**: Distinction between handled domain errors, unexpected non-fatals, and `Messenger` notifications.
5. **Sanitization Rules**: Sensitive keys and regex redactions applied.
