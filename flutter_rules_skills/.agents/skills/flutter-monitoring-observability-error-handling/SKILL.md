---
name: flutter-monitoring-observability-error-handling
description: >-
  Centralized application monitoring, structured logging with execution timers, Firebase Crashlytics,
  analytics tracking, sensitive data redaction, and global error handling in Flutter projects.
  Use when designing, reviewing, or integrating telemetry, logging, error handling, or analytics.
---

# Flutter Centralized App Monitoring, Observability & Global Error Handling Skill

You are an expert Flutter monitoring, observability, telemetry, and error management architect.

## 1. Mission & Core Principles

Design and maintain a centralized, reusable architecture that manages all application logging, execution timing, Firebase Crashlytics, analytics event tracking, global error handling, and user messaging from a single, well-governed layer.

### Core Principles
1. **Single Source of Truth**: All logging, Crashlytics reporting, analytics tracking, and global error coordination must pass through dedicated centralized contracts (`AppLogger`, `CrashReporter`, `AnalyticsTracker`, `GlobalErrorHandler`). Direct calls to `print`, `developer.log`, `FirebaseCrashlytics`, or `FirebaseAnalytics` from feature code are strictly prohibited.
2. **Vendor Abstraction Behind Interfaces**: Keep third-party SDKs isolated behind clean interfaces so vendors can be replaced or mocked without modifying business logic.
3. **Execution Timing & Benchmarking for Logs**: Built-in stopwatch timers (`AppLogger.timed` and `startTimer`) measure operation durations in milliseconds, pinpointing slow network calls, heavy database transactions, and frame drops.
4. **Distinguish Expected Errors from Defects**: Do not report normal, expected domain/validation failures (e.g. invalid form input, 404 Not Found) as non-fatal crashes. Record non-fatal exceptions only for unexpected states, transport failures, and unhandled system exceptions.
5. **Decoupled User Messaging**: Never leak technical diagnostics or stack traces to end users. User-facing notifications must use localized `UiText` and `Messenger`, decoupled from logging and Crashlytics dispatch.
6. **Zero-Crash & Non-Blocking Guarantee**: Telemetry, logging, or analytics failures must **NEVER** crash the application or block critical execution paths. Every SDK call is safely isolated.
7. **Privacy & Redaction First**: Never log or transmit authentication tokens, passwords, credit card numbers, or personally identifiable information (PII).

---

## 2. Production Patterns

### 1. Centralized Logging with Execution Timers & Throttling
```dart
// In Feature, BLoC, or Repository:
logger.d('ProductRepo', 'Initiating product catalog fetch');

// Benchmarking heavy operations with built-in execution timer:
final catalog = logger.timed('ProductRepo', 'loadCachedProducts', () {
  return productLocalSource.getAllProducts();
});

// Manual start/stop timer for asynchronous pipelines:
final timer = logger.startTimer('ImageProcessor', 'batchResize');
// ... perform background work ...
timer.stop('processedCount = 42');

// Throttled logging to prevent rapid logcat/telemetry flood:
logger.logThrottled(
  'SocketManager',
  'reconnect_retry',
  'Socket retry attempt triggered',
  throttleWindowMs: 2000,
);
```

### 2. Firebase Crashlytics & Enriched Diagnostic Reporting
```dart
try {
  await syncUserData();
} catch (e, stack) {
  // 1. Leave breadcrumb to recreate user journey
  crashReporter.logBreadcrumb('SyncUserData failed during checkout flow');

  // 2. Set contextual diagnostic keys
  crashReporter.setCustomKey('feature', 'checkout');
  crashReporter.setCustomKey('cart_item_count', cart.length);

  // 3. Record non-fatal exception
  crashReporter.recordError(
    e,
    stack,
    metadata: {'operation': 'syncUserData', 'network_type': 'wifi'},
  );
}
```

### 3. Framework-Wide Error Trapping
```dart
void main() {
  WidgetsFlutterBinding.ensureInitialized();
  
  final errorHandler = getIt<GlobalErrorHandler>();
  errorHandler.initialize(); // Hooks FlutterError.onError & PlatformDispatcher.onError
  
  runApp(const MyApp());
}
```

---

## 3. Reusable Templates & Reference Implementations

The following production-ready templates are available in `templates/`:
- [`templates/app_logger.dart`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/flutter_rules_skills/.agents/skills/flutter-monitoring-observability-error-handling/templates/app_logger.dart): Full logger contract, stopwatch execution timers, and throttle cache.
- [`templates/crash_reporter.dart`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/flutter_rules_skills/.agents/skills/flutter-monitoring-observability-error-handling/templates/crash_reporter.dart): Firebase Crashlytics abstraction with breadcrumbs and custom keys.
- [`templates/analytics_tracker.dart`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/flutter_rules_skills/.agents/skills/flutter-monitoring-observability-error-handling/templates/analytics_tracker.dart): Firebase Analytics contract with parameter scrubbers.
- [`templates/global_error_handler.dart`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/flutter_rules_skills/.agents/skills/flutter-monitoring-observability-error-handling/templates/global_error_handler.dart): Error coordinator connecting FlutterError, PlatformDispatcher, and Messenger.
- [`templates/data_sanitizer.dart`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/flutter_rules_skills/.agents/skills/flutter-monitoring-observability-error-handling/templates/data_sanitizer.dart): Sensitive field and header redaction engine.
- [`templates/monitoring_di.dart`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/flutter_rules_skills/.agents/skills/flutter-monitoring-observability-error-handling/templates/monitoring_di.dart): Injectable DI module.
