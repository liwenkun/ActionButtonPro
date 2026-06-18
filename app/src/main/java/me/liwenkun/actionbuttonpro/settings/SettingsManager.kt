package me.liwenkun.actionbuttonpro.settings

object SettingsManager: SettingStore  {

    private lateinit var settingStore: SettingStore
    fun init(settingStore: SettingStore) {
        this.settingStore = settingStore
    }

    fun update(change: Settings.() -> Settings) {
        saveSettings(settingStore.getSettings().change())
    }

    override fun getSettings(): Settings = settingStore.getSettings()

    override fun saveSettings(settings: Settings) = settingStore.saveSettings(settings)
}
