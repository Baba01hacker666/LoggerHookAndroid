package com.dct.hooklogger

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Static-friendly hook runtime.
 *
 * Smali call examples:
 *   invoke-static {v0}, Lcom/dct/hooklogger/Hook;->log(Ljava/lang/String;)V
 *   invoke-static {v0, v1}, Lcom/dct/hooklogger/Hook;->log(Ljava/lang/String;Ljava/lang/String;)V
 *   invoke-static {v0, v1}, Lcom/dct/hooklogger/Hook;->kv(Ljava/lang/String;Ljava/lang/Object;)V
 */
object Hook {
    private const val TAG = "DCT-HOOK"
    private const val LOG_FILE_NAME = "dct_hook.log"

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var useLogcat = true

    private val io = Executors.newSingleThreadExecutor()
    private val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    /** Initialize once from Application, Activity, Service, Receiver, or any Context-bearing hook point. */
    @JvmStatic
    fun init(context: Context?) {
        if (context == null) return
        appContext = context.applicationContext ?: context
    }

    /** Disable Logcat output to avoid detection by runtime protections. */
    @JvmStatic
    fun disableLogcat() {
        useLogcat = false
        write("PROTECTION", "Logcat output disabled")
    }

    /** Suppress uncaught exceptions to prevent app from purposely crashing. */
    @JvmStatic
    fun suppressCrashes() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            write("CRASH_SUPPRESSED", "Thread: ${t.name}, Exception: ${e.javaClass.name}: ${e.message}")
            // Do not call defaultHandler to prevent crash
        }
        write("PROTECTION", "Crash suppression enabled")
    }

    /** Replace System.exit() calls with this to prevent app from killing itself. */
    @JvmStatic
    fun dummyExit(status: Int) {
        write("DUMMY_EXIT", "System.exit($status) called and intercepted.")
    }

    /** Replace android.os.Debug.isDebuggerConnected() calls with this. */
    @JvmStatic
    fun fakeIsDebuggerConnected(): Boolean {
        write("DEBUGGER", "fakeIsDebuggerConnected called, returning false")
        return false
    }

    /** Replace android.os.Debug.waitingForDebugger() calls with this. */
    @JvmStatic
    fun fakeWaitingForDebugger(): Boolean {
        write("DEBUGGER", "fakeWaitingForDebugger called, returning false")
        return false
    }

    /** Replace android.app.ActivityManager.isUserAMonkey() calls with this. */
    @JvmStatic
    fun fakeIsUserAMonkey(): Boolean {
        write("ENV", "fakeIsUserAMonkey called, returning false")
        return false
    }

    /** Replace Build.TAGS checks with this method (avoids 'test-keys' detections). */
    @JvmStatic
    fun sanitizedBuildTags(): String {
        val original = Build.TAGS ?: "null"
        val sanitized = original.replace("test-keys", "release-keys")
        write("ENV", "sanitizedBuildTags called, returning $sanitized")
        return sanitized
    }

    /** Replace Build.TYPE checks with this method (avoids 'eng' / 'userdebug' detections). */
    @JvmStatic
    fun sanitizedBuildType(): String {
        val original = Build.TYPE ?: "null"
        val sanitized = if (original == "eng" || original == "userdebug") "user" else original
        write("ENV", "sanitizedBuildType called, returning $sanitized")
        return sanitized
    }

    /** Replace SystemProperties.get("ro.debuggable") checks with this. */
    @JvmStatic
    fun fakeRoDebuggable(): String {
        write("ENV", "fakeRoDebuggable called, returning 0")
        return "0"
    }

    /** Universal one-argument logger. */
    @JvmStatic
    fun log(message: String?) {
        write("LOG", message ?: "null")
    }

    /** Tagged logger. */
    @JvmStatic
    fun log(tag: String?, message: String?) {
        write(tag ?: "LOG", message ?: "null")
    }

    /** Key-value logger; accepts any object from smali. */
    @JvmStatic
    fun kv(key: String?, value: Any?) {
        write("KV", "${key ?: "null"}=${safeString(value)}")
    }

    /** Method trace helper. */
    @JvmStatic
    fun trace(method: String?) {
        write("TRACE", method ?: "unknown")
    }

    /** Throwable/string stack trace logger. */
    @JvmStatic
    fun stack(label: String?) {
        val sb = StringBuilder()
        sb.append(label ?: "stack").append('\n')
        Throwable().stackTrace.forEach { sb.append("  at ").append(it).append('\n') }
        write("STACK", sb.toString())
    }

    /** Hex dump logger. */
    @JvmStatic
    fun hex(label: String?, bytes: ByteArray?) {
        if (bytes == null) {
            write("HEX", "${label ?: "hex"}: null")
            return
        }
        val sb = StringBuilder()
        sb.append(label ?: "hex").append(" (").append(bytes.size).append(" bytes):\n")
        val hexChars = "0123456789ABCDEF".toCharArray()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            sb.append(hexChars[v ushr 4]).append(hexChars[v and 0x0F]).append(" ")
            if ((i + 1) % 16 == 0) sb.append("\n")
        }
        write("HEX", sb.toString())
    }

    /** Reflection-based object dump. */
    @JvmStatic
    fun dumpObj(label: String?, obj: Any?) {
        if (obj == null) {
            write("DUMP", "${label ?: "obj"}: null")
            return
        }
        val sb = StringBuilder()
        sb.append(label ?: "obj").append(" (").append(obj.javaClass.name).append("):\n")
        try {
            var clazz: Class<*>? = obj.javaClass
            while (clazz != null && clazz != Any::class.java) {
                val fields = clazz.declaredFields
                for (field in fields) {
                    field.isAccessible = true
                    val value = field.get(obj)
                    sb.append("  ").append(field.name).append(" = ").append(safeString(value)).append("\n")
                }
                clazz = clazz.superclass
            }
        } catch (t: Throwable) {
            sb.append("  <dump failed: ${t.javaClass.name}: ${t.message}>\n")
        }
        write("DUMP", sb.toString())
    }

    /** Log Android Bundle contents. */
    @JvmStatic
    fun bundle(label: String?, bundle: Bundle?) {
        if (bundle == null) {
            write("BUNDLE", "${label ?: "bundle"}: null")
            return
        }
        val sb = StringBuilder()
        sb.append(label ?: "bundle").append(":\n")
        try {
            for (key in bundle.keySet()) {
                val value = bundle.get(key)
                sb.append("  ").append(key).append(" = ").append(safeString(value)).append("\n")
            }
        } catch (t: Throwable) {
            sb.append("  <bundle parse failed: ${t.javaClass.name}: ${t.message}>\n")
        }
        write("BUNDLE", sb.toString())
    }

    /** Log current executing thread name and ID. */
    @JvmStatic
    fun thread() {
        val t = Thread.currentThread()
        write("THREAD", "id=${t.id}, name=${t.name}")
    }

    /** Returns the preferred log file path for quick debugging. */
    @JvmStatic
    fun logPath(): String {
        return getLogFile()?.absolutePath ?: "uninitialized"
    }

    /** Clears current log file. */
    @JvmStatic
    fun clear() {
        safe {
            getLogFile()?.writeText("")
        }
    }

    private fun write(tag: String, message: String) {
        safe {
            val line = "${time.format(Date())} [$tag] $message"
            if (useLogcat) {
                Log.d(TAG, line)
            }
            io.execute {
                safe {
                    val file = getLogFile() ?: return@safe
                    file.parentFile?.mkdirs()
                    file.appendText(line + "\n")
                }
            }
        }
    }

    private fun getLogFile(): File? {
        val ctx = appContext

        // Best default: app-private external storage. No runtime permission required.
        if (ctx != null) {
            val dir = ctx.getExternalFilesDir(null) ?: ctx.filesDir
            return File(dir, LOG_FILE_NAME)
        }

        // Fallback for very early/static calls before init(). Works on older Android only.
        @Suppress("DEPRECATION")
        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        return File(File(docs, "DCTHookLogger"), LOG_FILE_NAME)
    }

    private fun safeString(value: Any?): String {
        if (value == null) return "null"
        return try {
            when (value) {
                is ByteArray -> value.contentToString()
                is ShortArray -> value.contentToString()
                is IntArray -> value.contentToString()
                is LongArray -> value.contentToString()
                is FloatArray -> value.contentToString()
                is DoubleArray -> value.contentToString()
                is BooleanArray -> value.contentToString()
                is CharArray -> value.contentToString()
                is Array<*> -> value.contentDeepToString()
                else -> value.toString()
            }
        } catch (t: Throwable) {
            "<toString failed: ${t.javaClass.name}: ${t.message}>"
        }
    }

    private inline fun safe(block: () -> Unit) {
        try {
            block()
        } catch (ignored: Throwable) {
            if (useLogcat) {
                Log.e(TAG, "suppressed: ${ignored.javaClass.name}: ${ignored.message}")
            }
        }
    }
}
