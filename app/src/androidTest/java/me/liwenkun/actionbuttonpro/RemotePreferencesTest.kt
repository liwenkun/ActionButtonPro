package me.liwenkun.actionbuttonpro

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.liwenkun.actionbuttonpro.settings.RemoteSharedPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class RemotePreferencesTest {

    private lateinit var context: Context
    private lateinit var remotePrefs: RemoteSharedPreference

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // Create a test preferences instance pointing to "test_settings"
        remotePrefs = RemoteSharedPreference(context, "test_settings")

        generateSequence(RemotePreferencesTest::class.java.classLoader) { it.parent }.forEach {
            println("${it}")
        }
        // Clear all before each test to ensure an isolated environment
        remotePrefs.edit().clear().commit()
    }

    @Test
    fun testBasicPutAndGet() {
        val stringSet = setOf("apple", "banana", "cherry")

        // 1. Batch write values
        remotePrefs.edit()
            .putString("key_string", "hello_world")
            .putInt("key_int", 42)
            .putLong("key_long", 123456789L)
            .putFloat("key_float", 3.14f)
            .putBoolean("key_bool", true)
            .putStringSet("key_set", stringSet)
            .apply() // Commit changes to RemotePreferenceProvider via a single batch RPC

        // 2. Read back and assert values
        assertEquals("hello_world", remotePrefs.getString("key_string", "default"))
        assertEquals(42, remotePrefs.getInt("key_int", 0))
        assertEquals(123456789L, remotePrefs.getLong("key_long", 0L))
        assertEquals(3.14f, remotePrefs.getFloat("key_float", 0f), 0.001f)
        assertTrue(remotePrefs.getBoolean("key_bool", false))
        assertEquals(stringSet, remotePrefs.getStringSet("key_set", emptySet()))
    }

    @Test
    fun testDefaultValues() {
        // Assert that non-existent keys return the supplied defaults
        assertEquals("fallback", remotePrefs.getString("non_existent_key", "fallback"))
        assertEquals(99, remotePrefs.getInt("non_existent_key", 99))
        assertEquals(999L, remotePrefs.getLong("non_existent_key", 999L))
        assertEquals(1.23f, remotePrefs.getFloat("non_existent_key", 1.23f), 0.001f)
        assertTrue(remotePrefs.getBoolean("non_existent_key", true))
        
        val defaultSet = setOf("default1", "default2")
        assertEquals(defaultSet, remotePrefs.getStringSet("non_existent_key", defaultSet))
    }

    @Test
    fun testContains() {
        assertFalse(remotePrefs.contains("contain_test_key"))

        remotePrefs.edit().putString("contain_test_key", "present").apply()

        assertTrue(remotePrefs.contains("contain_test_key"))
    }

    @Test
    fun testRemove() {
        remotePrefs.edit()
            .putString("remove_key", "temporary_value")
            .apply()

        assertTrue(remotePrefs.contains("remove_key"))

        // Perform removal
        remotePrefs.edit().remove("remove_key").apply()

        assertFalse(remotePrefs.contains("remove_key"))
        assertNull(remotePrefs.getString("remove_key", null))
    }

    @Test
    fun testClear() {
        remotePrefs.edit()
            .putString("k1", "v1")
            .putInt("k2", 2)
            .apply()

        assertTrue(remotePrefs.contains("k1"))
        assertTrue(remotePrefs.contains("k2"))

        // Perform clear
        remotePrefs.edit().clear().apply()

        assertFalse(remotePrefs.contains("k1"))
        assertFalse(remotePrefs.contains("k2"))
        assertTrue(remotePrefs.getAll().isEmpty())
    }

    @Test
    fun testGetAll() {
        remotePrefs.edit()
            .putString("str", "test")
            .putInt("num", 10)
            .apply()

        val all = remotePrefs.getAll()
        assertEquals(2, all.size)
        assertEquals("test", all["str"])
        assertEquals(10, all["num"])
    }

    @Test
    fun testContentObserverNotification() {
        val latch = CountDownLatch(1)
        var receivedKey: String? = null

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            receivedKey = key
            latch.countDown()
        }

        // Register the listener (which dynamically registers ContentObserver)
        remotePrefs.registerOnSharedPreferenceChangeListener(listener)

        try {
            // Modify a value, triggering content observer update
            remotePrefs.edit().putString("notify_key", "notified_value").apply()

            // Wait for ContentObserver callback on the main thread
            val callbackReceived = latch.await(2, TimeUnit.SECONDS)
            assertTrue("Timed out waiting for OnSharedPreferenceChangeListener callback", callbackReceived)
            assertEquals("notify_key", receivedKey)
        } finally {
            // Unregister listener to prevent memory leaks
            remotePrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
}
