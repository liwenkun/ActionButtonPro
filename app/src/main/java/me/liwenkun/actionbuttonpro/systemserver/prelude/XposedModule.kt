package me.liwenkun.actionbuttonpro.systemserver.prelude

import androidx.annotation.Keep
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.liwenkun.actionbuttonpro.systemserver.log

@Keep
class XposedModule : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "android") {
            return
        }
        log("Xposed module loaded")
        ModuleImpl.onSystemServerReady(lpparam.classLoader)
    }
}
