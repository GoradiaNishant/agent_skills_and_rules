/// Sanitizes sensitive user credentials, tokens, and PII prior to logging or telemetry.
class DataSanitizer {
  static const _sensitiveKeys = {
    'authorization',
    'token',
    'access_token',
    'refresh_token',
    'id_token',
    'password',
    'secret',
    'api_key',
    'apikey',
    'credit_card',
    'card_number',
    'cvv',
    'ssn',
    'pin'
  };

  static final _bearerRegex = RegExp(r'Bearer\s+([a-zA-Z0-9_\-\.]+)', caseSensitive: false);
  static final _emailRegex = RegExp(r'[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}');

  static Object? sanitizeEntry(String key, Object? value) {
    if (value == null) return null;
    final lowerKey = key.toLowerCase();
    if (_sensitiveKeys.any((s) => lowerKey.contains(s))) {
      return '[REDACTED]';
    }
    if (value is String) return sanitizeText(value);
    return value;
  }

  static String sanitizeText(String text) {
    var sanitized = text.replaceAllMapped(_bearerRegex, (m) => 'Bearer [REDACTED]');
    sanitized = sanitized.replaceAllMapped(_emailRegex, (match) {
      final email = match.group(0)!;
      final atIndex = email.indexOf('@');
      if (atIndex > 1) {
        return '${email[0]}***${email.substring(atIndex - 1)}';
      }
      return '***@***';
    });
    return sanitized;
  }

  static Map<String, Object?> sanitizeMap(Map<String, Object?> params) {
    return params.map((key, value) => MapEntry(key, sanitizeEntry(key, value)));
  }
}
