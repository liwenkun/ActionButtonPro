package me.liwenkun.actionbuttonpro.ui

import me.liwenkun.actionbuttonpro.R

/**
 * 集中定义所有预设 ACTION，标签已国际化。
 *
 * 新增只需在此列表加一项，UI 自动渲染，无需修改其他代码。
 */
enum class PresetActions(
    val labelResId: Int,
    val action: String
) {
    // ── 媒体 ──
    IMAGE_CAPTURE(R.string.preset_image_capture, "android.media.action.IMAGE_CAPTURE"),
    VIDEO_CAPTURE(R.string.preset_video_capture, "android.media.action.VIDEO_CAPTURE"),

    // ── 系统工具 ──
    ASSISTANT(R.string.preset_assistant, "android.intent.action.ASSIST"),
    WEB_SEARCH(R.string.preset_web_search, "android.speech.action.WEB_SEARCH"),
    SETTINGS(R.string.preset_settings, "android.settings.SETTINGS"),
    APPLICATION_SETTINGS(R.string.preset_app_settings, "android.settings.APPLICATION_SETTINGS"),
    DATE_SETTINGS(R.string.preset_date_settings, "android.settings.DATE_SETTINGS"),
    DEVELOPMENT_SETTINGS(R.string.preset_dev_settings, "android.settings.APPLICATION_DEVELOPMENT_SETTINGS"),
    POWER_USAGE(R.string.preset_power_usage, "android.intent.action.POWER_USAGE_SUMMARY");

    companion object {
        /** 从 action 字符串反向查找预设，未找到返回 null */
        fun findByAction(action: String): PresetActions? =
            entries.find { it.action == action }
    }
}