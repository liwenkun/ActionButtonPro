package me.liwenkun.actionbuttonpro.settings

import me.liwenkun.actionbuttonpro.ui.CustomAction
import org.json.JSONObject

// ═══════════════════════════════════════════════════════════
// GestureSettings — 单个 Gesture（单击 / 双击 / 长按）的配置
// ═══════════════════════════════════════════════════════════

data class GestureSettings(
    /** 是否启用此手势 */
    val enabled: Boolean = false,
    /** 执行模式："action" / "shell" */
    val activeMode: String = "action",
    /** Action 命令（preset 或自定义 Action 字符串） */
    val action: String = "android.media.action.IMAGE_CAPTURE",
    /** Shell 命令行 */
    val shellCommand: String = "",
    /** 是否展示命令输出结果 */
    val showShellOutput: Boolean = false,
    /** 输出展示形式："toast" / "notification" */
    val shellOutputFormat: String = "toast",
    val passThrough: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("enabled", enabled)
        put("active_mode", activeMode)
        put("action", action)
        put("shell_command", shellCommand)
        put("show_shell_output", showShellOutput)
        put("shell_output_format", shellOutputFormat)
        put("pass_through", passThrough)
    }

    companion object {
        fun fromJson(json: JSONObject): GestureSettings = GestureSettings(
            enabled = json.optBoolean("enabled", false),
            activeMode = json.optString("active_mode", "action"),
            action = json.optString("action", "android.media.action.IMAGE_CAPTURE"),
            shellCommand = json.optString("shell_command", ""),
            showShellOutput = json.optBoolean("show_shell_output", false),
            shellOutputFormat = json.optString("shell_output_format", "toast"),
            passThrough = json.optBoolean("pass_through", false),
        )
    }
}

// ═══════════════════════════════════════════════════════════
// Settings — 顶层设置（三 Gesture 独立配置 + 共享 customActions）
// ═══════════════════════════════════════════════════════════

data class Settings(
    /** 总开关 */
    val enabled: Boolean = false,
    /** 是否已弹出过使用须知 */
    val hasShownInstructions: Boolean = false,
    /** 单击配置 */
    val singlePress: GestureSettings = GestureSettings(),
    /** 双击配置 */
    val doublePress: GestureSettings = GestureSettings(),
    /** 长按配置 */
    val longPress: GestureSettings = GestureSettings(),
    /** 用户选择的默认数字助理包名 */
    val assistantPackage: String = "",
    /** 自定义 Action 列表（跨 Gesture 共享） */
    val customActions: List<CustomAction> = emptyList(),
    /** 双击检测超时时长 */
    val doubleClickTimeoutMs: Int = 400,
    /** 抖动屏蔽时长 */
    val debounceTimeoutMs: Int = 550,
    /** 是否开启长按震动 */
    val vibrateOnLongPress: Boolean = false,
) {
    /** 序列化为 JSON 字符串 */
    fun toJson(): String = JSONObject().apply {
        put("enabled", enabled)
        put("has_shown_instructions", hasShownInstructions)
        put("single_press", singlePress.toJson())
        put("double_press", doublePress.toJson())
        put("long_press", longPress.toJson())
        put("assistant_package", assistantPackage)
        put("custom_actions", CustomAction.listToJsonArray(customActions))
        put("double_click_timeout_ms", doubleClickTimeoutMs)
        put("debounce_timeout_ms", debounceTimeoutMs)
        put("vibrate_on_long_press", vibrateOnLongPress)
    }.toString()

    companion object {
        val DEFAULT = Settings()

        fun fromJson(json: String): Settings {
            return json.takeIf { it.isNotBlank() }?.runCatching(::JSONObject)?.getOrNull()?.runCatching {
                Settings(
                    enabled = optBoolean("enabled", false),
                    hasShownInstructions = optBoolean("has_shown_instructions", false),
                    singlePress = GestureSettings.fromJson(optJSONObject("single_press") ?: JSONObject()),
                    doublePress = GestureSettings.fromJson(optJSONObject("double_press") ?: JSONObject()),
                    longPress = GestureSettings.fromJson(optJSONObject("long_press") ?: JSONObject()),
                    assistantPackage = optString("assistant_package", ""),
                    customActions = parseCustomActions(),
                    doubleClickTimeoutMs = optInt("double_click_timeout_ms", 400),
                    debounceTimeoutMs = optInt("debounce_timeout_ms", 550),
                    vibrateOnLongPress = optBoolean("vibrate_on_long_press", false),
                )
            }?.getOrNull() ?: DEFAULT
        }

        private fun JSONObject.parseCustomActions(): List<CustomAction> {
            return optJSONArray("custom_actions")?.runCatching(CustomAction::listFromJsonArray)
                ?.getOrNull() ?: emptyList()
        }
    }
}