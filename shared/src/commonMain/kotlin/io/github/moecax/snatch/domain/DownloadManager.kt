package io.github.moecax.snatch.domain

import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.DownloadRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrackedDownload(
    val id: Long,
    val request: DownloadRequest,
    val progress: DownloadProgress,
)

/**
 * Runs downloads in [scope] rather than in the caller's, so a download outlives the ViewModel
 * (or Activity) that started it. The flow returned from [download] is just an observer: cancelling
 * it does not cancel the download. [downloads] exposes every download for out-of-UI observers such
 * as the Android foreground service.
 */
class DownloadManager(
    private val downloader: MediaDownloader,
    private val scope: CoroutineScope,
    private val onDownloadStarted: () -> Unit = {},
) : MediaDownloader {

    private val _downloads = MutableStateFlow<Map<Long, TrackedDownload>>(emptyMap())
    val downloads: StateFlow<Map<Long, TrackedDownload>> = _downloads.asStateFlow()

    override fun download(request: DownloadRequest): Flow<DownloadProgress> {
        val initial = DownloadProgress.InProgress(bytes = 0, total = null)
        val progress = MutableStateFlow<DownloadProgress>(initial)
        var id = 0L
        _downloads.update { current ->
            id = (current.keys.maxOrNull() ?: -1L) + 1
            current + (id to TrackedDownload(id, request, initial))
        }

        fun publish(value: DownloadProgress) {
            progress.value = value
            _downloads.update { current ->
                val tracked = current[id] ?: return@update current
                current + (id to tracked.copy(progress = value))
            }
        }

        scope.launch {
            try {
                downloader.download(request).collect { publish(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                publish(DownloadProgress.Error(AppError.Unknown(e)))
            }
            if (progress.value is DownloadProgress.InProgress) {
                publish(DownloadProgress.Error(AppError.Unknown(null)))
            }
        }
        // Started after the entry is registered so an observer woken by the hook already sees it.
        onDownloadStarted()

        return progress.transformWhile { emit(it); it is DownloadProgress.InProgress }
    }

    /** Drops a finished download once an observer has surfaced its result. */
    fun acknowledge(id: Long) {
        _downloads.update { current ->
            if (current[id]?.progress is DownloadProgress.InProgress) current else current - id
        }
    }
}
