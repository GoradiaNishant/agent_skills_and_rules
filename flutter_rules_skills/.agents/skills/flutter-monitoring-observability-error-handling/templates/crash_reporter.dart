import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:injectable/injectable.dart';
import 'data_sanitizer.dart';

abstract interface class CrashReporter {
  void logBreadcrumb(String message);
  void setCustomKey(String key, Object value);
  void setUserId(String? userId);
  void recordError(
    Object exception,
    StackTrace? stack, {
    Map<String, Object?> metadata = const {},
    bool fatal = false,
  });
}

@singleton
class FirebaseCrashReporter implements CrashReporter {
  FirebaseCrashlytics get _crashlytics => FirebaseCrashlytics.instance;

  @override
  void logBreadcrumb(String message) {
    try {
      _crashlytics.log(DataSanitizer.sanitizeText(message));
    } catch (_) {}
  }

  @override
  void setCustomKey(String key, Object value) {
    try {
      final sanitized = DataSanitizer.sanitizeEntry(key, value);
      if (sanitized == null) return;
      if (sanitized is String) {
        _crashlytics.setCustomKey(key, sanitized);
      } else if (sanitized is num) {
        _crashlytics.setCustomKey(key, sanitized);
      } else if (sanitized is bool) {
        _crashlytics.setCustomKey(key, sanitized);
      } else {
        _crashlytics.setCustomKey(key, sanitized.toString());
      }
    } catch (_) {}
  }

  @override
  void setUserId(String? userId) {
    try {
      _crashlytics.setUserIdentifier(
        userId != null ? DataSanitizer.sanitizeText(userId) : '',
      );
    } catch (_) {}
  }

  @override
  void recordError(
    Object exception,
    StackTrace? stack, {
    Map<String, Object?> metadata = const {},
    bool fatal = false,
  }) {
    try {
      final sanitizedMetadata = DataSanitizer.sanitizeMap(metadata);
      sanitizedMetadata.forEach((k, v) {
        if (v != null) setCustomKey(k, v);
      });
      _crashlytics.recordError(exception, stack, fatal: fatal);
    } catch (_) {}
  }
}
