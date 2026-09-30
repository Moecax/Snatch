package io.github.moecax.snatch.domain

import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.DownloadRequest
import io.github.moecax.snatch.domain.model.MediaType
import io.github.moecax.snatch.domain.model.MediaVariant
import io.github.moecax.snatch.domain.model.ResolvedMedia
import io.github.moecax.snatch.domain.model.SocialPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private val variant = MediaVariant("v", "720p", "https://example.com/v.mp4", MediaType.Video, null, "mp4")
private val request = DownloadRequest(
    variant = variant,
    resolved = ResolvedMedia("https://example.com", SocialPlatform.YOUTUBE, "t", null, null, listOf(variant)),
    suggestedFileName = "t.mp4",
)

private class ScriptedDownloader(private val script: suspend kotlinx.coroutines.flow.FlowCollector<DownloadProgress>.() -> Unit) :
    MediaDownloader {
    override fun download(request: DownloadRequest): Flow<DownloadProgress> = flow(script)
}

class DownloadManagerTest {

    @Test
    fun observerSeesProgressThroughTerminalAndCompletes() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        val manager = DownloadManager(
            ScriptedDownloader {
                emit(DownloadProgress.InProgress(50, 100))
                emit(DownloadProgress.Done("content://saved"))
            },
            scope,
        )

        val seen = manager.download(request).toList()

        assertEquals(DownloadProgress.Done("content://saved"), seen.last())
        scope.coroutineContext[Job]!!.cancel()
    }

    @Test
    fun downloadKeepsRunningAfterObserverStopsCollecting() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        val manager = DownloadManager(
            ScriptedDownloader {
                emit(DownloadProgress.InProgress(1, 2))
                emit(DownloadProgress.Done("content://saved"))
            },
            scope,
        )

        manager.download(request).first()
        advanceUntilIdle()

        val tracked = manager.downloads.value.values.single()
        assertEquals(DownloadProgress.Done("content://saved"), tracked.progress)
        scope.coroutineContext[Job]!!.cancel()
    }

    @Test
    fun thrownExceptionBecomesErrorProgress() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        val manager = DownloadManager(ScriptedDownloader { error("boom") }, scope)

        val last = manager.download(request).toList().last()

        assertIs<DownloadProgress.Error>(last)
        assertIs<AppError.Unknown>(last.cause)
        scope.coroutineContext[Job]!!.cancel()
    }

    @Test
    fun acknowledgeRemovesFinishedButKeepsActiveDownloads() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        var started = 0
        val manager = DownloadManager(
            ScriptedDownloader {
                emit(DownloadProgress.InProgress(1, 2))
                kotlinx.coroutines.awaitCancellation()
            },
            scope,
            onDownloadStarted = { started++ },
        )

        manager.download(request)
        advanceUntilIdle()
        val id = manager.downloads.value.keys.single()
        manager.acknowledge(id)

        assertEquals(1, started)
        assertTrue(manager.downloads.value.containsKey(id))
        scope.coroutineContext[Job]!!.cancel()
    }
}
