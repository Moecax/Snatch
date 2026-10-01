package io.github.moecax.snatch.data

import io.github.moecax.snatch.domain.SettingsStore
import java.io.File
import java.util.Properties

class DesktopSettings(
    private val settingsFile: File = File(System.getProperty("user.home"), ".snatch/settings.properties"),
) : SettingsStore {
    private val defaultDownloadDirectory = File(System.getProperty("user.home"), "Downloads")

    // Read on every access rather than cached, so a change made in the UI applies to the very next download.
    var downloadDirectory: File
        get() = getString(DOWNLOAD_DIR_KEY)?.takeIf { it.isNotBlank() }?.let(::File)
            ?: defaultDownloadDirectory
        set(value) = putString(DOWNLOAD_DIR_KEY, value.absolutePath)

    override fun getString(key: String): String? = load().getProperty(key)

    override fun putString(key: String, value: String?) {
        val props = load().apply { if (value == null) remove(key) else setProperty(key, value) }
        settingsFile.parentFile?.mkdirs()
        settingsFile.outputStream().use { props.store(it, null) }
    }

    // A missing or unreadable file just means "no overrides yet" — never fail a download over settings.
    private fun load(): Properties = Properties().apply {
        runCatching { if (settingsFile.exists()) settingsFile.inputStream().use(::load) }
    }

    private companion object {
        const val DOWNLOAD_DIR_KEY = "downloadDirectory"
    }
}
