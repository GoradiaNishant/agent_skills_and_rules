package com.example.app.core.navigation

import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.reflect.KType
import kotlin.reflect.typeOf

val navigationJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

/**
 * Creates a type-safe [NavType] for any Kotlin class annotated with @[kotlinx.serialization.Serializable].
 */
inline fun <reified T : Any> serializableNavType(): NavType<T> {
    return SerializableNavType(serializer(), typeOf<T>())
}

/**
 * Custom [NavType] implementation that uses kotlinx.serialization and URL encoding/decoding
 * for safe passage of objects within Jetpack Compose route strings and saved state bundles.
 */
class SerializableNavType<T : Any>(
    private val serializer: KSerializer<T>,
    private val kType: KType
) : NavType<T>(isNullableAllowed = false) {

    override fun put(bundle: Bundle, key: String, value: T) {
        try {
            val jsonString = navigationJson.encodeToString(serializer, value)
            bundle.putString(key, jsonString)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to serialize $kType into bundle: ${e.message}")
        }
    }

    override fun get(bundle: Bundle, key: String): T? {
        return try {
            val encodedJson = bundle.getString(key) ?: return null
            navigationJson.decodeFromString(serializer, encodedJson)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to deserialize $kType from bundle: ${e.message}")
        }
    }

    override fun parseValue(value: String): T {
        return try {
            val decodedJson = URLDecoder.decode(value, StandardCharsets.UTF_8.name())
            navigationJson.decodeFromString(serializer, decodedJson)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse $kType from route value: ${e.message}")
        }
    }

    override fun serializeAsValue(value: T): String {
        return try {
            val jsonString = navigationJson.encodeToString(serializer, value)
            URLEncoder.encode(jsonString, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to serialize $kType to route value: ${e.message}")
        }
    }
}
