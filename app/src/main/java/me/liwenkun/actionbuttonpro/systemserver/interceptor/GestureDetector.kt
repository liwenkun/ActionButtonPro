package me.liwenkun.actionbuttonpro.systemserver.interceptor

import android.os.SystemClock
import android.view.KeyEvent
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import me.liwenkun.actionbuttonpro.settings.SettingsManager
import me.liwenkun.actionbuttonpro.systemserver.log
import me.liwenkun.actionbuttonpro.systemserver.postSafely
import me.liwenkun.actionbuttonpro.systemserver.uiHandler


@MainThread
class GestureDetector(
    private val onSinglePress: (Array<KeyEvent>) -> Unit,
    private val onDoublePress: (Array<KeyEvent>) -> Unit,
    private val onLongPressDown: (KeyEvent) -> Unit,
    private val onLongPressUp: (KeyEvent) -> Unit,
    private val needFurtherDetection: (count: Int) -> Boolean,
) {

    private val keyEvents = mutableListOf<KeyEvent>()

    private val doubleClickWindowMs: Long
        get() = SettingsManager.getSettings().doubleClickTimeoutMs.toLong()

    private val eventDebounceMs: Long
        get() = SettingsManager.getSettings().debounceTimeoutMs.toLong()

    companion object {
        const val KEYCODE_ACTION_BUTTON_SINGLE_TAP = 781
        const val KEYCODE_ACTION_BUTTON_LONG_PRESS = 782
    }

    private var lastShortUpTime = 0L
    private var lastGestureEmitTime = 0L

    @WorkerThread
    fun onKeyEvent(keyEvent: KeyEvent) = uiHandler.postSafely(async = true) {
        when (keyEvent.keyCode) {
            KEYCODE_ACTION_BUTTON_SINGLE_TAP -> {
                if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                    keyEvents += keyEvent
                } else if (keyEvent.action == KeyEvent.ACTION_UP) {
                    keyEvents += keyEvent
                    onShortPressUp()
                }
            }
            KEYCODE_ACTION_BUTTON_LONG_PRESS -> {
                if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                    onLongPressDown(keyEvent)
                } else if (keyEvent.action == KeyEvent.ACTION_UP) {
                    onLongPressUp(keyEvent)
                }
            }
        }
    }

    private fun onShortPressUp() {
        val now = SystemClock.uptimeMillis()
        if (now - lastShortUpTime >= doubleClickWindowMs) {
            val lastUp = keyEvents.removeLast()
            val lastDown = keyEvents.removeLast()
            keyEvents.clear()
            keyEvents += lastDown
            keyEvents += lastUp
        }
        lastShortUpTime = now
        triggerDetection(keyEvents.size / 2)
    }

    private fun triggerDetection(count: Int) {
        if (needFurtherDetection(count)) {
            log("triggerGestureByCount: postDelayed $count")
            uiHandler.postSafely(doubleClickWindowMs) {
                triggerGestureDelayed(count)
            }
            return
        }
        log("triggerGestureByCount: immediately $count, time: ${SystemClock.uptimeMillis()}")
        emitGesture(count)
    }

    private fun emitGesture(count: Int) {
        assert(keyEvents.size == count * 2)
        val now = SystemClock.uptimeMillis()
        if (now - lastGestureEmitTime < eventDebounceMs) {
            log("emitGesture: too fast, skip")
            return
        }
        lastGestureEmitTime = SystemClock.uptimeMillis()
        when (count) {
            1 -> onSinglePress(keyEvents.toTypedArray())
            2 -> onDoublePress(keyEvents.toTypedArray())
        }
        keyEvents.clear()
    }

    private fun triggerGestureDelayed(count: Int) {
        // 延时过程中可能又来了新事件（shortUpCount 已被更新）
        if (keyEvents.size != count * 2) {
            // 窗口内又来了新事件，放弃本次判定（下一次 postDelayed 会覆盖）
            return
        }
        emitGesture(count)
    }
}
