import 'dart:developer' as developer;
import 'package:flutter/foundation.dart';
import 'package:injectable/injectable.dart';
import 'data_sanitizer.dart';

/// Stopwatch handle returned by [AppLogger.startTimer].
abstract interface class LogTimer {
  /// Stops the timer, logs the elapsed duration in milliseconds, and returns elapsed ms.
  int stop([String? metadata]);
}

/// Central application logging contract with built-in execution timers and throttling.
abstract interface class AppLogger {
  void v(String tag, String message);
  void d(String tag, String message);
  void i(String tag, String message);
  void w(String tag, String message, [Object? error, StackTrace? stackTrace]);
  void e(String tag, String message, [Object? error, StackTrace? stackTrace]);
  void f(String tag, String message, [Object? error, StackTrace? stackTrace]);

  /// Starts an execution timer to benchmark operation performance.
  LogTimer startTimer(String tag, String operation);

  /// Benchmarks a synchronous or asynchronous operation and logs the elapsed milliseconds.
  T timed<T>(String tag, String operation, T Function() block);

  /// Logs a message with rate-limiting to prevent logcat and telemetry spam.
  void logThrottled(
    String tag,
    String throttleKey,
    String message, {
    int level = 900, // WARNING level
    int throttleWindowMs = 2000,
  });
}

@singleton
class AppLoggerImpl implements AppLogger {
  final bool _isDebug;
  final Map<String, int> _throttleMap = {};

  AppLoggerImpl({bool? isDebug}) : _isDebug = isDebug ?? kDebugMode;

  @override
  void v(String tag, String message) {
    if (_isDebug) {
      developer.log(DataSanitizer.sanitizeText(message), name: tag, level: 500);
    }
  }

  @override
  void d(String tag, String message) {
    if (_isDebug) {
      developer.log(DataSanitizer.sanitizeText(message), name: tag, level: 700);
    }
  }

  @override
  void i(String tag, String message) {
    developer.log(DataSanitizer.sanitizeText(message), name: tag, level: 800);
  }

  @override
  void w(String tag, String message, [Object? error, StackTrace? stackTrace]) {
    developer.log(
      DataSanitizer.sanitizeText(message),
      name: tag,
      level: 900,
      error: error,
      stackTrace: stackTrace,
    );
  }

  @override
  void e(String tag, String message, [Object? error, StackTrace? stackTrace]) {
    developer.log(
      DataSanitizer.sanitizeText(message),
      name: tag,
      level: 1000,
      error: error,
      stackTrace: stackTrace,
    );
  }

  @override
  void f(String tag, String message, [Object? error, StackTrace? stackTrace]) {
    developer.log(
      DataSanitizer.sanitizeText(message),
      name: tag,
      level: 1200,
      error: error,
      stackTrace: stackTrace,
    );
  }

  @override
  LogTimer startTimer(String tag, String operation) {
    final stopwatch = Stopwatch()..start();
    return _LogTimerImpl(this, tag, operation, stopwatch);
  }

  @override
  T timed<T>(String tag, String operation, T Function() block) {
    final stopwatch = Stopwatch()..start();
    try {
      final result = block();
      stopwatch.stop();
      d(tag, '⏱️ [$operation] completed in ${stopwatch.elapsedMilliseconds}ms');
      return result;
    } catch (err, stack) {
      stopwatch.stop();
      e(tag, '⏱️ [$operation] FAILED after ${stopwatch.elapsedMilliseconds}ms: $err', err, stack);
      rethrow;
    }
  }

  @override
  void logThrottled(
    String tag,
    String throttleKey,
    String message, {
    int level = 900,
    int throttleWindowMs = 2000,
  }) {
    final now = DateTime.now().millisecondsSinceEpoch;
    final lastLogged = _throttleMap[throttleKey] ?? 0;
    if (now - lastLogged < throttleWindowMs) {
      return; // Throttled
    }
    _throttleMap[throttleKey] = now;
    developer.log(DataSanitizer.sanitizeText(message), name: tag, level: level);
  }
}

class _LogTimerImpl implements LogTimer {
  final AppLogger _logger;
  final String _tag;
  final String _operation;
  final Stopwatch _stopwatch;

  _LogTimerImpl(this._logger, this._tag, this._operation, this._stopwatch);

  @override
  int stop([String? metadata]) {
    _stopwatch.stop();
    final elapsed = _stopwatch.elapsedMilliseconds;
    final extra = metadata != null ? ' | $metadata' : '';
    _logger.d(_tag, '⏱️ [$_operation] finished in ${elapsed}ms$extra');
    return elapsed;
  }
}
