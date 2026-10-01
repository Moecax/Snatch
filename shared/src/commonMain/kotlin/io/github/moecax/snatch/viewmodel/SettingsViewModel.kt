package io.github.moecax.snatch.viewmodel

import androidx.lifecycle.ViewModel
import io.github.moecax.snatch.domain.ResolverSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(val baseUrl: String, val apiKey: String) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank()
}

class SettingsViewModel(private val settings: ResolverSettings) : ViewModel() {

    private val _state = MutableStateFlow(currentState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    /** Returns false (and saves nothing) if [baseUrl] is non-blank but not an http(s) URL. */
    fun save(baseUrl: String, apiKey: String): Boolean {
        if (baseUrl.isNotBlank() && !ResolverSettings.isValidBaseUrl(baseUrl)) return false
        settings.save(baseUrl, apiKey)
        _state.value = currentState()
        return true
    }

    private fun currentState() = SettingsUiState(settings.baseUrl.orEmpty(), settings.apiKey.orEmpty())
}
