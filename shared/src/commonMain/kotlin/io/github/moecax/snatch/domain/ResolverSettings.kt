package io.github.moecax.snatch.domain

/**
 * User-configured resolver endpoint. There's deliberately no built-in default: public Cobalt
 * instances come and go, and the official one doesn't accept third-party callers.
 */
class ResolverSettings(private val store: SettingsStore) {

    val baseUrl: String?
        get() = store.getString(BASE_URL_KEY)?.takeIf { it.isNotBlank() }

    val apiKey: String?
        get() = store.getString(API_KEY_KEY)?.takeIf { it.isNotBlank() }

    val videoQuality: String
        get() = store.getString(VIDEO_QUALITY_KEY)?.takeIf { it in QUALITY_OPTIONS } ?: DEFAULT_VIDEO_QUALITY

    fun save(baseUrl: String, apiKey: String, videoQuality: String) {
        store.putString(BASE_URL_KEY, baseUrl.trim().ifBlank { null })
        store.putString(API_KEY_KEY, apiKey.trim().ifBlank { null })
        store.putString(VIDEO_QUALITY_KEY, videoQuality.takeIf { it in QUALITY_OPTIONS })
    }

    companion object {
        private const val BASE_URL_KEY = "cobaltBaseUrl"
        private const val API_KEY_KEY = "cobaltApiKey"
        private const val VIDEO_QUALITY_KEY = "cobaltVideoQuality"

        const val DEFAULT_VIDEO_QUALITY = "1080"

        /** Cobalt's accepted `videoQuality` values (https://github.com/imputnet/cobalt/blob/main/docs/api.md); "max" means best available. */
        val QUALITY_OPTIONS = listOf("360", "480", "720", "1080", "1440", "2160", "max")

        fun isValidBaseUrl(url: String): Boolean {
            val trimmed = url.trim()
            return (trimmed.startsWith("https://") || trimmed.startsWith("http://")) &&
                trimmed.substringAfter("://").isNotBlank()
        }
    }
}
