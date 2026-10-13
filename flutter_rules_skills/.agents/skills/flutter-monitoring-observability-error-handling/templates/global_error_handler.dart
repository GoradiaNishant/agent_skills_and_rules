import 'package:flutter/foundation.dart';
import 'package:injectable/injectable.dart';
import '../../flutter-network-session-management/templates/message.dart';
import '../../flutter-network-session-management/templates/messenger.dart';
import '../../flutter-network-session-management/templates/ui_text.dart';
import 'app_logger.dart';
import 'crash_reporter.dart';

abstract interface class GlobalErrorHandler {
  void initialize();
  void handleError(
    Object error,
    StackTrace? stack, {
    required String contextTag,
    UiText? userMessage,
    RequestCode requestCode = RequestCode.unknown,
  });
}

@singleton
class GlobalErrorHandlerImpl implements GlobalErrorHandler {
  final AppLogger _logger;
  final CrashReporter _crashReporter;
  final Messenger _messenger;

  GlobalErrorHandlerImpl(this._logger, this._crashReporter, this._messenger);

  @override
  void initialize() {
    // 1. Framework & Widget build errors
    FlutterError.onError = (FlutterErrorDetails details) {
      _logger.e('FlutterError', details.exceptionAsString(), details.exception, details.stack);
      _crashReporter.recordError(details.exception, details.stack, fatal: false);
    };

    // 2. Unhandled asynchronous zone / isolate errors
    PlatformDispatcher.instance.onError = (Object error, StackTrace stack) {
      _logger.e('PlatformDispatcher', error.toString(), error, stack);
      _crashReporter.recordError(error, stack, fatal: true);
      return true; // Mark as handled
    };
  }

  @override
  void handleError(
    Object error,
    StackTrace? stack, {
    required String contextTag,
    UiText? userMessage,
    RequestCode requestCode = RequestCode.unknown,
  }) {
    // Log technical error with throttle protection
    _logger.logThrottled(
      contextTag,
      '${contextTag}_${error.runtimeType}',
      'Error in [$contextTag]: $error',
    );

    // Record non-fatal exception in telemetry
    _crashReporter.logBreadcrumb('Error in context: $contextTag | ${error.runtimeType}');
    _crashReporter.recordError(
      error,
      stack,
      metadata: {
        'context_tag': contextTag,
        'request_code': requestCode.name,
      },
      fatal: false,
    );

    // Deliver user-facing message via Messenger if provided
    if (userMessage != null) {
      _messenger.deliver(
        Message.error(userMessage, requestCode: requestCode),
      );
    }
  }
}
