package core.monitoring.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import core.monitoring.sanitizer.DataSanitizer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central analytics contract. All event tracking, screen views, and user properties
 * must pass through this single interface.
 */
interface AnalyticsTracker {
    /**
     * Logs an analytics event with sanitized parameters.
     * Event names must follow lowercase snake_case conventions.
     */
    fun trackEvent(eventName: String, params: Map<String, Any?> = emptyMap())

    /**
     * Tracks screen view navigation.
     */
    fun trackScreen(screenName: String, screenClass: String? = null)

    /**
     * Sets a user property for audience segmentation.
     */
    fun setUserProperty(name: String, value: String?)

    /**
     * Respects user privacy consent settings.
     */
    fun setAnalyticsCollectionEnabled(enabled: Boolean)
}

/**
 * Production Firebase Analytics implementation of [AnalyticsTracker].
 */
@Singleton
class FirebaseAnalyticsTracker @Inject constructor(
    @ApplicationContext private val context: Context
) : AnalyticsTracker {

    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }

    override fun trackEvent(eventName: String, params: Map<String, Any?>) {
        runCatching {
            val sanitizedParams = DataSanitizer.sanitizeMap(params)
            val bundle = Bundle().apply {
                sanitizedParams.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Boolean -> putBoolean(key, value)
                        null -> Unit
                        else -> putString(key, value.toString())
                    }
                }
            }
            firebaseAnalytics.logEvent(eventName.lowercase(), bundle)
        }
    }

    override fun trackScreen(screenName: String, screenClass: String?) {
        runCatching {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                if (screenClass != null) {
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
                }
            }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        }
    }

    override fun setUserProperty(name: String, value: String?) {
        runCatching {
            val sanitized = value?.let { DataSanitizer.sanitizeText(it) }
            firebaseAnalytics.setUserProperty(name.lowercase(), sanitized)
        }
    }

    override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        runCatching {
            firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
        }
    }
}
