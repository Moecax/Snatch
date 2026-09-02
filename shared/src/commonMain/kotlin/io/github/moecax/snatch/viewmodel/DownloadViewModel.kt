package io.github.moecax.snatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.moecax.snatch.domain.DownloadProgress
import io.github.moecax.snatch.domain.MediaDownloader
import io.github.moecax.snatch.domain.ResolverRegistry
import io.github.moecax.snatch.domain.appErrorOrNull
import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.DownloadRequest
import io.github.moecax.snatch.domain.model.MediaVariant
import io.github.moecax.snatch.domain.model.ResolvedMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class DownloadViewModel(
    private val resolverRegistry: ResolverRegistry,
    private val mediaDownloader: MediaDownloader,
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
        val request = DownloadRequest(
            variant = variant,
            resolved = current.media,
            suggestedFileName = suggestedFileName(current.media, variant),
        )
        viewModelScope.launch {
            mediaDownloader.download(request).collect { progress ->
                _state.value = when (progress) {
                    is DownloadProgress.InProgress -> DownloadUiState.Downloading(
                        media = current.media,
                        variant = variant,
                        bytesDownloaded = progress.bytes,
                        totalBytes = progress.total,
                    )
                    is DownloadProgress.Done -> DownloadUiState.Complete(
                        fileName = request.suggestedFileName,
                        location = progress.fileUri,
                    )
                    is DownloadProgress.Error -> DownloadUiState.Failed(
                        error = progress.cause,
                        retryable = progress.cause.retryable,
                    )
                }
            }
        }
    }

    private fun suggestedFileName(media: ResolvedMedia, variant: MediaVariant): String {
        val base = media.title?.ifBlank { null } ?: variant.id
        val sanitized = base.map { if (it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_') it else '_' }.joinToString("")
        val extension = variant.container ?: "bin"
        return "$sanitized.$extension"
    }
}
