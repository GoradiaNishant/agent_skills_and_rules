package com.example.app.core.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Extension for [NavGraphBuilder] to define animated composable routes cleanly.
 * Automatically wires enter, exit, popEnter, and popExit animations from [TransitionAnimations].
 */
fun NavGraphBuilder.navComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        arguments = arguments,
        deepLinks = deepLinks,
        enterTransition = TransitionAnimations.enterTransition(route),
        exitTransition = TransitionAnimations.exitTransition(route),
        popEnterTransition = TransitionAnimations.popEnterTransition(route),
        popExitTransition = TransitionAnimations.popExitTransition(route),
        content = content
    )
}

/**
 * Obtains a shared [ViewModel] scoped to the parent navigation graph entry.
 * If the current destination has no parent graph, falls back to scoping to the current entry.
 *
 * Example:
 * ```kotlin
 * navComposable("onboarding/step2") { backStackEntry ->
 *     val sharedVm: OnboardingViewModel = backStackEntry.sharedViewModel(navController)
 * }
 * ```
 */
@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(navController: NavController): T {
    val navGraphRoute = destination.parent?.route ?: return hiltViewModel()
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return hiltViewModel(parentEntry)
}
