package com.example.app.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/**
 * Standardized animated transitions for Jetpack Compose navigation.
 * Employs horizontal slide + fade + subtle scale for detail navigation,
 * and pure smooth fade for bottom navigation tabs.
 */
object TransitionAnimations {

    private const val ANIMATION_DURATION = 350
    private const val SLIDE_OFFSET = 300
    private const val DEFAULT_SCALE = 0.95f

    // List of bottom-tab routes that should transition via cross-fade only
    private val bottomTabRoutes = setOf(
        Destination.Home.Dashboard.route,
        Destination.Home.Feed.route,
        Destination.Home.Profile.route,
    )

    fun enterTransition(route: String): (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?) =
        {
            if (bottomTabRoutes.contains(route)) {
                fadeIn(animationSpec = tween(ANIMATION_DURATION, easing = EaseInOut))
            } else {
                slideInHorizontally(
                    initialOffsetX = { SLIDE_OFFSET },
                    animationSpec = tween(ANIMATION_DURATION)
                ) + fadeIn(
                    animationSpec = tween(ANIMATION_DURATION, easing = EaseInOut)
                ) + scaleIn(
                    initialScale = DEFAULT_SCALE,
                    animationSpec = tween(ANIMATION_DURATION, easing = EaseInOut)
                )
            }
        }

    fun exitTransition(route: String): (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?) =
        {
            if (bottomTabRoutes.contains(route)) {
                fadeOut(animationSpec = tween(ANIMATION_DURATION, easing = EaseInOut))
            } else {
                slideOutHorizontally(
                    targetOffsetX = { -SLIDE_OFFSET },
                    animationSpec = tween(ANIMATION_DURATION)
                ) + fadeOut(
                    animationSpec = tween(ANIMATION_DURATION, easing = EaseInOut)
                ) + scaleOut(
                    targetScale = DEFAULT_SCALE,
                    animationSpec = tween(ANIMATION_DURATION, easing = EaseInOut)
                )
            }
        }

    fun popEnterTransition(route: String): (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?) =
        {
            if (bottomTabRoutes.contains(route)) {
                fadeIn(animationSpec = tween(ANIMATION_DURATION, easing = EaseInOut))
            } else {
                slideInHorizontally(
                    initialOffsetX = { -SLIDE_OFFSET },
                    animationSpec = tween(ANIMATION_DURATION)
                ) + fadeIn(
                    animationSpec = tween(ANIMATION_DURATION, easing = EaseInOut)
                ) + scaleIn(
                    initialScale = DEFAULT_SCALE,
                    animationSpec = tween(ANIMATION_DURATION, easing = EaseInOut)
                )
            }
        }

    fun popExitTransition(route: String): (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?) =
        {
            if (bottomTabRoutes.contains(route)) {
                fadeOut(animationSpec = tween(ANIMATION_DURATION, easing = EaseInOut))
            } else {
                slideOutHorizontally(
                    targetOffsetX = { SLIDE_OFFSET },
                    animationSpec = tween(ANIMATION_DURATION)
                ) + fadeOut(
                    animationSpec = tween(ANIMATION_DURATION, easing = EaseInOut)
                ) + scaleOut(
                    targetScale = DEFAULT_SCALE,
                    animationSpec = tween(ANIMATION_DURATION, easing = EaseInOut)
                )
            }
        }
}
