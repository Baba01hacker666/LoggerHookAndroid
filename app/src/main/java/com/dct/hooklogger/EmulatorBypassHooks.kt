package com.dct.hooklogger

import android.os.Build
import java.util.Locale

internal object EmulatorBypassHooks {
    fun sanitizeEmulatorCheck(checkName: String?, detected: Boolean): Boolean {
        HookRuntime.write("EMU_BYPASS", "sanitizeEmulatorCheck '${checkName ?: "unknown"}' input=$detected output=false")
        return false
    }

    fun sanitizedModel(original: String?): String {
        val raw = original ?: Build.MODEL ?: ""
        val l = raw.lowercase(Locale.US)
        val suspicious = listOf("sdk", "emulator", "google_sdk", "vbox", "test-keys").any { l.contains(it) }
        val out = if (suspicious) "Pixel 7" else raw
        HookRuntime.write("EMU_BYPASS", "sanitizedModel input='$raw' output='$out'")
        return out
    }

    fun sanitizedManufacturer(original: String?): String {
        val raw = original ?: Build.MANUFACTURER ?: ""
        val l = raw.lowercase(Locale.US)
        val suspicious = listOf("unknown", "genymotion", "netease", "tiantian", "nox", "bignox").any { l.contains(it) }
        val out = if (suspicious) "Google" else raw
        HookRuntime.write("EMU_BYPASS", "sanitizedManufacturer input='$raw' output='$out'")
        return out
    }

    fun sanitizedBrand(original: String?): String {
        val raw = original ?: Build.BRAND ?: ""
        val l = raw.lowercase(Locale.US)
        val suspicious = listOf("generic", "google", "nox", "bignox", "netease").any { l.contains(it) }
        val out = if (suspicious) "google" else raw
        HookRuntime.write("EMU_BYPASS", "sanitizedBrand input='$raw' output='$out'")
        return out
    }

    fun sanitizedDevice(original: String?): String {
        val raw = original ?: Build.DEVICE ?: ""
        val l = raw.lowercase(Locale.US)
        val suspicious = listOf("generic", "vbox", "emulator", "nox", "bignox").any { l.contains(it) }
        val out = if (suspicious) "panther" else raw
        HookRuntime.write("EMU_BYPASS", "sanitizedDevice input='$raw' output='$out'")
        return out
    }

    fun sanitizedProduct(original: String?): String {
        val raw = original ?: Build.PRODUCT ?: ""
        val l = raw.lowercase(Locale.US)
        val suspicious = listOf("sdk", "google_sdk", "emulator", "vbox", "nox", "bignox").any { l.contains(it) }
        val out = if (suspicious) "panther" else raw
        HookRuntime.write("EMU_BYPASS", "sanitizedProduct input='$raw' output='$out'")
        return out
    }

    fun sanitizedBoard(original: String?): String {
        val raw = original ?: Build.BOARD ?: ""
        val l = raw.lowercase(Locale.US)
        val suspicious = listOf("unknown", "goldfish", "vbox", "nox", "bignox").any { l.contains(it) }
        val out = if (suspicious) "panther" else raw
        HookRuntime.write("EMU_BYPASS", "sanitizedBoard input='$raw' output='$out'")
        return out
    }

    fun sanitizedHardware(original: String?): String {
        val raw = original ?: Build.HARDWARE ?: ""
        val l = raw.lowercase(Locale.US)
        val suspicious = listOf("goldfish", "ranchu", "vbox", "emulator", "qemu").any { l.contains(it) }
        val out = if (suspicious) "qcom" else raw
        HookRuntime.write("EMU_BYPASS", "sanitizedHardware input='$raw' output='$out'")
        return out
    }

    fun sanitizedKernelQemu(original: String?): String {
        val out = if ((original ?: "").trim() == "1") "0" else (original ?: "0")
        HookRuntime.write("EMU_BYPASS", "sanitizedKernelQemu input='${original ?: "null"}' output='$out'")
        return out
    }

    fun sanitizedFingerprint(original: String?): String {
        val raw = original ?: Build.FINGERPRINT ?: ""
        val l = raw.lowercase(Locale.US)
        val suspicious = listOf("generic", "emulator", "sdk_gphone", "vbox", "test-keys").any { l.contains(it) }
        val out = if (suspicious) "google/pixel/pixel:14/UQ1A.240205.002/1234567:user/release-keys" else raw
        HookRuntime.write("EMU_BYPASS", "sanitizedFingerprint output='${out.take(48)}...'")
        return out
    }

    fun sanitizedImei(original: String?): String {
        val value = original?.trim().orEmpty()
        val out = if (value.isEmpty() || value.all { it == '0' }) "356938035643809" else value
        HookRuntime.write("EMU_BYPASS", "sanitizedImei input='${if (value.isEmpty()) "<empty>" else value}' output='$out'")
        return out
    }

    fun sanitizedSensorCount(originalCount: Int): Int {
        val out = if (originalCount < 8) 12 else originalCount
        HookRuntime.write("EMU_BYPASS", "sanitizedSensorCount input=$originalCount output=$out")
        return out
    }

    fun sanitizedBatteryLevel(originalPercent: Int): Int {
        val bounded = originalPercent.coerceIn(0, 100)
        val out = if (bounded <= 5) 77 else bounded
        HookRuntime.write("EMU_BYPASS", "sanitizedBatteryLevel input=$originalPercent output=$out")
        return out
    }
}
