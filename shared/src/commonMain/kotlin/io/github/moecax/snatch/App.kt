package io.github.moecax.snatch

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.moecax.snatch.di.AppContainer
import io.github.moecax.snatch.ui.MainScreen
import io.github.moecax.snatch.viewmodel.DownloadViewModel

@Composable
fun App(container: AppContainer) {
    MaterialTheme {
        // Scoped to the real ViewModelStoreOwner (not `remember`) so the ViewModel and the
        // HttpClient it owns survive configuration changes instead of leaking a new instance
        // per recreation, and get torn down via onCleared() when the owner actually finishes.
        val viewModel: DownloadViewModel = viewModel { container.createDownloadViewModel() }
        val state by viewModel.state.collectAsStateWithLifecycle()
        MainScreen(state = state, onEvent = viewModel::onEvent)
    }
}
