package me.liwenkun.actionbuttonpro.settings

import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import java.util.Collections

class RemoteSharedPreference(
    context: Context,
    private var prefName: String? = null,
) : SharedPreferences {

    private val authority: String = "me.liwenkun.actionbuttonpro.provider.settings"

    init {
        if (prefName == null) {
            throw IllegalArgumentException("prefName cannot be null")
        }
    }

    private val contentResolver = context.contentResolver
    private val uri = "content://$authority".toUri()
    private val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (uri == null) return
            val segments = uri.pathSegments
            // Uri pattern: content://$authority/$prefName/key
            if (segments.isNotEmpty() && segments[0] == prefName) {
                val key = if (segments.size > 1) segments[1] else null
                Collections.unmodifiableList(listeners)
                val listenersCopy = synchronized(listeners) { ArrayList(listeners) }
                listenersCopy.forEach { listener ->
                    listener.onSharedPreferenceChanged(this@RemoteSharedPreference, key)
                }
            }
        }
    }

    private fun callProvider(method: String, key: String?, extras: Bundle? = Bundle()): Bundle? {
        val finalExtras = extras ?: Bundle()
        finalExtras.putString("name", prefName)
        return try {
            contentResolver.call(uri, method, key, finalExtras)
        } catch (_: Exception) {
            null
        }
    }

    override fun contains(key: String?): Boolean {
        val bundle = callProvider("contains", key)
        return bundle?.getBoolean("value", false) ?: false
    }

    override fun edit(): SharedPreferences.Editor {
        return Editor()
    }

    @Suppress("DEPRECATION")
    override fun getAll(): Map<String?, *> {
        val bundle = callProvider("getAll", null) ?: return emptyMap<String?, Any>()
        val map = HashMap<String?, Any?>()
        for (key in bundle.keySet()) {
            map[key] = bundle.get(key)
        }
        return map
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        val extras = Bundle().apply { putBoolean("default", defValue) }
        val bundle = callProvider("getBoolean", key, extras)
        return bundle?.getBoolean("value", defValue) ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        val extras = Bundle().apply { putFloat("default", defValue) }
        val bundle = callProvider("getFloat", key, extras)
        return bundle?.getFloat("value", defValue) ?: defValue
    }

    override fun getInt(key: String?, defValue: Int): Int {
        val extras = Bundle().apply { putInt("default", defValue) }
        val bundle = callProvider("getInt", key, extras)
        return bundle?.getInt("value", defValue) ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        val extras = Bundle().apply { putLong("default", defValue) }
        val bundle = callProvider("getLong", key, extras)
        return bundle?.getLong("value", defValue) ?: defValue
    }

    override fun getString(key: String?, defValue: String?): String? {
        val extras = Bundle().apply { putString("default", defValue) }
        val bundle = callProvider("getString", key, extras)
        return bundle?.getString("value") ?: defValue
    }

    override fun getStringSet(key: String?, defValues: Set<String?>?): Set<String?>? {
        val extras = Bundle().apply {
            if (defValues != null) {
                putStringArrayList("default", ArrayList(defValues.filterNotNull()))
            }
        }
        val bundle = callProvider("getStringSet", key, extras)
        val list = bundle?.getStringArrayList("value")
        return list?.toSet() ?: defValues
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        if (listener != null) {
            synchronized(listeners) {
                if (!listeners.contains(listener)) {
                    if (listeners.isEmpty()) {
                        val watchUri = "content://$authority/$prefName".toUri()
                        contentResolver.registerContentObserver(watchUri, true, observer)
                    }
                    listeners.add(listener)
                }
            }
        }
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        if (listener != null) {
            synchronized(listeners) {
                if (listeners.remove(listener) && listeners.isEmpty()) {
                    contentResolver.unregisterContentObserver(observer)
                }
            }
        }
    }

    inner class Editor : SharedPreferences.Editor {
        private var clearAll = false
        private val removeKeys = ArrayList<String>()
        private val values = Bundle()

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) {
                values.putString(key, value)
                removeKeys.remove(key)
            }
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) {
                values.putInt(key, value)
                removeKeys.remove(key)
            }
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) {
                values.putLong(key, value)
                removeKeys.remove(key)
            }
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) {
                values.putFloat(key, value)
                removeKeys.remove(key)
            }
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) {
                values.putBoolean(key, value)
                removeKeys.remove(key)
            }
            return this
        }

        override fun putStringSet(key: String?, valuesSet: Set<String?>?): SharedPreferences.Editor {
            if (key != null) {
                val list = if (valuesSet != null) ArrayList(valuesSet.filterNotNull()) else null
                values.putStringArrayList(key, list)
                removeKeys.remove(key)
            }
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) {
                removeKeys.add(key)
                values.remove(key)
            }
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            removeKeys.clear()
            values.clear()
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            val batchExtras = Bundle().apply {
                putBoolean("clear", clearAll)
                if (removeKeys.isNotEmpty()) {
                    putStringArrayList("remove_keys", removeKeys)
                }
                if (!values.isEmpty) {
                    putBundle("values", values)
                }
            }
            callProvider("batch_edit", null, batchExtras)

            // Reset state
            clearAll = false
            removeKeys.clear()
            values.clear()
        }
    }
}
