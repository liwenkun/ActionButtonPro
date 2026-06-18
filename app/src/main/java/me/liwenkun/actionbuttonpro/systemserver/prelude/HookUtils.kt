package me.liwenkun.actionbuttonpro.systemserver.prelude

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.liwenkun.actionbuttonpro.systemserver.prelude.LspModule.Companion.MODULE
import kotlin.reflect.KClass

class HookConfig {
    var beforeHook: (MethodHookParam.() -> Unit)? = null
    var afterHook: (MethodHookParam.() -> Unit)? = null
    fun before(block: MethodHookParam.() -> Unit) {
        beforeHook = block
    }

    fun after(block: MethodHookParam.() -> Unit) {
        afterHook = block
    }
}

class MethodHookParam {
    var args :Array<Any> = emptyArray()
    var result: Any? = Unit
    var thisObject: Any? = Unit
}

fun KClass<*>.hook(
    methodName: String, vararg paramTypes: Class<*>,
    configure: HookConfig.() -> Unit
) = hookXposed(methodName, *paramTypes) {
    configure()
}

private fun KClass<*>.hookXposed(methodName: String, vararg paramTypes: Class<*>,
               configure: HookConfig.() -> Unit) {
    HookConfig().apply(configure).also { hookConfig ->
        val callback = object: XC_MethodHook() {
            override fun beforeHookedMethod(param: XC_MethodHook.MethodHookParam) {
                MethodHookParam().apply {
                    thisObject = param.thisObject
                    args = param.args
                }.let {
                    hookConfig.beforeHook?.invoke(it)
                    if (it.result != Unit) {
                        param.setResult(it.result)
                    }
                }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                MethodHookParam().apply {
                    thisObject = param.thisObject
                    result = param.result
                    args = param.args
                }.let {
                    hookConfig.afterHook?.invoke(it)
                    param.result = it.result
                }
            }
        }
        XposedHelpers.findAndHookMethod(java, methodName,
            *paramTypes, callback)
    }
}

private fun KClass<*>.hookLSPosed(methodName: String, vararg paramTypes: Class<*>,
                          configure: HookConfig.() -> Unit) {

    HookConfig().apply(configure).also { hookConfig ->

        MODULE.hook(java.getDeclaredMethod(methodName, *paramTypes)).intercept { chain ->
            MethodHookParam().apply {
                thisObject = chain.thisObject
                args = chain.args.toTypedArray()
            }.let {
                hookConfig.beforeHook?.invoke(it)

                if (it.result != Unit) {
                    return@intercept it
                }

                if (chain.thisObject == null) {
                    chain.proceed(it.args)
                } else {
                    chain.proceedWith(it.thisObject!!, it.args)
                }
            }.let { result ->
                MethodHookParam().apply {
                    thisObject = chain.thisObject
                    this.result = result
                    args = chain.args.toTypedArray()
                }.let {
                    hookConfig.afterHook?.invoke(it)
                    it.result
                }
            }
        }
    }
}