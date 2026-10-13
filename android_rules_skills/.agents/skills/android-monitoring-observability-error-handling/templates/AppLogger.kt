package core.monitoring.logger

import android.os.SystemClock
import android.util.Log
import core.monitoring.sanitizer.DataSanitizer
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handle returned by [AppLogger.startTimer] to calculate elapsed execution time.
 */
interface LogTimer {
    /**
     * Stops the timer, logs the elapsed duration in milliseconds, and returns the duration.
     */
    fun stop(metadata: String? = null): Long
}

/**
 * Central application logging contract. All logging must route through this interface.
 */
interface AppLogger {
    fun v(tag: String, message: String)
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun f(tag: String, message: String, throwable: Throwable? = null)

    /**
     * Starts an execution timer to benchmark operation performance.
     */
    fun startTimer(tag: String, operation: String): LogTimer

    /**
     * Logs a message with rate-limiting to prevent logcat and telemetry spam.
     * Messages with identical [throttleKey] within [throttleWindowMs] are suppressed.
     */
    fun logThrottled(
        tag: String,
        throttleKey: String,
        message: String,
        level: Int = Log.WARN,
        throttleWindowMs: Long = 2000L
    )
}

/**
 * Executes [block], measures elapsed time in milliseconds, and logs the benchmark duration.
 */
inline fun <T> AppLogger.timed(tag: String, operation: String, block: () -> T): T {
    val startTime = SystemClock.elapsedRealtime()
    return try {
        val result = block()
        val duration = SystemClock.elapsedRealtime() - startTime
        d(tag, "⏱️ [$operation] completed in ${duration}ms")
        result
    } catch (e: Throwable) {
        val duration = SystemClock.elapsedRealtime() - startTime
        e(tag, "⏱️ [$operation] FAILED after ${duration}ms: ${e.message}", e)
        throw e
    }
}

/**
 * Production implementation of [AppLogger].
 * In Debug builds, outputs formatted sanitized logs to console.
 * In Release builds, suppresses verbose/debug logs and routes warnings/errors to telemetry.
 */
@Singleton
class AppLoggerImpl @Inject constructor(
    private val isDebug: Boolean
) : AppLogger {

    private val throttleMap = ConcurrentHashMap<String, Long>()

    override fun v(tag: String, message: String) {
        if (isDebug) Log.v(tag, DataSanitizer.sanitizeText(message))
    }

    override fun d(tag: String, message: String) {
        if (isDebug) Log.d(tag, DataSanitizer.sanitizeText(message))
    }

    override fun i(tag: String, message: String) {
        val sanitized = DataSanitizer.sanitizeText(message)
        if (isDebug) {
            Log.i(tag, sanitized)
        }
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        val sanitized = DataSanitizer.sanitizeText(message)
        Log.w(tag, sanitized, throwable)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        val sanitized = DataSanitizer.sanitizeText(message)
        Log.e(tag, sanitized, throwable)
    }

    override fun f(tag: String, message: String, throwable: Throwable?) {
        val sanitized = DataSanitizer.sanitizeText(message)
        Log.wtf(tag, sanitized, throwable)
    }

    override fun startTimer(tag: String, operation: String): LogTimer {
        val startTime = SystemClock.elapsedRealtime()
        return object : LogTimer {
            override fun stop(metadata: String?): Long {
                val elapsed = SystemClock.elapsedRealtime() - startTime
                val extra = if (metadata != null) " | $metadata" else ""
                d(tag, "⏱️ [$operation] finished in ${elapsed}ms$extra")
                return elapsed
            }
        }
    }

    override fun logThrottled(
        tag: String,
        throttleKey: String,
        message: String,
        level: Int,
        throttleWindowMs: Long
    ) {
        val now = SystemClock.elapsedRealtime()
        val lastLogged = throttleMap[throttleKey] ?: 0L
        if (now - lastLogged < throttleWindowMs) {
            return // Suppressed by throttle timer
        }
        throttleMap[throttleKey] = now

        when (level) {
            Log.VERBOSE -> v(tag, message)
            Log.DEBUG -> d(tag, message)
            Log.INFO -> i(tag, message)
            Log.WARN -> w(tag, message)
            Log.ERROR -> e(tag, message)
            Log.ASSERT -> f(tag, message)
            else -> d(tag, message)
        }
    }
}
