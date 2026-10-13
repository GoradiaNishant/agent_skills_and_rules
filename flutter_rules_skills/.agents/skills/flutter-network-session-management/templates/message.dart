import 'ui_text.dart';

/// Identifies the operation that triggered the message.
enum RequestCode {
  unknown,
  authLogin,
  authLogout,
  tokenRefresh,
  dataSync,
  formSubmit,
  fileUpload,
  socketConnect,
  socketDisconnect,
  socketMessage,
  socketError,
}

/// Severity classification of a notification message.
enum MessageType {
  success,
  error,
  warning,
  info,
}

/// Represents a structured user-facing UI notification.
class Message {
  final MessageType type;
  final UiText content;
  final RequestCode requestCode;
  final UiText? actionLabel;
  final void Function()? onAction;

  const Message({
    required this.type,
    required this.content,
    this.requestCode = RequestCode.unknown,
    this.actionLabel,
    this.onAction,
  });

  factory Message.success(
    UiText content, {
    RequestCode requestCode = RequestCode.unknown,
    UiText? actionLabel,
    void Function()? onAction,
  }) =>
      Message(
        type: MessageType.success,
        content: content,
        requestCode: requestCode,
        actionLabel: actionLabel,
        onAction: onAction,
      );

  factory Message.error(
    UiText content, {
    RequestCode requestCode = RequestCode.unknown,
    UiText? actionLabel,
    void Function()? onAction,
  }) =>
      Message(
        type: MessageType.error,
        content: content,
        requestCode: requestCode,
        actionLabel: actionLabel,
        onAction: onAction,
      );

  factory Message.warning(
    UiText content, {
    RequestCode requestCode = RequestCode.unknown,
    UiText? actionLabel,
    void Function()? onAction,
  }) =>
      Message(
        type: MessageType.warning,
        content: content,
        requestCode: requestCode,
        actionLabel: actionLabel,
        onAction: onAction,
      );

  factory Message.info(
    UiText content, {
    RequestCode requestCode = RequestCode.unknown,
    UiText? actionLabel,
    void Function()? onAction,
  }) =>
      Message(
        type: MessageType.info,
        content: content,
        requestCode: requestCode,
        actionLabel: actionLabel,
        onAction: onAction,
      );
}
