package io.github.moecax.snatch.data

import io.github.moecax.snatch.domain.DownloadProgress
import io.github.moecax.snatch.domain.FileSink
import io.github.moecax.snatch.domain.MediaDownloader
import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.DownloadRequest
import io.github.moecax.snatch.domain.model.MediaType
import io.github.moecax.snatch.domain.model.MediaVariant
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow

private const val CHUNK_SIZE_BYTES = 8 * 1024

class KtorMediaDownloader(
    private val httpClient: HttpClient,
    private val fileSink: FileSink,
) : MediaDownloader {

    override fun download(request: DownloadRequest): Flow<DownloadProgress> = channelFlow {
        val emitProgress: suspend (DownloadProgress) -> Unit = { send(it) }
        try {
            httpClient.prepareGet(request.variant.url).execute { response ->
                val total = response.contentLength() ?: request.variant.approxSizeBytes
                val channel = response.bodyAsChannel()
                var bytesRead = 0L
                val bytes = flow {
                    val buffer = ByteArray(CHUNK_SIZE_BYTES)
                    while (true) {
                        val read = channel.readAvailable(buffer)
                        if (read == -1) break
                        if (read == 0) continue
                        bytesRead += read
                        emit(buffer.copyOf(read))
                        emitProgress(DownloadProgress.InProgress(bytesRead, total))
                    }
                }
                val fileUri = try {
                    fileSink.write(request.suggestedFileName, mimeTypeFor(request.variant), request.variant.mediaType, bytes)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emitProgress(DownloadProgress.Error(AppError.StorageError(e.message)))
                    return@execute
                }
                emitProgress(DownloadProgress.Done(fileUri))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emitProgress(DownloadProgress.Error(AppError.NoNetwork))
        }
    }
}

private fun mimeTypeFor(variant: MediaVariant): String = when (variant.mediaType) {
    MediaType.Video -> "video/${variant.container ?: "mp4"}"
    MediaType.Image -> "image/${variant.container ?: "jpeg"}"
    MediaType.Audio -> "audio/${variant.container ?: "mpeg"}"
    MediaType.Gallery -> "application/octet-stream"
}
