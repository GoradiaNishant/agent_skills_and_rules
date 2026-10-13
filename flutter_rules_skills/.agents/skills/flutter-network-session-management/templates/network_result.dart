import 'ui_text.dart';

/// A typed result wrapper for network and repository operations.
sealed class NetworkResult<T> {
  const NetworkResult();

  const factory NetworkResult.success(T data, {UiText? message}) =
      NetworkSuccess<T>;

  const factory NetworkResult.error(UiText message, {int? code}) =
      NetworkError<T>;

  const factory NetworkResult.exception(Object throwable, [StackTrace? stackTrace]) =
      NetworkException<T>;
}

class NetworkSuccess<T> extends NetworkResult<T> {
  final T data;
  final UiText? message;
  const NetworkSuccess(this.data, {this.message});
}

class NetworkError<T> extends NetworkResult<T> {
  final UiText message;
  final int? code;
  const NetworkError(this.message, {this.code});
}

class NetworkException<T> extends NetworkResult<T> {
  final Object throwable;
  final StackTrace? stackTrace;
  const NetworkException(this.throwable, [this.stackTrace]);
}
