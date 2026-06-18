package me.liwenkun.actionbuttonpro.systemserver.interceptor.injectpoint

import android.view.KeyEvent
import com.android.server.policy.StrategyActionButtonKeyLaunchApp
import me.liwenkun.actionbuttonpro.systemserver.interceptor.ActionButtonKeyEventHandler
import me.liwenkun.actionbuttonpro.systemserver.prelude.hook

class SystemServerKeyEventInjectPoint(
    keyEventHandlerCreator: KeyEventInjectPoint.() -> ActionButtonKeyEventHandler)
    : KeyEventInjectPoint(keyEventHandlerCreator) {

    override fun hook() {
        StrategyActionButtonKeyLaunchApp::class.hook(
            "injectActionButtonPressKeyEvent",
            KeyEvent::class.java
        ) {
            before {
                (args[0] as KeyEvent).intercepted(this)
            }
        }
    }

    override fun injectImpl(keyEvent: KeyEvent) {
        StrategyActionButtonKeyLaunchApp.injectActionButtonPressKeyEvent(keyEvent)
    }
}