package io.github.moecax.snatch.data

import io.github.moecax.snatch.domain.DownloadProgress
import io.github.moecax.snatch.domain.FileSink
import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.DownloadRequest
import io.github.moecax.snatch.domain.model.MediaType
import io.github.moecax.snatch.domain.model.MediaVariant
import io.github.moecax.snatch.domain.model.ResolvedMedia
import io.github.moecax.snatch.domain.model.SocialPlatform
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeFileSink : FileSink {
    var lastWrittenSize: Int? = null

    override suspend fun write(
        fileName: String,
        mimeType: String,
        mediaType: MediaType,
        bytes: Flow<ByteArray>,
    ): String {
        var size = 0
        bytes.collect { chunk -> size += chunk.size }
        lastWrittenSize = size
        return "fake://$fileName"
    }
}

private class ThrowingFileSink(private val error: Throwable) : FileSink {
    override suspend fun write(
        fileName: String,
        mimeType: String,
        mediaType: MediaType,
        bytes: Flow<ByteArray>,
    ): String {
        bytes.collect { throw error }
        return "unreachable"
    }
}

private fun sampleRequest(url: String = "https://example.com/video.mp4") = DownloadRequest(
    variant = MediaVariant(
        id = "1080p",
        label = "1080p",
        url = url,
        mediaType = MediaType.Video,
        approxSizeBytes = null,
        container = "mp4",
    ),
    resolved = ResolvedMedia(
        sourceUrl = url,
        platform = SocialPlatform.YOUTUBE,
        title = "Sample",
        author = "Creator",
        thumbnailUrl = null,
        variants = emptyList(),
    ),
    suggestedFileName = "sample.mp4",
)

class KtorMediaDownloaderTest {

    @Test
    fun happyPathReportsProgressThenDone() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(ByteArray(10) { it.toByte() }),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Length" to listOf("10")),
            )
        }
        val fileSink = FakeFileSink()
        val downloader = KtorMediaDownloader(HttpClient(mockEngine), fileSink)

        val events = downloader.download(sampleRequest()).toList()

        assertTrue(events.any { it is DownloadProgress.InProgress && it.bytes > 0 && it.total == 10L })
        assertIs<DownloadProgress.Done>(events.last())
        assertEquals("fake://sample.mp4", (events.last() as DownloadProgress.Done).fileUri)
        assertEquals(10, fileSink.lastWrittenSize)
    }

    @Test
    fun networkFailureProducesErrorProgress() = runTest {
        val mockEngine = MockEngine { throw RuntimeException("boom") }
        val downloader = KtorMediaDownloader(HttpClient(mockEngine), FakeFileSink())

        val events = downloader.download(sampleRequest()).toList()

        assertIs<DownloadProgress.Error>(events.last())
        assertEquals(AppError.NoNetwork, (events.last() as DownloadProgress.Error).cause)
    }

    @Test
    fun notFoundResponseProducesErrorAndNeverWrites() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel("Not Found".encodeToByteArray()),
                status = HttpStatusCode.NotFound,
            )
        }
        val fileSink = FakeFileSink()
        val downloader = KtorMediaDownloader(HttpClient(mockEngine), fileSink)

        val events = downloader.download(sampleRequest()).toList()

        assertIs<DownloadProgress.Error>(events.last())
        assertEquals(AppError.MediaUnavailable, (events.last() as DownloadProgress.Error).cause)
        assertNull(fileSink.lastWrittenSize)
    }

    @Test
    fun rateLimitedResponseProducesRateLimitedError() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(ByteArray(0)),
                status = HttpStatusCode.TooManyRequests,
            )
        }
        val fileSink = FakeFileSink()
        val downloader = KtorMediaDownloader(HttpClient(mockEngine), fileSink)

        val events = downloader.download(sampleRequest()).toList()

        assertIs<DownloadProgress.Error>(events.last())
        assertEquals(AppError.RateLimited, (events.last() as DownloadProgress.Error).cause)
        assertNull(fileSink.lastWrittenSize)
    }

    @Test
    fun forbiddenResponseProducesRetryableResolutionFailedError() = runTest {
        val mockEngine = MockEngine {
            respond(content = ByteReadChannel(ByteArray(0)), status = HttpStatusCode.Forbidden)
        }
        val fileSink = FakeFileSink()
        val downloader = KtorMediaDownloader(HttpClient(mockEngine), fileSink)

        val events = downloader.download(sampleRequest()).toList()

        val last = events.last()
        assertIs<DownloadProgress.Error>(last)
        assertIs<AppError.ResolutionFailed>(last.cause)
        assertEquals(true, last.cause.retryable)
        assertNull(fileSink.lastWrittenSize)
    }

    @Test
    fun genuineSinkFailureIsClassifiedAsStorageError() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(ByteArray(10) { it.toByte() }),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Length" to listOf("10")),
            )
        }
        val fileSink = ThrowingFileSink(RuntimeException("disk full"))
        val downloader = KtorMediaDownloader(HttpClient(mockEngine), fileSink)

        val events = downloader.download(sampleRequest()).toList()

        val last = events.last()
        assertIs<DownloadProgress.Error>(last)
        assertIs<AppError.StorageError>(last.cause)
        assertEquals(false, last.cause.retryable)
    }
}
