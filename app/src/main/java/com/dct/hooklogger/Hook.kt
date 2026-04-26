package com.dct.hooklogger

import android.content.Context
import android.os.Bundle

/**
 * Static-friendly hook runtime.
 */
object Hook {
    @JvmStatic
    fun init(context: Context?) = ProtectionHooks.init(context)

    @JvmStatic
    fun disableLogcat() = ProtectionHooks.disableLogcat()

    @JvmStatic
    fun suppressCrashes() = ProtectionHooks.suppressCrashes()

    @JvmStatic
    fun dummyExit(status: Int) = ProtectionHooks.dummyExit(status)

    @JvmStatic
    fun fakeIsDebuggerConnected(): Boolean = ProtectionHooks.fakeIsDebuggerConnected()

    @JvmStatic
    fun fakeWaitingForDebugger(): Boolean = ProtectionHooks.fakeWaitingForDebugger()

    @JvmStatic
    fun fakeIsUserAMonkey(): Boolean = ProtectionHooks.fakeIsUserAMonkey()

    @JvmStatic
    fun fakeDevelopmentSettingsEnabled(): Int = ProtectionHooks.fakeDevelopmentSettingsEnabled()

    @JvmStatic
    fun fakeAdbEnabled(): Int = ProtectionHooks.fakeAdbEnabled()

    @JvmStatic
    fun sanitizedGlobalSetting(name: String?, originalValue: Int): Int =
        ProtectionHooks.sanitizedGlobalSetting(name, originalValue)

    @JvmStatic
    fun sanitizedBuildTags(): String = ProtectionHooks.sanitizedBuildTags()

    @JvmStatic
    fun sanitizedBuildType(): String = ProtectionHooks.sanitizedBuildType()

    @JvmStatic
    fun fakeRoDebuggable(): String = ProtectionHooks.fakeRoDebuggable()

    @JvmStatic
    fun fakeGetInstallerPackageName(packageName: String?): String? =
        ProtectionHooks.fakeGetInstallerPackageName(packageName)

    @JvmStatic
    fun sanitizedRuntimeCommand(command: String?): String = RootBypassHooks.sanitizedRuntimeCommand(command)

    @JvmStatic
    fun sanitizedRuntimeCommandArgs(command: Array<String>?): Array<String> =
        RootBypassHooks.sanitizedRuntimeCommandArgs(command)

    @JvmStatic
    fun fakeFileExistsForRoot(path: String?): Boolean = RootBypassHooks.fakeFileExistsForRoot(path)

    @JvmStatic
    fun sanitizedProcMounts(content: String?): String = RootBypassHooks.sanitizedProcMounts(content)

    @JvmStatic
    fun sanitizedSystemProperty(key: String?, originalValue: String?): String =
        RootBypassHooks.sanitizedSystemProperty(key, originalValue)

    @JvmStatic
    fun sanitizeRootBeerCheck(checkName: String?, detected: Boolean): Boolean =
        RootBypassHooks.sanitizeRootBeerCheck(checkName, detected)

    @JvmStatic
    fun sanitizeEmulatorCheck(checkName: String?, detected: Boolean): Boolean =
        EmulatorBypassHooks.sanitizeEmulatorCheck(checkName, detected)

    @JvmStatic
    fun sanitizedHardware(original: String?): String = EmulatorBypassHooks.sanitizedHardware(original)

    @JvmStatic
    fun sanitizedKernelQemu(original: String?): String = EmulatorBypassHooks.sanitizedKernelQemu(original)

    @JvmStatic
    fun sanitizedFingerprint(original: String?): String = EmulatorBypassHooks.sanitizedFingerprint(original)

    @JvmStatic
    fun sanitizedImei(original: String?): String = EmulatorBypassHooks.sanitizedImei(original)

    @JvmStatic
    fun sanitizedSensorCount(originalCount: Int): Int = EmulatorBypassHooks.sanitizedSensorCount(originalCount)

    @JvmStatic
    fun sanitizedBatteryLevel(originalPercent: Int): Int = EmulatorBypassHooks.sanitizedBatteryLevel(originalPercent)

    @JvmStatic
    fun log(message: String?) = LoggingHooks.log(message)

    @JvmStatic
    fun log(tag: String?, message: String?) = LoggingHooks.log(tag, message)

    @JvmStatic
    fun kv(key: String?, value: Any?) = LoggingHooks.kv(key, value)

    @JvmStatic
    fun trace(method: String?) = LoggingHooks.trace(method)

    @JvmStatic
    fun stack(label: String?) = LoggingHooks.stack(label)

    @JvmStatic
    fun hex(label: String?, bytes: ByteArray?) = LoggingHooks.hex(label, bytes)

    @JvmStatic
    fun dumpObj(label: String?, obj: Any?) = LoggingHooks.dumpObj(label, obj)

    @JvmStatic
    fun bundle(label: String?, bundle: Bundle?) = LoggingHooks.bundle(label, bundle)

    @JvmStatic
    fun thread() = LoggingHooks.thread()

    @JvmStatic
    fun logPath(): String = LoggingHooks.logPath()

    @JvmStatic
    fun clear() = LoggingHooks.clear()
}
