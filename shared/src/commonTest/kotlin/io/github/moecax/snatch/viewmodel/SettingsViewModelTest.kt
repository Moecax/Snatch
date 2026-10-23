@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.moecax.snatch.viewmodel

import io.github.moecax.snatch.data.InMemorySettingsStore
import io.github.moecax.snatch.domain.AppErrorException
import io.github.moecax.snatch.domain.ResolverSettings
import io.github.moecax.snatch.domain.model.AppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsViewModelTest {

    private val settings = ResolverSettings(InMemorySettingsStore())
    private val viewModel = SettingsViewModel(settings)

    @Test
    fun startsUnconfigured() {
        assertFalse(viewModel.state.value.isConfigured)
        assertNull(settings.baseUrl)
    }

    @Test
    fun startsWithTheDefaultVideoQuality() {
        assertEquals(ResolverSettings.DEFAULT_VIDEO_QUALITY, viewModel.state.value.videoQuality)
    }

    @Test
    fun savingAValidUrlPersistsTrimmedValuesAndTheChosenQuality() {
        assertTrue(viewModel.save("  https://cobalt.mock.test/  ", " key ", "720"))

        assertEquals("https://cobalt.mock.test/", settings.baseUrl)
        assertEquals("key", settings.apiKey)
        assertEquals("720", settings.videoQuality)
        assertTrue(viewModel.state.value.isConfigured)
        assertEquals("720", viewModel.state.value.videoQuality)
    }

    @Test
    fun invalidUrlIsRejectedAndNothingIsSaved() {
        assertFalse(viewModel.save("cobalt.mock.test", "", ResolverSettings.DEFAULT_VIDEO_QUALITY))

        assertNull(settings.baseUrl)
        assertFalse(viewModel.state.value.isConfigured)
    }

    @Test
    fun blankUrlClearsTheSetting() {
        viewModel.save("https://cobalt.mock.test", "key", "1080")

        assertTrue(viewModel.save("", "", "1080"))

        assertNull(settings.baseUrl)
        assertNull(settings.apiKey)
        assertFalse(viewModel.state.value.isConfigured)
    }

    @Test
    fun testReportsSuccessFromTheInjectedConnectionCheck() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = SettingsViewModel(settings, testConnection = { _, _ -> Result.success("10.2.0") })

        viewModel.test("https://cobalt.mock.test", "")
        advanceUntilIdle()

        assertEquals(ConnectionTestResult.Success("10.2.0"), viewModel.testResult.value)
        Dispatchers.resetMain()
    }

    @Test
    fun testReportsTheMappedAppErrorOnFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = SettingsViewModel(
            settings,
            testConnection = { _, _ -> Result.failure(AppErrorException(AppError.ResolverAuthRequired)) },
        )

        viewModel.test("https://cobalt.mock.test", "")
        advanceUntilIdle()

        assertEquals(ConnectionTestResult.Failure(AppError.ResolverAuthRequired), viewModel.testResult.value)
        Dispatchers.resetMain()
    }

    @Test
    fun testRejectsAnInvalidUrlWithoutCallingTheConnectionCheck() {
        var called = false
        val viewModel = SettingsViewModel(settings, testConnection = { _, _ -> called = true; Result.success("") })

        viewModel.test("not-a-url", "")

        assertEquals(ConnectionTestResult.Failure(AppError.InvalidUrl), viewModel.testResult.value)
        assertFalse(called)
    }

    @Test
    fun clearTestResultResetsItToNull() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = SettingsViewModel(settings, testConnection = { _, _ -> Result.success("1.0") })
        viewModel.test("https://cobalt.mock.test", "")
        advanceUntilIdle()
        assertIs<ConnectionTestResult.Success>(viewModel.testResult.value)

        viewModel.clearTestResult()

        assertNull(viewModel.testResult.value)
        Dispatchers.resetMain()
    }
}
