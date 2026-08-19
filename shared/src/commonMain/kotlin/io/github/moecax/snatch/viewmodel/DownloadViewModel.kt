package io.github.moecax.snatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.moecax.snatch.domain.ResolverRegistry
import io.github.moecax.snatch.domain.appErrorOrNull
import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.MediaVariant
import io.github.moecax.snatch.domain.model.ResolvedMedia
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadViewModel(
    private val resolverRegistry: ResolverRegistry,
) : ViewModel() {

    private val _state = MutableStateFlow<DownloadUiState>(DownloadUiState.Idle)
    val state: StateFlow<DownloadUiState> = _state.asStateFlow()

    private var lastSubmittedUrl: String? = null

    fun onEvent(event: DownloadEvent) {
        when (event) {
            is DownloadEvent.UrlSubmitted -> resolve(event.url)
            is DownloadEvent.VariantSelected -> selectVariant(event.id)
            DownloadEvent.DownloadClicked -> startDownload()
            DownloadEvent.Retry -> lastSubmittedUrl?.let { resolve(it) }
            DownloadEvent.Reset -> _state.value = DownloadUiState.Idle
        }
    }

    private fun resolve(url: String) {
        lastSubmittedUrl = url
        _state.value = DownloadUiState.Resolving(url)
        viewModelScope.launch {
            val result = resolverRegistry.resolve(url)
            _state.value = if (result.isSuccess) {
                val media = result.getOrThrow()
                DownloadUiState.Ready(media, media.variants.firstOrNull()?.id.orEmpty())
            } else {
                val error = result.appErrorOrNull() ?: AppError.Unknown(result.exceptionOrNull())
                DownloadUiState.Failed(error, error.retryable)
            }
        }
    }

    private fun selectVariant(id: String) {
        val current = _state.value
        if (current is DownloadUiState.Ready) {
            _state.value = current.copy(selectedVariantId = id)
        }
    }

    private fun startDownload() {
        val current = _state.value
        if (current !is DownloadUiState.Ready) return
        val variant = current.media.variants.firstOrNull { it.id == current.selectedVariantId } ?: return
        viewModelScope.launch {
            simulateDownload(current.media, variant)
        }
    }

    private suspend fun simulateDownload(media: ResolvedMedia, variant: MediaVariant) {
        val totalBytes = variant.approxSizeBytes ?: 10_000_000L
        val steps = 5
        for (step in 1..steps) {
            delay(200)
            _state.value = DownloadUiState.Downloading(
                media = media,
                variant = variant,
                bytesDownloaded = totalBytes * step / steps,
                totalBytes = totalBytes,
            )
        }
        _state.value = DownloadUiState.Complete(
            fileName = suggestedFileName(media, variant),
            location = "Downloads",
        )
    }

    private fun suggestedFileName(media: ResolvedMedia, variant: MediaVariant): String {
        val base = media.title?.ifBlank { null } ?: variant.id
        val sanitized = base.map { if (it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_') it else '_' }.joinToString("")
        val extension = variant.container ?: "bin"
        return "$sanitized.$extension"
    }
}
