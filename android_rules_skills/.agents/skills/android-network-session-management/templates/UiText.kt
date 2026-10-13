package core.ui.model

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * Encapsulates both server-driven dynamic strings and localized compile-time string resources.
 * Prevents hardcoded strings in Repositories, ViewModels, and UI components.
 */
sealed class UiText {

    /**
     * A runtime string (e.g., from an API error response body).
     * [value] is nullable; if blank or null, nothing is displayed.
     */
    data class DynamicString(val value: String?) : UiText()

    /**
     * A compile-time string resource reference with optional formatting arguments.
     */
    data class StringResource(
        @StringRes val resId: Int?,
        val args: List<Any> = emptyList()
    ) : UiText()

    /**
     * A compile-time plural resource reference with quantity count and formatting arguments.
     */
    data class PluralResource(
        @PluralsRes val resId: Int?,
        val count: Int,
        val args: List<Any> = emptyList()
    ) : UiText()

    /**
     * Resolves to a displayable String with a standard Android [Context].
     * Returns null if content is absent, blank, or resId is null.
     */
    fun asString(context: Context): String? = when (this) {
        is DynamicString -> value?.takeIf { it.isNotBlank() }
        is StringResource -> resId?.let { id ->
            if (args.isEmpty()) context.getString(id)
            else context.getString(id, *args.toTypedArray())
        }
        is PluralResource -> resId?.let { id ->
            if (args.isEmpty()) context.resources.getQuantityString(id, count)
            else context.resources.getQuantityString(id, count, *args.toTypedArray())
        }
    }

    fun asStringOrEmpty(context: Context): String = asString(context).orEmpty()

    companion object {
        /**
         * Smart fallback factory:
         * Uses [apiMessage] if present and not blank;
         * otherwise falls back to the localized [fallbackRes] string resource.
         */
        fun from(apiMessage: String? = null, @StringRes fallbackRes: Int? = null): UiText =
            if (!apiMessage.isNullOrBlank()) DynamicString(apiMessage)
            else StringResource(fallbackRes)

        /** Convenience factory for a string resource with optional format arguments */
        fun res(@StringRes resId: Int?, vararg args: Any): UiText =
            StringResource(resId, args.toList())

        /** Convenience factory for a plural string resource */
        fun plural(@PluralsRes resId: Int?, count: Int, vararg args: Any): UiText =
            PluralResource(resId, count, args.toList())

        /** Convenience factory for a dynamic string */
        fun str(value: String?): UiText = DynamicString(value)

        /** Empty representation */
        val Empty: UiText = DynamicString(null)
    }
}

/**
 * Composable extension to resolve UiText directly in Jetpack Compose UI.
 */
@Composable
@ReadOnlyComposable
fun UiText?.asString(): String? {
    if (this == null) return null
    return when (this) {
        is UiText.DynamicString -> value?.takeIf { it.isNotBlank() }
        is UiText.StringResource -> resId?.let { id ->
            if (args.isEmpty()) stringResource(id)
            else stringResource(id, *args.toTypedArray())
        }
        is UiText.PluralResource -> resId?.let { id ->
            if (args.isEmpty()) pluralStringResource(id, count)
            else pluralStringResource(id, count, *args.toTypedArray())
        }
    }
}

@Composable
@ReadOnlyComposable
fun UiText?.asStringOrEmpty(): String = this.asString().orEmpty()
