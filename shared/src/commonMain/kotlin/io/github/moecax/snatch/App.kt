package io.github.moecax.snatch

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.moecax.snatch.di.AppContainer
import io.github.moecax.snatch.ui.MainScreen
import io.github.moecax.snatch.viewmodel.DownloadEvent
import io.github.moecax.snatch.viewmodel.DownloadViewModel

@Composable
fun App(
    container: AppContainer,
    // Lets a platform intercept events (e.g. Android asks for notification permission before a download starts).
    interceptEvents: @Composable (onEvent: (DownloadEvent) -> Unit) -> (DownloadEvent) -> Unit = { it },
) {
    MaterialTheme {
        // Scoped to the real ViewModelStoreOwner (not `remember`) so the ViewModel and the
        // HttpClient it owns survive configuration changes instead of leaking a new instance
        // per recreation, and get torn down via onCleared() when the owner actually finishes.
        val viewModel: DownloadViewModel = viewModel { container.createDownloadViewModel() }
        val state by viewModel.state.collectAsStateWithLifecycle()
        MainScreen(state = state, onEvent = interceptEvents(viewModel::onEvent))
    }
}
