package me.liwenkun.actionbuttonpro.systemserver.prelude

import android.app.Application
import android.content.Context
import me.liwenkun.actionbuttonpro.systemserver.interceptor.Interceptor
import me.liwenkun.actionbuttonpro.systemserver.log
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
object ModuleImpl {
    @Volatile
    private var initialized: AtomicBoolean = AtomicBoolean(false)
    fun onSystemServerReady(systemServerLoader: ClassLoader) {
        val moduleClassLoader: ClassLoader = javaClass.classLoader!!
        DoubleParentClassLoader(moduleClassLoader.parent, systemServerLoader).also {
            ClassLoader::class.hook("getParent") {
                after {
                    if (thisObject === moduleClassLoader) result = it
                }
            }
        }

        Application::class.hook("attach", Context::class.java) {
            after {
                if (initialized.compareAndSet(expectedValue = false, newValue = true)) {
                    Interceptor.onApplicationReady(thisObject as Application)
                    log("application.attach: $thisObject ${args[0]}")
                }
            }
        }
    }
}
