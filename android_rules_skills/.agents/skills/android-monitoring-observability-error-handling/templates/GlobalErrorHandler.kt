package core.monitoring.error

import core.monitoring.crash.CrashReporter
import core.monitoring.logger.AppLogger
import core.ui.message.Message
import core.ui.message.Messenger
import core.ui.message.RequestCode
import core.ui.model.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Single source of truth for global error handling, Crashlytics logging,
 * and user-facing error message dispatching.
 */
interface GlobalErrorHandler {
    /**
     * Handles an unexpected exception: logs locally, reports non-fatal to Crashlytics,
     * and optionally dispatches a user-friendly UI message via [Messenger].
     */
    fun handleError(
        throwable: Throwable,
        contextTag: String,
        userMessage: UiText? = null,
        requestCode: RequestCode = RequestCode.UNKNOWN
    )

    /**
     * Standard CoroutineExceptionHandler that safely intercepts uncaught coroutine errors
     * without crashing the app.
     */
    fun createCoroutineExceptionHandler(contextTag: String): CoroutineExceptionHandler
}

/**
 * Production implementation of [GlobalErrorHandler].
 */
@Singleton
class GlobalErrorHandlerImpl @Inject constructor(
    private val logger: AppLogger,
    private val crashReporter: CrashReporter,
    private val messenger: Messenger
) : GlobalErrorHandler {

    override fun handleError(
        throwable: Throwable,
        contextTag: String,
        userMessage: UiText?,
        requestCode: RequestCode
    ) {
        // 1. Structured Coroutine Cancellation must NEVER be intercepted or swallowed
        if (throwable is CancellationException) {
            throw throwable
        }

        // 2. Log technical error locally with timer-based rate limiting
        logger.logThrottled(
            tag = contextTag,
            throttleKey = "${contextTag}_${throwable.javaClass.simpleName}",
            message = "Global error in [$contextTag]: ${throwable.message}",
            throwable = throwable
        )

        // 3. Add diagnostic breadcrumb and record non-fatal to Crashlytics
        crashReporter.logBreadcrumb("Error occurred in context: $contextTag | ${throwable.javaClass.simpleName}")
        crashReporter.recordException(
            throwable = throwable,
            metadata = mapOf(
                "context_tag" to contextTag,
                "request_code" to requestCode.name,
                "has_user_message" to (userMessage != null)
            )
        )

        // 4. Deliver user-facing message via Messenger if provided
        userMessage?.let { msg ->
            messenger.deliver(
                Message.error(
                    content = msg,
                    requestCode = requestCode
                )
            )
        }
    }

    override fun createCoroutineExceptionHandler(contextTag: String): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _: CoroutineContext, throwable: Throwable ->
            handleError(
                throwable = throwable,
                contextTag = contextTag
            )
        }
    }
}
