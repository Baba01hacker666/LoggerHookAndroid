package com.dct.hooklogger

import java.io.File
import java.util.Locale

internal object RootBypassHooks {
    fun sanitizedRuntimeCommand(command: String?): String {
        val input = command?.trim().orEmpty()
        if (input.isEmpty()) {
            HookRuntime.write("ROOT_BYPASS", "sanitizedRuntimeCommand input='<empty>' output='' ")
            return input
        }

        val parts = input.split("\\s+".toRegex(), limit = 2)
        val exe = parts[0]
        val tail = if (parts.size > 1) parts[1] else ""

        val loweredExe = exe.lowercase(Locale.US)
        val suspiciousExe = loweredExe == "su" || loweredExe.endsWith("/su") || loweredExe.contains("magisk")

        val safeExe = if (suspiciousExe) "sh" else exe
        val sanitized = if (tail.isNotEmpty()) "$safeExe $tail" else safeExe

        HookRuntime.write("ROOT_BYPASS", "sanitizedRuntimeCommand input='$input' output='$sanitized'")
        return sanitized
    }

    fun sanitizedRuntimeCommandArgs(command: Array<String>?): Array<String> {
        if (command == null || command.isEmpty()) {
            HookRuntime.write("ROOT_BYPASS", "sanitizedRuntimeCommandArgs input=<null_or_empty>")
            return command ?: emptyArray()
        }

        val copied = command.copyOf()
        val exe = copied[0]
        val loweredExe = exe.lowercase(Locale.US)
        val suspiciousExe = loweredExe == "su" || loweredExe.endsWith("/su") || loweredExe.contains("magisk")
        if (suspiciousExe) {
            copied[0] = "sh"
            HookRuntime.write("ROOT_BYPASS", "sanitizedRuntimeCommandArgs replaced argv0='$exe' -> 'sh'")
        } else {
            HookRuntime.write("ROOT_BYPASS", "sanitizedRuntimeCommandArgs passthrough argv0='$exe'")
        }
        return copied
    }

    fun fakeFileExistsForRoot(path: String?): Boolean {
        val p = path?.trim().orEmpty()
        val lowered = p.lowercase(Locale.US)
        val rootArtifacts = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/system/app/superuser.apk",
            "/system/etc/init/magisk",
            "/proc/mounts",
            "magisk"
        )
        val forceFalse = rootArtifacts.any { lowered.contains(it) }
        if (forceFalse) {
            HookRuntime.write("ROOT_BYPASS", "fakeFileExistsForRoot intercepted path='$p', returning false")
            return false
        }
        val exists = try {
            File(p).exists()
        } catch (_: Throwable) {
            false
        }
        HookRuntime.write("ROOT_BYPASS", "fakeFileExistsForRoot path='$p', passthrough=$exists")
        return exists
    }

    fun sanitizedProcMounts(content: String?): String {
        if (content == null) {
            HookRuntime.write("ROOT_BYPASS", "sanitizedProcMounts called with null")
            return ""
        }
        val filtered = content
            .lineSequence()
            .filterNot { line ->
                val l = line.lowercase(Locale.US)
                l.contains("magisk") || l.contains("overlay") && l.contains("/system")
            }
            .joinToString("\n")
        HookRuntime.write("ROOT_BYPASS", "sanitizedProcMounts filtered=${content.length - filtered.length} chars")
        return filtered
    }

    fun sanitizedSystemProperty(key: String?, originalValue: String?): String {
        val k = key?.trim().orEmpty()
        val fake = when (k) {
            "ro.build.tags" -> "release-keys"
            "ro.secure" -> "1"
            "ro.debuggable" -> "0"
            "service.adb.root" -> "0"
            "ro.boot.verifiedbootstate" -> "green"
            else -> null
        }
        val out = fake ?: (originalValue ?: "")
        HookRuntime.write("ROOT_BYPASS", "sanitizedSystemProperty key='$k' returning '$out'")
        return out
    }

    fun sanitizeRootBeerCheck(checkName: String?, detected: Boolean): Boolean {
        if (detected) {
            HookRuntime.write("ROOT_BYPASS", "sanitizeRootBeerCheck '${checkName ?: "unknown"}' intercepted true -> false")
        } else {
            HookRuntime.write("ROOT_BYPASS", "sanitizeRootBeerCheck '${checkName ?: "unknown"}' passthrough false")
        }
        return false
    }
}
