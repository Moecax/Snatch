package io.github.moecax.snatch.data

import io.github.moecax.snatch.domain.SettingsStore
import kotlinx.browser.localStorage

class WebSettingsStore : SettingsStore {
    override fun getString(key: String): String? = localStorage.getItem(PREFIX + key)

    override fun putString(key: String, value: String?) {
        if (value == null) localStorage.removeItem(PREFIX + key) else localStorage.setItem(PREFIX + key, value)
    }

    private companion object {
        const val PREFIX = "snatch."
    }
}
