package me.liwenkun.actionbuttonpro.ui

import org.json.JSONArray
import org.json.JSONObject

/**
 * 自定义 Action 数据模型。
 *
 * JSON 格式：[{"label":"...","action":"..."}, ...]
 */
data class CustomAction(
    val label: String,
    val action: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("label", label)
        put("action", action)
    }

    companion object {
        fun fromJson(json: JSONObject): CustomAction = CustomAction(
            label = json.getString("label"),
            action = json.getString("action")
        )

        /** 序列化列表 → JSON 字符串 */
        fun listToJson(list: List<CustomAction>): String {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }

        /** 序列化列表 → JSONArray */
        fun listToJsonArray(list: List<CustomAction>): JSONArray {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            return arr
        }

        /** 反序列化 JSONArray → 列表 */
        fun listFromJsonArray(json: JSONArray): List<CustomAction> {
            if (json.length() == 0) return emptyList()
            return try {
                (0 until json.length()).map { i ->
                    fromJson(json.getJSONObject(i))
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}