package core.monitoring.crash

import com.google.firebase.crashlytics.FirebaseCrashlytics
import core.monitoring.sanitizer.DataSanitizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction over crash reporting and non-fatal issue diagnostics.
 * Keeps vendor-specific SDK implementations decoupled from feature layers.
 */
interface CrashReporter {
    /**
     * Adds a lightweight breadcrumb to trace the sequence of user and system events before a crash.
     */
    fun logBreadcrumb(message: String)

    /**
     * Sets a diagnostic key-value attribute attached to subsequent crash/non-fatal reports.
     */
    fun setCustomKey(key: String, value: Any)

    /**
     * Associates an anonymized user identifier with crash reports.
     */
    fun setUserId(userId: String?)

    /**
     * Records a non-fatal exception with sanitized diagnostic metadata.
     */
    fun recordException(throwable: Throwable, metadata: Map<String, Any?> = emptyMap())
}

/**
 * Production Firebase Crashlytics implementation of [CrashReporter].
 * Automatically scrubs sensitive data and prevents telemetry exceptions from crashing the app.
 */
@Singleton
class FirebaseCrashReporter @Inject constructor() : CrashReporter {

    private val crashlytics: FirebaseCrashlytics by lazy {
        FirebaseCrashlytics.getInstance()
    }

    override fun logBreadcrumb(message: String) {
        runCatching {
            val sanitized = DataSanitizer.sanitizeText(message)
            crashlytics.log(sanitized)
        }
    }

    override fun setCustomKey(key: String, value: Any) {
        runCatching {
            val sanitizedVal = DataSanitizer.sanitizeEntry(key, value) ?: return
            when (sanitizedVal) {
                is String -> crashlytics.setCustomKey(key, sanitizedVal)
                is Boolean -> crashlytics.setCustomKey(key, sanitizedVal)
                is Int -> crashlytics.setCustomKey(key, sanitizedVal)
                is Long -> crashlytics.setCustomKey(key, sanitizedVal)
                is Float -> crashlytics.setCustomKey(key, sanitizedVal)
                is Double -> crashlytics.setCustomKey(key, sanitizedVal)
                else -> crashlytics.setCustomKey(key, sanitizedVal.toString())
            }
        }
    }

    override fun setUserId(userId: String?) {
        runCatching {
            // Anonymize or pass through sanitized user ID (never email or raw PII)
            crashlytics.setUserId(userId?.let { DataSanitizer.sanitizeText(it) } ?: "")
        }
    }

    override fun recordException(throwable: Throwable, metadata: Map<String, Any?>) {
        runCatching {
            val sanitizedMetadata = DataSanitizer.sanitizeMap(metadata)
            sanitizedMetadata.forEach { (key, value) ->
                if (value != null) setCustomKey(key, value)
            }
            crashlytics.recordException(throwable)
        }
    }
}
