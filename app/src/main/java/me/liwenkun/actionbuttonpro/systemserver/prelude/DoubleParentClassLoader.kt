package me.liwenkun.actionbuttonpro.systemserver.prelude

import me.liwenkun.actionbuttonpro.systemserver.log

class DoubleParentClassLoader(private val father: ClassLoader,
                              mother: ClassLoader)
    : ClassLoader(father) {

    private val mother = OutgoingClassLoader(mother)

    override fun loadClass(name: String?, resolve: Boolean): Class<*>? {
        log("load: $name")
        return runCatching {
            super.loadClass(name, resolve)
        }.onSuccess {
            log("load success: $it from $father ")
        }.onFailure {
            log("-load failed: $name from $father")
        }.getOrNull() ?: runCatching {
            mother.loadClass(name, resolve)
        }.onSuccess {
            log("-load success: $it from $mother")
        }.onFailure {
            log("-load failed: $name from $mother")
        }.getOrNull()
    }
}

private class OutgoingClassLoader(xposedClassLoader: ClassLoader):
    ClassLoader(xposedClassLoader) {

    public override fun loadClass(name: String?, resolve: Boolean): Class<*>? {
        return super.loadClass(name, resolve)
    }
}