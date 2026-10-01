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

    fun save(baseUrl: String, apiKey: String) {
        store.putString(BASE_URL_KEY, baseUrl.trim().ifBlank { null })
        store.putString(API_KEY_KEY, apiKey.trim().ifBlank { null })
    }

    companion object {
        private const val BASE_URL_KEY = "cobaltBaseUrl"
        private const val API_KEY_KEY = "cobaltApiKey"

        fun isValidBaseUrl(url: String): Boolean {
            val trimmed = url.trim()
            return (trimmed.startsWith("https://") || trimmed.startsWith("http://")) &&
                trimmed.substringAfter("://").isNotBlank()
        }
    }
}
