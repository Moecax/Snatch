package io.github.moecax.snatch.viewmodel

import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.MediaVariant
import io.github.moecax.snatch.domain.model.ResolvedMedia

sealed interface DownloadUiState {
    data object Idle : DownloadUiState

    data class Resolving(val url: String) : DownloadUiState

    data class Ready(
        val media: ResolvedMedia,
        val selectedVariantId: String,
    ) : DownloadUiState

    data class Downloading(
        val media: ResolvedMedia,
        val variant: MediaVariant,
        val bytesDownloaded: Long,
        val totalBytes: Long?,
    ) : DownloadUiState

    data class Complete(
        val fileName: String,
        val location: String,
    ) : DownloadUiState

    data class Failed(
        val error: AppError,
        val retryable: Boolean,
    ) : DownloadUiState
}
