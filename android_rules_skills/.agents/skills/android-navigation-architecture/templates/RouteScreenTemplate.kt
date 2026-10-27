package com.example.app.features.sample.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import com.example.app.core.navigation.Destination
import com.example.app.core.navigation.NavigationKeys
import com.example.app.core.navigation.Navigator
import com.example.app.core.navigation.getArguments
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── 1. Route Destination Composable (Stateful) ──────────────────────────────
// STRICT RULE: Route composables MUST NEVER accept NavController or result callbacks
// (e.g. onBackWithResult). All navigation and state restoration belongs strictly in the ViewModel.

@Composable
fun ItemDetailRoute(
    viewModel: ItemDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Lifecycle hooks for re-subscribing/refreshing when screen resumes
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onEvent(ItemDetailEvent.ScreenResumed)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.onEvent(ItemDetailEvent.ScreenPaused)
    }

    ItemDetailScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

// ── 2. Screen Composable (Pure Stateless UI) ────────────────────────────────
// STRICT RULE: Screen composables accept ONLY uiState and onEvent.
// No NavController, no lambdas for navigation/results, no Hilt references.

@Composable
fun ItemDetailScreen(
    uiState: ItemDetailUiState,
    onEvent: (ItemDetailEvent) -> Unit,
) {
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }

                uiState.errorMessage != null -> {
                    Text(text = "Error: ${uiState.errorMessage}")
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Item ID: ${uiState.itemId}")
                        Text(text = "Title: ${uiState.title}")

                        Button(
                            onClick = { onEvent(ItemDetailEvent.EditClicked) },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Edit Item (Forward Nav)")
                        }

                        Button(
                            onClick = { onEvent(ItemDetailEvent.SaveAndReturnClicked) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Save & Return Result")
                        }

                        Button(
                            onClick = { onEvent(ItemDetailEvent.BackClicked) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
    }
}

// ── 3. ViewModel: Navigator & SavedStateHandle Injected Here Exclusively ────
// All navigation, route argument parsing, and result returning happen HERE.

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigator: Navigator,
) : ViewModel() {

    // Retrieve route argument directly from SavedStateHandle
    val itemId: String = checkNotNull(
        savedStateHandle.get<String>(Destination.ItemDetails.routeArgName)
            ?: savedStateHandle.getArguments<String>(Destination.ItemDetails.routeArgName)
    ).trim('"')

    private val _uiState = MutableStateFlow(ItemDetailUiState(itemId = itemId))
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    init {
        loadItemDetails()
        observeChildResults()
    }

    /**
     * Observes results from child screens directly via [Navigator.observeResult].
     * Completely eliminates callback passing into Screen or Route composables.
     */
    private fun observeChildResults() {
        viewModelScope.launch {
            navigator.observeResult<Boolean>(NavigationKeys.KEY_ITEM_UPDATED).collect { isUpdated ->
                if (isUpdated) {
                    loadItemDetails()
                }
            }
        }
    }

    fun onEvent(event: ItemDetailEvent) {
        when (event) {
            ItemDetailEvent.ScreenResumed -> {
                // Reconnect active streams, websockets, or refresh data
            }

            ItemDetailEvent.ScreenPaused -> {
                // Pause live connections
            }

            ItemDetailEvent.EditClicked -> {
                // Forward navigation via Navigator
                navigator.navigateTo("edit-item/$itemId")
            }

            ItemDetailEvent.SaveAndReturnClicked -> {
                // Return result to calling screen's ViewModel and pop backstack
                navigator.navigateBackWithResult(NavigationKeys.KEY_ITEM_UPDATED, true)
            }

            ItemDetailEvent.BackClicked -> {
                // Standard back navigation
                navigator.navigateBack()
            }
        }
    }

    private fun loadItemDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Perform repository call or data fetch
            _uiState.update {
                it.copy(
                    isLoading = false,
                    title = "Sample Item #$itemId",
                )
            }
        }
    }
}

// ── 4. Contract: UiState & Events ───────────────────────────────────────────

data class ItemDetailUiState(
    val itemId: String = "",
    val title: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface ItemDetailEvent {
    data object ScreenResumed : ItemDetailEvent
    data object ScreenPaused : ItemDetailEvent
    data object EditClicked : ItemDetailEvent
    data object SaveAndReturnClicked : ItemDetailEvent
    data object BackClicked : ItemDetailEvent
}

// ── 5. Compose Preview ──────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun ItemDetailScreenPreview() {
    ItemDetailScreen(
        uiState = ItemDetailUiState(
            itemId = "101",
            title = "Preview Stock Item"
        ),
        onEvent = {}
    )
}
