package com.example.app.core.navigation

import android.net.Uri
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Decoupled navigation coordinator scoped to ActivityRetainedComponent.
 * Injected directly into ViewModels to execute navigation actions, pass results back
 * to calling ViewModels, and handle deep links without referencing NavController or Android UI contexts.
 */
@ActivityRetainedScoped
class Navigator @Inject constructor() {

    private val _navigate = MutableSharedFlow<NavTarget>(extraBufferCapacity = 1)
    private val _deeplink = MutableSharedFlow<NavDeeplink>(extraBufferCapacity = 1)
    private val _back = MutableSharedFlow<NavBack>(extraBufferCapacity = 1)
    private val _end = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    private val _results = MutableSharedFlow<Pair<String, Any>>(extraBufferCapacity = 5)

    val navigate = _navigate.asSharedFlow()
    val deeplink = _deeplink.asSharedFlow()
    val back = _back.asSharedFlow()
    val end = _end.asSharedFlow()
    val results = _results.asSharedFlow()

    /**
     * Navigate to a route string.
     * @param route The destination route.
     * @param popBackstack If true, pops current destination before navigating.
     */
    fun navigateTo(route: String, popBackstack: Boolean = false) {
        _navigate.tryEmit(NavTarget(route = route, popBackstack = popBackstack))
    }

    /**
     * Navigate to a route with full popUpTo semantics.
     * @param route The destination route string.
     * @param popUpToRoute The route to pop up to in the backstack.
     * @param inclusive If true, the [popUpToRoute] destination is also popped.
     */
    fun navigateTo(
        route: String,
        popUpToRoute: String,
        inclusive: Boolean = true,
    ) {
        _navigate.tryEmit(
            NavTarget(
                route = route,
                popBackstack = false,
                popUpToRoute = popUpToRoute,
                popUpToInclusive = inclusive,
            )
        )
    }

    /**
     * Navigate using a deep link Uri.
     */
    fun navigateTo(uri: Uri, popBackstack: Boolean = false) {
        _deeplink.tryEmit(NavDeeplink(uri, popBackstack))
    }

    /**
     * Navigate back / pop backstack.
     * @param recreate If true, pops and re-navigates to previous destination to force refresh.
     */
    fun navigateBack(recreate: Boolean = false) {
        _back.tryEmit(NavBack(recreate))
    }

    /**
     * Send a result back to a parent or calling ViewModel.
     * ViewModels observe this via [observeResult].
     */
    fun <T : Any> setResult(key: String, result: T) {
        _results.tryEmit(key to result)
    }

    /**
     * Sets a result and immediately navigates back.
     * Eliminates the need for any onBackWithResult callbacks in Screen/Route composables.
     */
    fun <T : Any> navigateBackWithResult(key: String, result: T) {
        setResult(key, result)
        navigateBack()
    }

    /**
     * Type-safe Flow of results for a specific [key].
     * Call inside ViewModel init or viewModelScope.launch.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> observeResult(key: String): Flow<T> {
        return results
            .filter { it.first == key }
            .map { it.second as T }
    }

    /**
     * Finish the host Activity.
     */
    fun finish() {
        _end.tryEmit(true)
    }

    data class NavTarget(
        val route: String,
        val popBackstack: Boolean = false,
        val popUpToRoute: String? = null,
        val popUpToInclusive: Boolean = true,
    )

    data class NavDeeplink(
        val uri: Uri,
        val popBackstack: Boolean = false
    )

    data class NavBack(
        val recreate: Boolean = false
    )
}
