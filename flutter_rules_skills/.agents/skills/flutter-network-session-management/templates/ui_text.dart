import 'package:flutter/widgets.dart';

/// Polymorphic representation of user-visible text.
/// Supports dynamic API strings and localized string keys / callbacks.
sealed class UiText {
  const UiText();

  /// Resolves the text into a displayable [String] given the [BuildContext].
  String asString(BuildContext context);

  /// Creates a [UiText] from a dynamic string (e.g. backend error message).
  const factory UiText.dynamic(String value) = _DynamicUiText;

  /// Creates a [UiText] from a localization lookup function.
  const factory UiText.localized(String Function(BuildContext context) resolver) =
      _LocalizedUiText;

  /// Smart factory prioritizing the dynamic API message if present and non-empty,
  /// falling back to the localized resolver.
  factory UiText.from({
    String? apiMessage,
    required String Function(BuildContext context) fallback,
  }) {
    if (apiMessage != null && apiMessage.trim().isNotEmpty) {
      return UiText.dynamic(apiMessage.trim());
    }
    return UiText.localized(fallback);
  }
}

class _DynamicUiText extends UiText {
  final String value;
  const _DynamicUiText(this.value);

  @override
  String asString(BuildContext context) => value;

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is _DynamicUiText && other.value == value;

  @override
  int get hashCode => value.hashCode;
}

class _LocalizedUiText extends UiText {
  final String Function(BuildContext context) resolver;
  const _LocalizedUiText(this.resolver);

  @override
  String asString(BuildContext context) => resolver(context);
}
