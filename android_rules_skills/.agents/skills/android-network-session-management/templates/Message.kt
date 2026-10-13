package core.ui.message

import androidx.annotation.Keep
import core.ui.model.UiText

/**
 * Common request identifier or tag used to identify which operation triggered the notification.
 */
enum class RequestCode {
    UNKNOWN,
    AUTH_LOGIN,
    AUTH_LOGOUT,
    TOKEN_REFRESH,
    DATA_SYNC,
    FORM_SUBMIT,
    FILE_UPLOAD,
    SOCKET_CONNECT,
    SOCKET_DISCONNECT,
    SOCKET_MESSAGE,
    SOCKET_ERROR
}

/**
 * Represents a structured UI notification (Snackbar, Banner, Toast).
 */
data class Message(
    val type: Type,
    val content: UiText,
    val requestCode: RequestCode = RequestCode.UNKNOWN,
    val actionLabel: UiText? = null,
    val onAction: (() -> Unit)? = null
) {
    companion object {
        fun success(
            content: UiText,
            requestCode: RequestCode = RequestCode.UNKNOWN,
            actionLabel: UiText? = null,
            onAction: (() -> Unit)? = null
        ) = Message(Type.SUCCESS, content, requestCode, actionLabel, onAction)

        fun error(
            content: UiText,
            requestCode: RequestCode = RequestCode.UNKNOWN,
            actionLabel: UiText? = null,
            onAction: (() -> Unit)? = null
        ) = Message(Type.ERROR, content, requestCode, actionLabel, onAction)

        fun warning(
            content: UiText,
            requestCode: RequestCode = RequestCode.UNKNOWN,
            actionLabel: UiText? = null,
            onAction: (() -> Unit)? = null
        ) = Message(Type.WARNING, content, requestCode, actionLabel, onAction)

        fun info(
            content: UiText,
            requestCode: RequestCode = RequestCode.UNKNOWN,
            actionLabel: UiText? = null,
            onAction: (() -> Unit)? = null
        ) = Message(Type.INFO, content, requestCode, actionLabel, onAction)
    }

    @Keep
    enum class Type {
        SUCCESS,
        ERROR,
        WARNING,
        INFO
    }
}
