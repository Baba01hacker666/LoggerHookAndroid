package com.dct.hooklogger

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipFile

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

    fun sha256HexFromBytes(bytes: ByteArray?): String {
        if (bytes == null) {
            HookRuntime.write("INTEGRITY", "sha256HexFromBytes called with null")
            return ""
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val hex = digest.joinToString("") { "%02x".format(Locale.US, it) }
        HookRuntime.write("INTEGRITY", "sha256HexFromBytes computed SHA-256 (${bytes.size} bytes)")
        return hex
    }

    fun sha256HexFromFile(path: String?): String {
        if (path.isNullOrBlank()) {
            HookRuntime.write("INTEGRITY", "sha256HexFromFile called with empty path")
            return ""
        }
        val file = File(path)
        if (!file.exists() || !file.isFile) {
            HookRuntime.write("INTEGRITY", "sha256HexFromFile missing file: $path")
            return ""
        }
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buf = ByteArray(8192)
                while (true) {
                    val read = stream.read(buf)
                    if (read <= 0) break
                    digest.update(buf, 0, read)
                }
            }
            val hex = digest.digest().joinToString("") { "%02x".format(Locale.US, it) }
            HookRuntime.write("INTEGRITY", "sha256HexFromFile computed SHA-256 for $path")
            hex
        } catch (e: Exception) {
            HookRuntime.write("INTEGRITY", "sha256HexFromFile failed for $path: ${e.javaClass.simpleName}: ${e.message}")
            ""
        }
    }

    fun sha256ClassesDexFromApk(apkPath: String?): String {
        if (apkPath.isNullOrBlank()) {
            HookRuntime.write("INTEGRITY", "sha256ClassesDexFromApk called with empty apkPath")
            return ""
        }
        val apk = File(apkPath)
        if (!apk.exists() || !apk.isFile) {
            HookRuntime.write("INTEGRITY", "sha256ClassesDexFromApk missing APK: $apkPath")
            return ""
        }
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            ZipFile(apk).use { zip ->
                val entry = zip.getEntry("classes.dex")
                if (entry == null) {
                    HookRuntime.write("INTEGRITY", "sha256ClassesDexFromApk classes.dex not found in $apkPath")
                    return ""
                }
                zip.getInputStream(entry).use { stream: InputStream ->
                    val buf = ByteArray(8192)
                    while (true) {
                        val read = stream.read(buf)
                        if (read <= 0) break
                        digest.update(buf, 0, read)
                    }
                }
            }
            val hex = digest.digest().joinToString("") { "%02x".format(Locale.US, it) }
            HookRuntime.write("INTEGRITY", "sha256ClassesDexFromApk computed classes.dex SHA-256 for $apkPath")
            hex
        } catch (e: Exception) {
            HookRuntime.write(
                "INTEGRITY",
                "sha256ClassesDexFromApk failed for $apkPath: ${e.javaClass.simpleName}: ${e.message}"
            )
            ""
        }
    }

    fun verifySha256(expectedSha256: String?, actualSha256: String?): Boolean {
        val expected = expectedSha256?.trim()?.lowercase(Locale.US).orEmpty()
        val actual = actualSha256?.trim()?.lowercase(Locale.US).orEmpty()
        val match = expected.isNotEmpty() && expected == actual
        HookRuntime.write("INTEGRITY", "verifySha256 expected=$expected actual=$actual match=$match")
        return match
    }

    @Suppress("DEPRECATION")
    fun packageSignatures(packageName: String?): Array<String> {
        val ctx = HookRuntime.appContext
        if (ctx == null) {
            HookRuntime.write("INTEGRITY", "packageSignatures called before init")
            return emptyArray()
        }
        val pkg = packageName ?: ctx.packageName
        return try {
            val info = ctx.packageManager.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)
            val signatures = info.signatures?.map { it.toCharsString() }?.toTypedArray() ?: emptyArray()
            HookRuntime.write("INTEGRITY", "packageSignatures loaded ${signatures.size} signature(s) for $pkg")
            signatures
        } catch (e: Exception) {
            HookRuntime.write("INTEGRITY", "packageSignatures failed for $pkg: ${e.javaClass.simpleName}: ${e.message}")
            emptyArray()
        }
    }

    @Suppress("DEPRECATION")
    private fun packageSignatureBytes(packageName: String?): Array<ByteArray> {
        val ctx = HookRuntime.appContext ?: return emptyArray()
        val pkg = packageName ?: ctx.packageName
        return try {
            val info = ctx.packageManager.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)
            info.signatures?.map { it.toByteArray() }?.toTypedArray() ?: emptyArray()
        } catch (_: Exception) {
            emptyArray()
        }
    }

    fun verifyPackageSignatureSha256(packageName: String?, expectedSha256: String?): Boolean {
        val expected = expectedSha256?.trim()?.lowercase(Locale.US).orEmpty()
        if (expected.isEmpty()) {
            HookRuntime.write("INTEGRITY", "verifyPackageSignatureSha256 expected hash is empty")
            return false
        }
        val signatures = packageSignatureBytes(packageName)
        val match = signatures.any { sig ->
            val actual = sha256HexFromBytes(sig)
            actual == expected
        }
        HookRuntime.write(
            "INTEGRITY",
            "verifyPackageSignatureSha256 package=${packageName ?: "self"} expected=$expected match=$match"
        )
        return match
    }
}
