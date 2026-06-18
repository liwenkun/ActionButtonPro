# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Xposed API
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

# Ignore missing system/framework classes referenced by stubs/Xposed code
-dontwarn android.view.**
-dontwarn com.android.server.**
-dontwarn android.app.IUriGrantsManager
-dontwarn android.content.res.FontResourcesParser$FamilyResourceEntry
-dontwarn android.os.IVibratorStateListener
-dontwarn android.util.Pools$SynchronizedPool
-dontwarn android.util.proto.ProtoInputStream
-dontwarn android.window.CompatOnBackInvokedCallback
-dontwarn android.window.ImeOnBackInvokedDispatcher
-dontwarn android.window.WindowProviderService
-dontwarn com.android.internal.inputmethod.IConnectionlessHandwritingCallback
-dontwarn com.android.internal.inputmethod.ImeTracing$ServiceDumper
-dontwarn com.android.internal.inputmethod.InputMethodPrivilegedOperations
-dontwarn com.android.internal.util.RingBuffer
-dontwarn libcore.util.NativeAllocationRegistry

-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-adaptresourcefilecontents META-INF/xposed/scope.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}