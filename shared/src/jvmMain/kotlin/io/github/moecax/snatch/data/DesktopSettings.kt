package io.github.moecax.snatch.data

import java.io.File
import java.util.Properties

class DesktopSettings(
    private val settingsFile: File = File(System.getProperty("user.home"), ".snatch/settings.properties"),
) {
    private val defaultDownloadDirectory = File(System.getProperty("user.home"), "Downloads")

    // Read on every access rather than cached, so a change made in the UI applies to the very next download.
    var downloadDirectory: File
        get() = load().getProperty(DOWNLOAD_DIR_KEY)?.takeIf { it.isNotBlank() }?.let(::File)
            ?: defaultDownloadDirectory
        set(value) {
            val props = load().apply { setProperty(DOWNLOAD_DIR_KEY, value.absolutePath) }
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
