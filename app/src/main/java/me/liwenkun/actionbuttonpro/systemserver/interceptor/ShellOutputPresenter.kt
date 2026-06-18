package me.liwenkun.actionbuttonpro.systemserver.interceptor

import android.app.AlertDialog
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import me.liwenkun.actionbuttonpro.BuildConfig
import me.liwenkun.actionbuttonpro.R
import me.liwenkun.actionbuttonpro.systemserver.uiHandler

sealed interface ShellOutputPresenter {
    fun displayShellOutput(isSuccess: Boolean, message: String)
}

private fun Context.getAppString(resId: Int, vararg formatArgs: Any): String {
    return try {
        val appContext = createPackageContext(BuildConfig.APPLICATION_ID,
            Context.CONTEXT_IGNORE_SECURITY)
        appContext.getString(resId, *formatArgs)
    } catch (_: Exception) {
        ""
    }
}

class ToastShellOutputPresenter(private val application: Application) : ShellOutputPresenter {
    override fun displayShellOutput(isSuccess: Boolean, message: String) {
        val titleId = if (isSuccess) R.string.shell_exec_success else R.string.shell_exec_failed
        val title = application.getAppString(titleId).ifEmpty {
            if (isSuccess) "Shell 执行成功" else "Shell 执行失败"
        }
        uiHandler.post {
            Toast.makeText(application, "$title:\n$message", Toast.LENGTH_LONG).show()
        }
    }
}

class NotificationShellOutputPresenter(private val application: Application) : ShellOutputPresenter {
    
    private val notificationManager by lazy {
        application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    override fun displayShellOutput(isSuccess: Boolean, message: String) {
        val titleId = if (isSuccess) R.string.shell_exec_success else R.string.shell_exec_failed
        val title = application.getAppString(titleId).ifEmpty {
            if (isSuccess) "Shell 执行成功" else "Shell 执行失败"
        }
        val channelId = "shell_output_channel"
        val channelName = "Shell Output"
        
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val packageName = BuildConfig.APPLICATION_ID
        val smallIcon = try {
            val appContext = application.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
            IconCompat.createWithResource(appContext, appContext.applicationInfo.icon)
        } catch (_: Exception) {
            IconCompat.createWithResource(application, android.R.drawable.ic_dialog_info)
        }

        val builder = NotificationCompat.Builder(application, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}


class DialogShellOutputPresenter(private val application: Application) : ShellOutputPresenter {
    override fun displayShellOutput(isSuccess: Boolean, message: String) {
        val titleId = if (isSuccess) R.string.shell_exec_success else R.string.shell_exec_failed
        val title = application.getAppString(titleId).ifEmpty { if (isSuccess) "Shell 执行成功" else "Shell 执行失败" }
        val confirmText = application.getAppString(R.string.btn_confirm).ifEmpty { "确定" }
        uiHandler.post {
            runCatching {
                AlertDialog.Builder(
                    application,
                    android.R.style.Theme_DeviceDefault_Light_Dialog_Alert).apply {
                    setTitle(title)
                    setMessage(message)
                    setPositiveButton(confirmText, null)
                    val dialog = create()
                    dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                    dialog.show()
                }
            }
        }
    }
}

sealed interface ShellOutputPresenterFactory {
    fun getPresenter(format: String): ShellOutputPresenter
}


class DefaultShellOutputPresenterFactory(private val application: Application) : ShellOutputPresenterFactory {
    private val presenters = mutableMapOf<String, ShellOutputPresenter>()

    override fun getPresenter(format: String): ShellOutputPresenter {
        return presenters.computeIfAbsent(format) {
            when (it) {
                "toast" -> ToastShellOutputPresenter(application)
                "notification" -> NotificationShellOutputPresenter(application)
                "dialog" -> DialogShellOutputPresenter(application)
                else -> ToastShellOutputPresenter(application)
            }
        }
    }
}
