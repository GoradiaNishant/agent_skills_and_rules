package core.ui.message

import android.content.Context
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central event pipeline for dispatching transient UI messages (Snackbars, Toasts, Alerts)
 * from Repositories, ViewModels, or Interceptors without holding Android Context or Activity references.
 */
@Singleton
class Messenger @Inject constructor() {

    private val _message = MutableSharedFlow<Message>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val _clear = MutableSharedFlow<Boolean>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val _messageType = MutableStateFlow(Message.Type.SUCCESS)
    private val _requestCode = MutableStateFlow(RequestCode.UNKNOWN)

    val message: SharedFlow<Message> = _message.asSharedFlow()
    val clear: SharedFlow<Boolean> = _clear.asSharedFlow()
    val messageType: StateFlow<Message.Type> = _messageType.asStateFlow()
    val requestCode: StateFlow<RequestCode> = _requestCode.asStateFlow()

    // Deduplication state to prevent spamming duplicate messages within 2 seconds
    private var lastDispatchedKey: Any? = null
    private var lastDispatchedTime: Long = 0L

    /**
     * Delivers a [Message] to the active UI subscriber.
     * Automatically suppresses rapid duplicate messages within 2000ms.
     */
    fun deliver(message: Message, force: Boolean = false) {
        val now = System.currentTimeMillis()
        val messageKey = message.content
        if (!force && messageKey == lastDispatchedKey && now - lastDispatchedTime < 2000L) {
            return
        }
        lastDispatchedKey = messageKey
        lastDispatchedTime = now

        _clear.tryEmit(true)
        _messageType.tryEmit(message.type)
        _requestCode.tryEmit(message.requestCode)
        _message.tryEmit(message)
    }

    /**
     * Explicitly requests dismissal/clear of active message.
     */
    fun clear() {
        _clear.tryEmit(true)
    }
}
