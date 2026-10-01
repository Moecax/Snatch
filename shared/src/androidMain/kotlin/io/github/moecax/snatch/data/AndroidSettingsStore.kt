package io.github.moecax.snatch.data

import android.content.Context
import io.github.moecax.snatch.domain.SettingsStore

class AndroidSettingsStore(context: Context) : SettingsStore {
    private val prefs = context.getSharedPreferences("snatch_settings", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
}
