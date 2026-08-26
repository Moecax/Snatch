package io.github.moecax.snatch.domain

import io.github.moecax.snatch.domain.model.DownloadRequest
import kotlinx.coroutines.flow.Flow

interface MediaDownloader {
    fun download(request: DownloadRequest): Flow<DownloadProgress>
}
