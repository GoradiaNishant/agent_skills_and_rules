package core.network.utils

import core.ui.message.Message
import core.ui.message.Messenger
import core.ui.message.RequestCode
import core.ui.model.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Standard typed network result representation holding localized [UiText] instead of hardcoded strings.
 */
sealed interface NetworkResult<out T> {
    data class Success<out T>(val data: T, val message: UiText? = null) : NetworkResult<T>
    data class Error(
        val message: UiText,
        val code: Int? = null,
        val requestCode: RequestCode = RequestCode.UNKNOWN
    ) : NetworkResult<Nothing>
    data class Exception(val throwable: Throwable) : NetworkResult<Nothing>
}

inline fun <T> NetworkResult<T>.onSuccess(action: (value: T) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Success) action(data)
    return this
}

inline fun <T> NetworkResult<T>.onError(action: (message: UiText, code: Int?) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Error) action(message, code)
    return this
}

/**
 * Delivers failure message globally via [messenger] if present and returns the result for chaining.
 */
fun <T> NetworkResult<T>.deliverOnFailure(
    messenger: Messenger,
    force: Boolean = false
): NetworkResult<T> {
    if (this is NetworkResult.Error) {
        messenger.deliver(Message.error(this.message, this.requestCode), force = force)
    }
    return this
}
