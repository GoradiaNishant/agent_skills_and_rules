package com.example.app.core.navigation

import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlinx.serialization.Serializable

/**
 * Central registry of application navigation destinations.
 */
object Destination {

    const val BASE_DEEPLINK_URL = "app://example.com"

    // ── Static Routes ─────────────────────────────────────────────────────────
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Home : Screen("home") {
        data object Dashboard : Screen("home/dashboard")
        data object Feed : Screen("home/feed")
        data object Profile : Screen("home/profile")
    }

    // ── Dynamic Routes with Typed Arguments ───────────────────────────────────
    data object ItemDetails : DynamicScreen<String>(
        baseRoute = "item-details",
        routeArgName = "itemId",
        navType = serializableNavType<String>()
    )

    data object OrderConfirmation : DynamicScreen<OrderSummaryArgs>(
        baseRoute = "order-confirmation",
        routeArgName = "orderSummaryArgs",
        navType = serializableNavType<OrderSummaryArgs>()
    )

    // ── Query Parameter Routes ────────────────────────────────────────────────
    data object Search : Screen("search") {
        const val argQuery = "query"
        const val argFilter = "filter"

        override val route = "search?$argQuery={$argQuery}&$argFilter={$argFilter}"

        val navArguments = listOf(
            navArgument(argQuery) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(argFilter) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )

        fun dynamicRoute(query: String? = null, filter: String? = null): String {
            return buildString {
                append("search")
                val params = mutableListOf<String>()
                if (!query.isNullOrEmpty()) params.add("$argQuery=${Uri.encode(query)}")
                if (!filter.isNullOrEmpty()) params.add("$argFilter=${Uri.encode(filter)}")
                if (params.isNotEmpty()) {
                    append("?")
                    append(params.joinToString("&"))
                }
            }
        }
    }

    // ── Base Screen Classes ───────────────────────────────────────────────────
    abstract class Screen(val baseRoute: String) {
        open val route: String = baseRoute
        open val deeplink: String = "$BASE_DEEPLINK_URL/$baseRoute"
    }

    abstract class DynamicScreen<T : Any>(
        baseRoute: String,
        val routeArgName: String,
        val navType: NavType<T>,
    ) : Screen(baseRoute) {

        val navArguments = listOf(navArgument(routeArgName) { type = navType })

        override val route: String = "$baseRoute/{$routeArgName}"
        override val deeplink: String = "$BASE_DEEPLINK_URL/$baseRoute/{$routeArgName}"

        fun dynamicRoute(param: T): String {
            return when (param) {
                is String -> "$baseRoute/$param"
                else -> {
                    val serialized = navType.serializeAsValue(param)
                    "$baseRoute/${Uri.encode(serialized)}"
                }
            }
        }

        fun dynamicDeeplink(param: T): String {
            return when (param) {
                is String -> "$BASE_DEEPLINK_URL/$baseRoute/$param"
                else -> {
                    val serialized = navType.serializeAsValue(param)
                    "$BASE_DEEPLINK_URL/$baseRoute/$serialized"
                }
            }
        }
    }
}

@Serializable
data class OrderSummaryArgs(
    val orderId: String,
    val totalAmount: Double,
    val itemCount: Int,
)
