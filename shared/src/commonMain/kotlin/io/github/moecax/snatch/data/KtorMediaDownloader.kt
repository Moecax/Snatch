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
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow

private const val CHUNK_SIZE_BYTES = 8 * 1024

/**
 * Marks an exception as originating from the network-read side of the byte-producing
 * flow (as opposed to [fileSink]'s own I/O), so the outer catch can classify it as a
 * retryable transport failure instead of a non-retryable storage failure.
 */
private class TransportException(cause: Throwable) : Exception(cause)

class KtorMediaDownloader(
    private val httpClient: HttpClient,
    private val fileSink: FileSink,
) : MediaDownloader {

    override fun download(request: DownloadRequest): Flow<DownloadProgress> = channelFlow {
        val emitProgress: suspend (DownloadProgress) -> Unit = { send(it) }
        try {
            httpClient.prepareGet(request.variant.url).execute { response ->
                if (!response.status.isSuccess()) {
                    emitProgress(DownloadProgress.Error(errorFor(response.status)))
                    return@execute
                }
                val total = response.contentLength() ?: request.variant.approxSizeBytes
                val channel = response.bodyAsChannel()
                var bytesRead = 0L
                val bytes = flow {
                    val buffer = ByteArray(CHUNK_SIZE_BYTES)
                    while (true) {
                        val read = try {
                            channel.readAvailable(buffer)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            throw TransportException(e)
                        }
                        if (read == -1) break
                        if (read == 0) continue
                        bytesRead += read
                        emit(buffer.copyOf(read))
                        emitProgress(DownloadProgress.InProgress(bytesRead, total))
                    }
                    // A channel closed mid-stream by a dropped connection surfaces as a
                    // clean readAvailable == -1, not a thrown exception — closedCause is
                    // the only signal that this wasn't a normal end of stream.
                    channel.closedCause?.let { throw TransportException(it) }
                }
                val fileUri = try {
                    fileSink.write(request.suggestedFileName, mimeTypeFor(request.variant), request.variant.mediaType, bytes)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: TransportException) {
                    emitProgress(DownloadProgress.Error(AppError.NoNetwork))
                    return@execute
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

private fun errorFor(status: HttpStatusCode): AppError = when (status.value) {
    429 -> AppError.RateLimited
    404, 410 -> AppError.MediaUnavailable
    else -> AppError.NoNetwork
}

private fun mimeTypeFor(variant: MediaVariant): String {
    val container = variant.container?.lowercase()
    return when (variant.mediaType) {
        MediaType.Video -> when (container) {
            "mp4", "m4v", null -> "video/mp4"
            "webm" -> "video/webm"
            else -> "application/octet-stream"
        }
        MediaType.Image -> when (container) {
            "jpg", "jpeg", null -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/octet-stream"
        }
        MediaType.Audio -> when (container) {
            "mp3", null -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            else -> "application/octet-stream"
        }
        MediaType.Gallery -> "application/octet-stream"
    }
}
