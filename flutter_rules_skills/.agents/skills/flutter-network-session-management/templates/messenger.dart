import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:injectable/injectable.dart';
import 'message.dart';

/// Central event pipeline for dispatching transient UI messages (SnackBars, Banners)
/// from Repositories, BLoCs, or Interceptors without holding BuildContext references.
@singleton
class Messenger {
  final _messageController = StreamController<Message>.broadcast();
  final _clearController = StreamController<bool>.broadcast();

  final ValueNotifier<MessageType> messageType =
      ValueNotifier<MessageType>(MessageType.success);
  final ValueNotifier<RequestCode> requestCode =
      ValueNotifier<RequestCode>(RequestCode.unknown);

  Stream<Message> get message => _messageController.stream;
  Stream<bool> get clear => _clearController.stream;

  // Deduplication state to prevent spamming duplicate messages within 2000ms
  Object? _lastDispatchedKey;
  int _lastDispatchedTime = 0;

  /// Delivers a [Message] to the active UI subscriber.
  /// Automatically suppresses rapid duplicate messages within 2000ms unless [force] is true.
  void deliver(Message msg, {bool force = false}) {
    final now = DateTime.now().millisecondsSinceEpoch;
    final messageKey = msg.content;

    if (!force &&
        messageKey == _lastDispatchedKey &&
        now - _lastDispatchedTime < 2000) {
      return;
    }

    _lastDispatchedKey = messageKey;
    _lastDispatchedTime = now;

    _clearController.add(true);
    messageType.value = msg.type;
    requestCode.value = msg.requestCode;
    _messageController.add(msg);
  }

  /// Explicitly requests dismissal of active messages.
  void clearMessages() {
    _clearController.add(true);
  }

  void dispose() {
    _messageController.close();
    _clearController.close();
    messageType.dispose();
    requestCode.dispose();
  }
}
