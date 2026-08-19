@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.moecax.snatch.viewmodel

import io.github.moecax.snatch.data.FakeResolver
import io.github.moecax.snatch.domain.ResolverRegistry
import io.github.moecax.snatch.domain.model.AppError
import io.github.moecax.snatch.domain.model.SocialPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DownloadViewModelTest {

    @Test
    fun happyPathWalksThroughAllStates() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = DownloadViewModel(ResolverRegistry(listOf(FakeResolver())))
        assertIs<DownloadUiState.Idle>(viewModel.state.value)

        viewModel.onEvent(DownloadEvent.UrlSubmitted("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertIs<DownloadUiState.Resolving>(viewModel.state.value)

        advanceUntilIdle()

        val ready = viewModel.state.value
        assertIs<DownloadUiState.Ready>(ready)
        assertEquals("1080p", ready.selectedVariantId)

        viewModel.onEvent(DownloadEvent.VariantSelected("720p"))
        assertEquals("720p", (viewModel.state.value as DownloadUiState.Ready).selectedVariantId)

        viewModel.onEvent(DownloadEvent.DownloadClicked)
        advanceUntilIdle()

        assertIs<DownloadUiState.Complete>(viewModel.state.value)

        Dispatchers.resetMain()
    }

    @Test
    fun unsupportedUrlProducesNonRetryableFailedState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = DownloadViewModel(ResolverRegistry(listOf(FakeResolver())))
        viewModel.onEvent(DownloadEvent.UrlSubmitted("https://example.com/some/page"))
        advanceUntilIdle()

        val failed = viewModel.state.value
        assertIs<DownloadUiState.Failed>(failed)
        assertEquals(AppError.UnsupportedPlatform(SocialPlatform.UNKNOWN), failed.error)
        assertEquals(false, failed.retryable)

        Dispatchers.resetMain()
    }
}
