package com.example.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Composable bridge between [Navigator] and [NavController].
 * Observes navigation flows and dispatches real navigation operations on the UI thread.
 */
@Composable
fun NavHandler(
    navController: NavController,
    navigator: Navigator,
    finish: () -> Unit = {}
) {
    LaunchedEffect("navigation_handler") {
        navigator.navigate.onEach { target ->
            when {
                // Route navigation with full popUpTo semantics (e.g. clearing auth stack)
                target.popUpToRoute != null -> {
                    navController.navigate(target.route) {
                        popUpTo(target.popUpToRoute) {
                            inclusive = target.popUpToInclusive
                        }
                        launchSingleTop = true
                    }
                }
                // Single pop-and-replace navigation
                target.popBackstack -> {
                    navController.popBackStack()
                    navController.navigate(target.route)
                }
                // Standard navigation
                else -> {
                    navController.navigate(target.route)
                }
            }
        }.launchIn(this)

        navigator.deeplink.onEach { deeplink ->
            if (navController.graph.hasDeepLink(deeplink.uri)) {
                val alreadyAtDestination = navController.currentDestination?.hasDeepLink(deeplink.uri) ?: false
                if (!alreadyAtDestination) {
                    if (deeplink.popBackstack) navController.popBackStack()
                    navController.navigate(deeplink.uri)
                }
            }
        }.launchIn(this)

        navigator.back.onEach { backEvent ->
            if (backEvent.recreate) {
                navController.previousBackStackEntry?.destination?.route?.let { route ->
                    navController.navigate(route) {
                        popUpTo(route) { inclusive = true }
                    }
                }
            } else {
                navController.navigateUp()
            }
        }.launchIn(this)

        navigator.end.onEach {
            finish()
        }.launchIn(this)
    }
}
