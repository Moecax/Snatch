package io.github.moecax.snatch.domain

import io.github.moecax.snatch.domain.model.AppError

sealed interface DownloadProgress {
    data class InProgress(val bytes: Long, val total: Long?) : DownloadProgress
    data class Done(val fileUri: String) : DownloadProgress
    data class Error(val cause: AppError) : DownloadProgress
}
