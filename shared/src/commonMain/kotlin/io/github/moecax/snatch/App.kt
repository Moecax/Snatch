package io.github.moecax.snatch

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.moecax.snatch.di.AppContainer
import io.github.moecax.snatch.ui.MainScreen

@Composable
fun App(container: AppContainer) {
    MaterialTheme {
        val viewModel = remember { container.createDownloadViewModel() }
        val state by viewModel.state.collectAsStateWithLifecycle()
        MainScreen(state = state, onEvent = viewModel::onEvent)
    }
}
