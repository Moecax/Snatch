package io.github.moecax.snatch.domain

/** Small persistent key/value store; each platform backs it with its native preferences storage. */
interface SettingsStore {
    fun getString(key: String): String?

    /** A null [value] removes the key. */
    fun putString(key: String, value: String?)
}
