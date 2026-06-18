package me.liwenkun.actionbuttonpro.systemserver.interceptor

import android.app.Application
import me.liwenkun.actionbuttonpro.settings.SettingsManager
import me.liwenkun.actionbuttonpro.settings.SystemServerSettingStore
import me.liwenkun.actionbuttonpro.systemserver.interceptor.injectpoint.FrameworkKeyEventInjectPoint
import me.liwenkun.actionbuttonpro.systemserver.interceptor.injectpoint.KeyEventInjectPoint
import me.liwenkun.actionbuttonpro.systemserver.interceptor.injectpoint.SystemServerKeyEventInjectPoint
import me.liwenkun.actionbuttonpro.systemserver.log
import me.liwenkun.actionbuttonpro.systemserver.postSafely
import me.liwenkun.actionbuttonpro.systemserver.uiHandler
import me.liwenkun.actionbuttonpro.systemserver.workerHandler

object Interceptor {

    lateinit var application: Application

    internal fun onApplicationReady(application: Application) = uiHandler.postSafely {
        this.application = application
        SettingsManager.init(SystemServerSettingStore(application))
        hookOnce {
            ActionButtonKeyEventHandler(
                application = Interceptor.application,
                keyEventInjectPoint = this,
                presenterFactory = DefaultShellOutputPresenterFactory(Interceptor.application)
            )
        }
    }

    private fun hookOnce(
        keyEventHandlerCreator: KeyEventInjectPoint.() -> ActionButtonKeyEventHandler
    ) = workerHandler.postSafely {
        runCatching {
            SystemServerKeyEventInjectPoint(
                keyEventHandlerCreator
            ).hook()
        }.onFailure {
            log("hook failed: ${it.message}")
        }.onSuccess {
            uiHandler.post {
                onHookSuccess("StrategyActionButtonKeyLaunchApp")
            }
        }.getOrNull() ?: runCatching {
            FrameworkKeyEventInjectPoint(keyEventHandlerCreator).hook()
        }.onSuccess {
            uiHandler.post {
                onHookSuccess("InputManager")
            }
        }.onFailure {
            log("hook failed: ${it.message}")
            uiHandler.post {
                onHookFailed(it)
            }
        }
    }

    private fun onHookSuccess(which: String) = uiHandler.post {
        log("hook $which success")
    }

    private fun onHookFailed(e: Throwable) = uiHandler.post {
        log("hook failed: ${e.message}")
    }
}
