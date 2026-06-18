package me.liwenkun.actionbuttonpro.ui

/**
 * 操作模式：预设 / Shell
 */
enum class EditorMode(val key: String) {
    ACTION("action"),
    SHELL("shell");

    companion object {
        fun fromKey(key: String): EditorMode =
            entries.find { it.key == key } ?: ACTION
    }
}