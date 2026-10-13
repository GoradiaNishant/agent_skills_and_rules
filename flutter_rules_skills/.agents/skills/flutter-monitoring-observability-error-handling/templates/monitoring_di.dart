import 'package:injectable/injectable.dart';
import 'analytics_tracker.dart';
import 'app_logger.dart';
import 'crash_reporter.dart';
import 'global_error_handler.dart';

@module
abstract class MonitoringModule {
  @lazySingleton
  AppLogger get logger => AppLoggerImpl();

  @lazySingleton
  CrashReporter get crashReporter => FirebaseCrashReporter();

  @lazySingleton
  AnalyticsTracker get analyticsTracker => FirebaseAnalyticsTracker();

  @lazySingleton
  GlobalErrorHandler bindGlobalErrorHandler(GlobalErrorHandlerImpl impl) => impl;
}
