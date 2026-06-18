package me.liwenkun.actionbuttonpro.systemserver.interceptor.injectpoint

import android.hardware.input.InputManagerGlobal
import android.view.InputEvent
import android.view.KeyEvent
import me.liwenkun.actionbuttonpro.systemserver.interceptor.GestureDetector
import me.liwenkun.actionbuttonpro.systemserver.interceptor.KeyEventHandler
import me.liwenkun.actionbuttonpro.systemserver.log
import me.liwenkun.actionbuttonpro.systemserver.prelude.hook

class FrameworkKeyEventInjectPoint(keyEventHandlerCreator: KeyEventInjectPoint.() -> KeyEventHandler)
    : KeyEventInjectPoint(keyEventHandlerCreator) {

    companion object {
        const val METHOD_NAME = "injectInputEvent"
//        val method: Method by lazy {
//            InputManager::class.java
//                .getDeclaredMethod(
//                    METHOD_NAME,
//                    InputEvent::class.java, Int::class.java, Int::class.java)
//                .apply { isAccessible = true }
//        }
    }
    override fun hook() {
        InputManagerGlobal::class.hook(
            METHOD_NAME,
                    InputEvent::class.java, Int::class.java, Int::class.java
        ) {
            before {
                log("received key event ${args[0]}")
                (args[0] as? KeyEvent)?.takeIf {
                    it.keyCode == GestureDetector.KEYCODE_ACTION_BUTTON_SINGLE_TAP
                            || it.keyCode == GestureDetector.KEYCODE_ACTION_BUTTON_LONG_PRESS
                }?.intercepted(this, valueToReturn = true)
                    ?: log("not a action button key event, skip")
            }
        }
    }

    override fun injectImpl(keyEvent: KeyEvent) {
        InputManagerGlobal.getInstance().injectInputEvent(keyEvent, 0, 0)
//        val inputManager = application.getSystemService(Context.INPUT_SERVICE)
//        method.invoke(inputManager, keyEvent, 0, 0)
    }
}