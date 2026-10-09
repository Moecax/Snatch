package io.github.moecax.snatch

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.moecax.snatch.di.AppContainer
import io.github.moecax.snatch.ui.MainScreen
import io.github.moecax.snatch.ui.SettingsDialog
import io.github.moecax.snatch.ui.theme.SnatchTheme
import io.github.moecax.snatch.viewmodel.DownloadEvent
import io.github.moecax.snatch.viewmodel.DownloadViewModel
import io.github.moecax.snatch.viewmodel.SettingsViewModel

@Composable
fun App(
    container: AppContainer,
    // Lets a platform intercept events (e.g. Android asks for notification permission before a download starts).
    interceptEvents: @Composable (onEvent: (DownloadEvent) -> Unit) -> (DownloadEvent) -> Unit = { it },
) {
    SnatchTheme {
        // Scoped to the real ViewModelStoreOwner (not `remember`) so the ViewModel and the
        // HttpClient it owns survive configuration changes instead of leaking a new instance
        // per recreation, and get torn down via onCleared() when the owner actually finishes.
        val viewModel: DownloadViewModel = viewModel { container.createDownloadViewModel() }
        val settingsViewModel: SettingsViewModel = viewModel { container.createSettingsViewModel() }
        val state by viewModel.state.collectAsStateWithLifecycle()
        val settings by settingsViewModel.state.collectAsStateWithLifecycle()
        var showSettings by rememberSaveable { mutableStateOf(false) }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = state,
                onEvent = interceptEvents(viewModel::onEvent),
                resolverConfigured = settings.isConfigured,
                onOpenSettings = { showSettings = true },
            )
        }
        if (showSettings) {
            SettingsDialog(
                state = settings,
                onSave = settingsViewModel::save,
                onDismiss = { showSettings = false },
            )
        }
    }
}
