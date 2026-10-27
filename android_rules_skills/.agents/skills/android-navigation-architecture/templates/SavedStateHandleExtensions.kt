package com.example.app.core.navigation

import androidx.lifecycle.SavedStateHandle
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Navigation result keys used across screen communication.
 */
object NavigationKeys {
    const val KEY_ITEM_UPDATED = "key_item_updated"
    const val KEY_REFRESH_REQUIRED = "key_refresh_required"
    const val KEY_SELECTED_ID = "key_selected_id"
}

/**
 * Safely decodes a serialized JSON object from [SavedStateHandle].
 * Handles both plain JSON strings and URL-encoded strings gracefully.
 */
inline fun <reified T : Any> SavedStateHandle.getArguments(argName: String): T? {
    val rawValue = this.get<String>(argName) ?: return null

    return runCatching {
        // Direct JSON decode
        navigationJson.decodeFromString<T>(rawValue)
    }.recoverCatching {
        // Fallback: URL decode first, then JSON decode
        val decoded = URLDecoder.decode(rawValue, StandardCharsets.UTF_8.name())
        navigationJson.decodeFromString<T>(decoded)
    }.getOrNull()
}

/**
 * Clears a navigation result key from SavedStateHandle after consumption.
 */
fun <T> SavedStateHandle.consumeResult(key: String, defaultValue: T): T? {
    val value = this.get<T>(key)
    this[key] = defaultValue
    return value
}
