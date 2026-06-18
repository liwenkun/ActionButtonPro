package me.liwenkun.actionbuttonpro.systemserver.prelude

import androidx.annotation.Keep
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import me.liwenkun.actionbuttonpro.systemserver.log

@Keep
class LspModule : XposedModule() {

    companion object {
        lateinit var MODULE: LspModule
    }

    override fun onSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        MODULE = this
        log("LSPosed module loaded")
        ModuleImpl.onSystemServerReady(param.classLoader)
    }
}