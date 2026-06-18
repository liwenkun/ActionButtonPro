package me.liwenkun.actionbuttonpro.systemserver.interceptor.injectpoint

import android.view.KeyEvent
import androidx.annotation.WorkerThread
import me.liwenkun.actionbuttonpro.systemserver.interceptor.KeyEventHandler
import me.liwenkun.actionbuttonpro.systemserver.log
import me.liwenkun.actionbuttonpro.systemserver.prelude.MethodHookParam

abstract class KeyEventInjectPoint(keyEventHandlerCreator: KeyEventInjectPoint.() -> KeyEventHandler) {

    private val keyEventHandler by lazy {
        keyEventHandlerCreator()
    }

    @WorkerThread
    abstract fun hook()
    fun inject(keyEvent: KeyEvent) {
        injectImpl(TaggedKeyEvent(keyEvent))
    }
    protected abstract fun injectImpl(keyEvent: KeyEvent)

    protected fun KeyEvent.intercepted(methodHookParam: MethodHookParam, valueToReturn: Any? = null) {
        if (this is TaggedKeyEvent) {
            return
        }
        runCatching {
            keyEventHandler.onKeyEvent(TaggedKeyEvent(this))
        }.onSuccess {intercepted ->
            if (intercepted) {
                methodHookParam.result = valueToReturn
            }
        }.onFailure {
            log("executeAction failed: ${it.message}")
        }
    }
}