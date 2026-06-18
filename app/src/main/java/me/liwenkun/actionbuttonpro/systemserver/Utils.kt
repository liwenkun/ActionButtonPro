package me.liwenkun.actionbuttonpro.systemserver

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import me.liwenkun.actionbuttonpro.BuildConfig


val uiHandler by lazy { Handler(Looper.getMainLooper()) }

val workerHandler by lazy {
    HandlerThread(BuildConfig.APPLICATION_ID + ".worker_thread")
        .apply { start() }.looper.let(::Handler)
}

@SuppressLint("QueryPermissionsNeeded")
fun getAssistantPackage(context: Context): String? {
    return runCatching {
        Intent(Intent.ACTION_VOICE_COMMAND)
            .resolveActivityInfo(
                context.packageManager,
                PackageManager.MATCH_DEFAULT_ONLY
            ).packageName
    }.getOrNull()
}

inline fun Handler.postSafely(
    delay: Long = 0L,
    async: Boolean = false,
    crossinline onFailure: (Throwable) -> Unit = { log("exception: ${it.message}") },
    crossinline block: () -> Unit) = Runnable {
    runCatching {
        block()
    }.onFailure {
        onFailure(it)
    }
}.let {
    Message.obtain(this, it).also { msg ->
        msg.isAsynchronous = async
        sendMessageDelayed(msg, delay)
    }
    Unit
}

fun Handler.postInterval(interval: Long, condition: () -> Boolean = { true }, action: () -> Unit): Unit =
    postSafely(interval) {
        action()
        if (condition()) {
           postInterval(interval, condition, action)
        }
    }


fun log(msg : String) {
    Log.d("LSPosed-Bridge", "[ActionButtonPro] $msg")
}
