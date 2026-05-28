package com.dct.hooklogger

import android.content.Context
import android.os.Build
import android.provider.Settings

internal object ProtectionHooks {
    fun init(context: Context?) {
        if (context == null) return
        HookRuntime.appContext = context.applicationContext ?: context
    }

    fun disableLogcat() {
        HookRuntime.useLogcat = false
        HookRuntime.write("PROTECTION", "Logcat output disabled")
    }

    fun suppressCrashes() {
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            HookRuntime.write("CRASH_SUPPRESSED", "Thread: ${t.name}, Exception: ${e.javaClass.name}: ${e.message}")
        }
        HookRuntime.write("PROTECTION", "Crash suppression enabled")
    }

    fun dummyExit(status: Int) {
        HookRuntime.write("DUMMY_EXIT", "System.exit($status) called and intercepted.")
    }

    fun fakeIsDebuggerConnected(): Boolean {
        HookRuntime.write("DEBUGGER", "fakeIsDebuggerConnected called, returning false")
        return false
    }

    fun fakeWaitingForDebugger(): Boolean {
        HookRuntime.write("DEBUGGER", "fakeWaitingForDebugger called, returning false")
        return false
    }

    fun fakeIsUserAMonkey(): Boolean {
        HookRuntime.write("ENV", "fakeIsUserAMonkey called, returning false")
        return false
    }

    fun fakeDevelopmentSettingsEnabled(): Int {
        HookRuntime.write("DEBUGGER", "fakeDevelopmentSettingsEnabled called, returning 0")
        return 0
    }

    fun fakeAdbEnabled(): Int {
        HookRuntime.write("DEBUGGER", "fakeAdbEnabled called, returning 0")
        return 0
    }

    fun sanitizedGlobalSetting(name: String?, originalValue: Int): Int {
        val normalizedName = name?.trim()?.lowercase()
        val sanitized = when (normalizedName) {
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED.lowercase(),
            Settings.Global.ADB_ENABLED.lowercase() -> 0
            else -> originalValue
        }
        HookRuntime.write(
            "DEBUGGER",
            "sanitizedGlobalSetting called for ${name ?: "null"} with original=$originalValue, returning $sanitized"
        )
        return sanitized
    }

    fun sanitizedBuildTags(): String {
        val original = Build.TAGS ?: "null"
        val sanitized = original.replace("test-keys", "release-keys")
        HookRuntime.write("ENV", "sanitizedBuildTags called, returning $sanitized")
        return sanitized
    }

    fun sanitizedBuildType(): String {
        val original = Build.TYPE ?: "null"
        val sanitized = if (original == "eng" || original == "userdebug") "user" else original
        HookRuntime.write("ENV", "sanitizedBuildType called, returning $sanitized")
        return sanitized
    }

    fun fakeRoDebuggable(): String {
        HookRuntime.write("ENV", "fakeRoDebuggable called, returning 0")
        return "0"
    }

    fun fakeGetInstallerPackageName(packageName: String?): String {
        val installer = "com.android.vending"
        HookRuntime.write("ENV", "fakeGetInstallerPackageName called for ${packageName ?: "null"}, returning $installer")
        return installer
    }

    fun fakeVpnConnected(): Boolean {
        HookRuntime.write("VPN_BYPASS", "fakeVpnConnected called, returning false")
        return false
    }

    fun fakeNetworkCapabilitiesHasTransport(transportType: Int): Boolean {
        // NetworkCapabilities.TRANSPORT_VPN is 4
        if (transportType == 4) {
            HookRuntime.write("VPN_BYPASS", "fakeNetworkCapabilitiesHasTransport intercepted TRANSPORT_VPN(4), returning false")
            return false
        }
        HookRuntime.write("VPN_BYPASS", "fakeNetworkCapabilitiesHasTransport passthrough transportType=$transportType")
        return true // Assume other transports are available for simplicity, or ideally let original run if possible
    }
}