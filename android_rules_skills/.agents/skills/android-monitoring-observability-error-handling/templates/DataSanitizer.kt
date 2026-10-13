package core.monitoring.sanitizer

/**
 * Sanitizes sensitive user information, security credentials, and tokens
 * prior to logging or dispatching to telemetry/analytics.
 */
object DataSanitizer {

    private val SENSITIVE_KEYS = setOf(
        "authorization",
        "token",
        "access_token",
        "refresh_token",
        "id_token",
        "password",
        "secret",
        "api_key",
        "apikey",
        "credit_card",
        "card_number",
        "cvv",
        "ssn",
        "pin"
    )

    private val BEARER_REGEX = Regex("""(?i)Bearer\s+([a-zA-Z0-9_\-\.]+)""")
    private val EMAIL_REGEX = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""")

    /**
     * Sanitizes a single key-value pair. If the key matches sensitive terms, value is redacted.
     */
    fun sanitizeEntry(key: String, value: Any?): Any? {
        if (value == null) return null
        val lowerKey = key.lowercase()
        if (SENSITIVE_KEYS.any { lowerKey.contains(it) }) {
            return "[REDACTED]"
        }
        return if (value is String) sanitizeText(value) else value
    }

    /**
     * Sanitizes raw text string by redacting bearer tokens and masking email addresses.
     */
    fun sanitizeText(text: String): String {
        var sanitized = BEARER_REGEX.replace(text, "Bearer [REDACTED]")
        sanitized = EMAIL_REGEX.replace(sanitized) { matchResult ->
            val email = matchResult.value
            val atIndex = email.indexOf('@')
            if (atIndex > 1) {
                "${email.first()}***${email.substring(atIndex - 1)}"
            } else {
                "***@***"
            }
        }
        return sanitized
    }

    /**
     * Sanitizes a map of parameters for analytics or diagnostic metadata.
     */
    fun sanitizeMap(params: Map<String, Any?>): Map<String, Any?> {
        return params.mapValues { (key, value) -> sanitizeEntry(key, value) }
    }
}
