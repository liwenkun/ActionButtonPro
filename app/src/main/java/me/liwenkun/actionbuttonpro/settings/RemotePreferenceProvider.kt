package me.liwenkun.actionbuttonpro.settings

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.core.content.edit
import androidx.core.net.toUri

class RemotePreferenceProvider : ContentProvider() {

    override fun onCreate() = true

    override fun call(authority: String, method: String, arg: String?, extras: Bundle?): Bundle? {
        val ctx = context ?: return null
        val prefName = extras?.getString("name") ?: "settings"
        val sharedPrefs = ctx.getSharedPreferences(prefName, Context.MODE_PRIVATE)

        if (arg == null && method != "getAll" && method != "clear" && method != "batch_edit") return null

        val resultBundle = Bundle()
        when (method) {
            // ==================== Read Operations ====================
            "getString" -> {
                val default = extras?.getString("default")
                resultBundle.putString("value", sharedPrefs.getString(arg, default))
            }
            "getInt" -> {
                val default = extras?.getInt("default") ?: 0
                resultBundle.putInt("value", sharedPrefs.getInt(arg, default))
            }
            "getLong" -> {
                val default = extras?.getLong("default") ?: 0L
                resultBundle.putLong("value", sharedPrefs.getLong(arg, default))
            }
            "getFloat" -> {
                val default = extras?.getFloat("default") ?: 0f
                resultBundle.putFloat("value", sharedPrefs.getFloat(arg, default))
            }
            "getBoolean" -> {
                val default = extras?.getBoolean("default") ?: false
                resultBundle.putBoolean("value", sharedPrefs.getBoolean(arg, default))
            }
            "getStringSet" -> {
                val default = extras?.getStringArrayList("default")?.toSet()
                val result = sharedPrefs.getStringSet(arg, default)
                if (result != null) {
                    resultBundle.putStringArrayList("value", ArrayList(result))
                }
            }
            "contains" -> {
                resultBundle.putBoolean("value", sharedPrefs.contains(arg))
            }
            "getAll" -> {
                val allPrefs = sharedPrefs.all
                allPrefs.forEach { (key, value) -> 
                    when (value) {
                        is String -> resultBundle.putString(key, value)
                        is Int -> resultBundle.putInt(key, value)
                        is Long -> resultBundle.putLong(key, value)
                        is Float -> resultBundle.putFloat(key, value)
                        is Boolean -> resultBundle.putBoolean(key, value)
                        is Set<*> -> {
                            resultBundle.putStringArray(key, 
                                value.filterIsInstance<String>().toTypedArray())
                        }
                    }
                }
            }

            // ==================== Write Operations ====================
            "batch_edit" -> {
                val removeKeys = extras?.getStringArrayList("remove_keys")
                val values = extras?.getBundle("values")
                val isClear = extras?.getBoolean("clear", false) == true

                sharedPrefs.edit {
                    if (isClear) {
                        clear()
                    }
                    removeKeys?.forEach { remove(it) }

                    values?.keySet()?.forEach { k ->
                        @Suppress("DEPRECATION")
                        when (val v = values.get(k)) {
                            is String -> putString(k, v)
                            is Int -> putInt(k, v)
                            is Long -> putLong(k, v)
                            is Float -> putFloat(k, v)
                            is Boolean -> putBoolean(k, v)
                            is ArrayList<*> -> {
                                val set = v.filterIsInstance<String>().toSet()
                                putStringSet(k, set)
                            }
                        }
                    }
                }

                // Notify change to observers
                val baseUri = "content://$authority/$prefName".toUri()
//                ctx.contentResolver.notifyChange(baseUri, null)
                
                if (isClear) {
                    // Notify clear (null key means everything changed)
                    ctx.contentResolver.notifyChange(baseUri, null)
                } else {
                    removeKeys?.forEach { 
                        ctx.contentResolver.notifyChange(Uri.withAppendedPath(baseUri, it), null) 
                    }
                    values?.keySet()?.forEach { 
                        ctx.contentResolver.notifyChange(Uri.withAppendedPath(baseUri, it), null) 
                    }
                }

                resultBundle.putBoolean("result", true)
            }
            else -> return super.call(authority, method, arg, extras)
        }
        return resultBundle
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String?>?) = 0
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun query(uri: Uri, projection: Array<out String?>?, selection: String?, selectionArgs: Array<out String?>?, sortOrder: String?): Cursor? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String?>?) = 0
}
