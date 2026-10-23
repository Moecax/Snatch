package io.github.moecax.snatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.moecax.snatch.domain.ResolverSettings
import io.github.moecax.snatch.domain.appErrorOrNull
import io.github.moecax.snatch.domain.model.AppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(val baseUrl: String, val apiKey: String, val videoQuality: String) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank()
}

sealed interface ConnectionTestResult {
    data object Testing : ConnectionTestResult
    data class Success(val versionLabel: String) : ConnectionTestResult
    data class Failure(val error: AppError) : ConnectionTestResult
}

class SettingsViewModel(
    private val settings: ResolverSettings,
    private val testConnection: suspend (baseUrl: String, apiKey: String?) -> Result<String> = { _, _ -> Result.success("") },
) : ViewModel() {

    private val _state = MutableStateFlow(currentState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _testResult = MutableStateFlow<ConnectionTestResult?>(null)
    val testResult: StateFlow<ConnectionTestResult?> = _testResult.asStateFlow()

    /** Returns false (and saves nothing) if [baseUrl] is non-blank but not an http(s) URL. */
    fun save(baseUrl: String, apiKey: String, videoQuality: String): Boolean {
        if (baseUrl.isNotBlank() && !ResolverSettings.isValidBaseUrl(baseUrl)) return false
        settings.save(baseUrl, apiKey, videoQuality)
        _state.value = currentState()
        return true
    }

    /** Tests whatever is currently typed into the dialog, not necessarily what's saved. */
    fun test(baseUrl: String, apiKey: String) {
        if (!ResolverSettings.isValidBaseUrl(baseUrl)) {
            _testResult.value = ConnectionTestResult.Failure(AppError.InvalidUrl)
            return
        }
        _testResult.value = ConnectionTestResult.Testing
        viewModelScope.launch {
            val result = testConnection(baseUrl, apiKey.ifBlank { null })
            _testResult.value = if (result.isSuccess) {
                ConnectionTestResult.Success(result.getOrThrow())
            } else {
                ConnectionTestResult.Failure(result.appErrorOrNull() ?: AppError.Unknown(result.exceptionOrNull()))
            }
        }
    }

    fun clearTestResult() {
        _testResult.value = null
    }

    private fun currentState() = SettingsUiState(settings.baseUrl.orEmpty(), settings.apiKey.orEmpty(), settings.videoQuality)
}
