package io.github.moecax.snatch.viewmodel

import io.github.moecax.snatch.data.InMemorySettingsStore
import io.github.moecax.snatch.domain.ResolverSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun savingAValidUrlPersistsTrimmedValues() {
        assertTrue(viewModel.save("  https://cobalt.mock.test/  ", " key "))

        assertEquals("https://cobalt.mock.test/", settings.baseUrl)
        assertEquals("key", settings.apiKey)
        assertTrue(viewModel.state.value.isConfigured)
    }

    @Test
    fun invalidUrlIsRejectedAndNothingIsSaved() {
        assertFalse(viewModel.save("cobalt.mock.test", ""))

        assertNull(settings.baseUrl)
        assertFalse(viewModel.state.value.isConfigured)
    }

    @Test
    fun blankUrlClearsTheSetting() {
        viewModel.save("https://cobalt.mock.test", "key")

        assertTrue(viewModel.save("", ""))

        assertNull(settings.baseUrl)
        assertNull(settings.apiKey)
        assertFalse(viewModel.state.value.isConfigured)
    }
}
