package me.liwenkun.actionbuttonpro.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import me.liwenkun.actionbuttonpro.systemserver.log

private const val SETTINGS_FILE_NAME = "settings"
private const val SETTINGS_KEY = "settings_json"

class SystemServerSettingStore(context: Context) : SyncRemoteSharedPreferenceSettingStore(
    localSharedPreference = context.getSharedPreferences(SETTINGS_FILE_NAME, Context.MODE_PRIVATE),
    remoteSharedPreference = RemoteSharedPreference(context, SETTINGS_FILE_NAME)
)

class AppSettingStore(context: Context) : SharedPreferencesSettingStore(
    sharedPreferences = RemoteSharedPreference(context, SETTINGS_FILE_NAME)
)


sealed interface SettingStore {
    fun getSettings(): Settings
    fun saveSettings(settings: Settings)
}

open class SyncRemoteSharedPreferenceSettingStore(
    localSharedPreference: SharedPreferences,
    remoteSharedPreference: SharedPreferences,
):  SharedPreferencesSettingStore(localSharedPreference) {

    val listener = SharedPreferences.OnSharedPreferenceChangeListener  { _, _ ->
        remoteSharedPreference.loadSettings().also {
            saveSettings(it)
            log("sync remote shared preference to local: $it")
        }
    }
    init {
        remoteSharedPreference.registerOnSharedPreferenceChangeListener(listener)
    }
}

open class SharedPreferencesSettingStore(
    val sharedPreferences: SharedPreferences
): SettingStore {

    private var settings: Settings? = null
    override fun getSettings(): Settings = settings ?: sharedPreferences.loadSettings().also {
        settings = it
    }

    override fun saveSettings(settings: Settings) = sharedPreferences.saveSettings(settings).also {
        this.settings = settings
    }

}

private fun SharedPreferences.loadSettings(): Settings {
    return getString(SETTINGS_KEY, "").let {
        Settings.fromJson(it!!)
    }
}

private fun SharedPreferences.saveSettings(settings: Settings) {
    edit { putString(SETTINGS_KEY, settings.toJson()) }
}