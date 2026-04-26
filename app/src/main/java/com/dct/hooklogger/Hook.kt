package com.dct.hooklogger

import android.content.Context
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

    private val io = Executors.newSingleThreadExecutor()
    private val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    /** Initialize once from Application, Activity, Service, Receiver, or any Context-bearing hook point. */
    @JvmStatic
    fun init(context: Context?) {
        if (context == null) return
        appContext = context.applicationContext ?: context
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
            Log.d(TAG, line)
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
        return try {
            value?.toString() ?: "null"
        } catch (t: Throwable) {
            "<toString failed: ${t.javaClass.name}: ${t.message}>"
        }
    }

    private inline fun safe(block: () -> Unit) {
        try {
            block()
        } catch (ignored: Throwable) {
            Log.e(TAG, "suppressed: ${ignored.javaClass.name}: ${ignored.message}")
        }
    }
}
