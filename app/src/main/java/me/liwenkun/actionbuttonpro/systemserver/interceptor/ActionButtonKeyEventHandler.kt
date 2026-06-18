package me.liwenkun.actionbuttonpro.systemserver.interceptor

import android.app.Application
import android.content.Intent
import android.view.KeyEvent
import androidx.annotation.MainThread
import me.liwenkun.actionbuttonpro.settings.GestureSettings
import me.liwenkun.actionbuttonpro.settings.Settings
import me.liwenkun.actionbuttonpro.settings.SettingsManager
import me.liwenkun.actionbuttonpro.systemserver.interceptor.injectpoint.KeyEventInjectPoint
import me.liwenkun.actionbuttonpro.systemserver.log
import me.liwenkun.actionbuttonpro.systemserver.postSafely
import me.liwenkun.actionbuttonpro.systemserver.workerHandler
import me.liwenkun.actionbuttonpro.ui.EditorMode
import java.util.concurrent.TimeUnit

@MainThread
class ActionButtonKeyEventHandler (
    private val application: Application,
    private val keyEventInjectPoint: KeyEventInjectPoint,
    private val presenterFactory: ShellOutputPresenterFactory
): KeyEventHandler {

    private val settings: Settings
        get() = SettingsManager.getSettings()

    private val gestureDetector = GestureDetector(
        onSinglePress = {
            if (settings.singlePress.enabled) {
                executeGesture(settings.singlePress, "single press")
            } else if(settings.singlePress.passThrough) {
                log("""single press pass through
                    |
                """.trimMargin())
                it.forEach(keyEventInjectPoint::inject)
            }
        },
        onDoublePress = {
            if (settings.doublePress.enabled) {
                executeGesture(settings.doublePress, "double press")
            } else if (settings.doublePress.passThrough) {
                it.forEach(keyEventInjectPoint::inject)
            }
        },
        onLongPressDown = {
            if (settings.vibrateOnLongPress) {
                runCatching {
                    val vibrator = application.getSystemService(android.os.Vibrator::class.java)
                    if (vibrator != null && vibrator.hasVibrator()) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                }.onFailure { e ->
                    log("Vibration failed: ${e.message}")
                }
            }
            if (settings.longPress.enabled) {
                executeGesture(settings.longPress, "long press")
            } else if (settings.longPress.passThrough) {
                keyEventInjectPoint.inject(it)
            }
            log("long press DOWN")
        },
        onLongPressUp = {
            if (!settings.longPress.enabled && settings.longPress.passThrough) {
                keyEventInjectPoint.inject(it)
            }
            log("long press UP")
        },
        needFurtherDetection = {
            when (it) {
                1 -> {
                    settings.doublePress.enabled || settings.doublePress.passThrough
                }
                2 -> {
                   false
                }
                else -> false
            }
        }
    )


    // ═══════════════════════════════════════════
    // 按键入口（由 Hook 层回调）
    // ═══════════════════════════════════════════

    override fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        if (!settings.enabled) {
            log("master switch is disabled, skip interception")
            return false
        }
        gestureDetector.onKeyEvent(keyEvent)
        return true
    }

    // ═══════════════════════════════════════════
    // 执行手势（根据 GestureSettings）
    // ═══════════════════════════════════════════

    private fun executeGesture(gs: GestureSettings, label: String) = runCatching {
        if (!gs.enabled) {
            log("$label gesture disabled, skip")
            return@runCatching
        }

        when (EditorMode.fromKey(gs.activeMode)) {
            EditorMode.SHELL -> executeShell(gs)
            else -> executeAction(gs.action)
        }
    }.onFailure {
        log("$label gesture failed: ${it.message}")
    }

    // ═══════════════════════════════════════════
    // Shell / Action 执行
    // ═══════════════════════════════════════════

    private fun getLocalizedString(zh: String, en: String): String {
        return if (java.util.Locale.getDefault().language == "zh") zh else en
    }

    private fun executeShell(gs: GestureSettings) = workerHandler.postSafely {
        val command = gs.shellCommand
        if (command.isEmpty()) return@postSafely
        log("executing shell: $command")
        Runtime.getRuntime().exec("su").apply {
            outputStream.bufferedWriter().use {
                it.write(command)
                it.write("\n")
            }
            val timeout = !waitFor(10000, TimeUnit.MILLISECONDS)
            if (!timeout) {
                val exitCode = exitValue()
                if (exitCode == 0) {
                    val output = inputStream.reader().readText().trim()
                    log("shell executed successfully, output: \n $output")
                    if (gs.showShellOutput) {
                        val defaultMsg = getLocalizedString("执行成功（无输出结果）", "Executed successfully with no output")
                        presenterFactory.getPresenter(gs.shellOutputFormat).displayShellOutput(
                            isSuccess = true,
                            message = output.ifEmpty { defaultMsg }
                        )
                    }
                } else {
                    val errorOutput = errorStream.reader().readText().trim()
                    log("shell executed failed, exit code: $exitCode, output: \n $errorOutput")
                    if (gs.showShellOutput) {
                        val failPrefix = getGitVersionedFailedMessage(exitCode)
                        val noErrorDetails = getLocalizedString("无错误详情", "No error details")
                        val details = errorOutput.ifEmpty { noErrorDetails }
                        presenterFactory.getPresenter(gs.shellOutputFormat).displayShellOutput(
                            isSuccess = false,
                            message = "$failPrefix\n$details"
                        )
                    }
                }
            } else {
                log("shell executed timeout")
                if (gs.showShellOutput) {
                    val timeoutMsg = getLocalizedString("执行超时 (超过 10 秒)", "Command timed out (exceeded 10s)")
                    presenterFactory.getPresenter(gs.shellOutputFormat).displayShellOutput(
                        isSuccess = false,
                        message = timeoutMsg
                    )
                }
            }
        }
    }

    private fun getGitVersionedFailedMessage(exitCode: Int): String {
        return if (java.util.Locale.getDefault().language == "zh") {
            "执行失败 (退出码: $exitCode)"
        } else {
            "Execution failed (Exit code: $exitCode)"
        }
    }

    /** 执行 Action（Intent） */
    private fun executeAction(action: String) {
        if (action.isBlank()) return
        log("starting activity: $action")
        application.startActivity(
            Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

