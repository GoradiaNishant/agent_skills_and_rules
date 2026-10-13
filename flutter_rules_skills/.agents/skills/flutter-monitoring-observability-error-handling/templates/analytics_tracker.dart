import 'package:firebase_analytics/firebase_analytics.dart';
import 'package:injectable/injectable.dart';
import 'data_sanitizer.dart';

abstract interface class AnalyticsTracker {
  Future<void> trackEvent(String eventName, [Map<String, Object?> parameters = const {}]);
  Future<void> trackScreen(String screenName, [String? screenClass]);
  Future<void> setUserProperty(String name, String? value);
  Future<void> setAnalyticsCollectionEnabled(bool enabled);
}

@singleton
class FirebaseAnalyticsTracker implements AnalyticsTracker {
  FirebaseAnalytics get _analytics => FirebaseAnalytics.instance;

  @override
  Future<void> trackEvent(String eventName, [Map<String, Object?> parameters = const {}]) async {
    try {
      final sanitized = DataSanitizer.sanitizeMap(parameters);
      await _analytics.logEvent(
        name: eventName.toLowerCase(),
        parameters: sanitized,
      );
    } catch (_) {}
  }

  @override
  Future<void> trackScreen(String screenName, [String? screenClass]) async {
    try {
      await _analytics.logScreenView(
        screenName: screenName,
        screenClass: screenClass,
      );
    } catch (_) {}
  }

  @override
  Future<void> setUserProperty(String name, String? value) async {
    try {
      await _analytics.setUserProperty(
        name: name.toLowerCase(),
        value: value != null ? DataSanitizer.sanitizeText(value) : null,
      );
    } catch (_) {}
  }

  @override
  Future<void> setAnalyticsCollectionEnabled(bool enabled) async {
    try {
      await _analytics.setAnalyticsCollectionEnabled(enabled);
    } catch (_) {}
  }
}
