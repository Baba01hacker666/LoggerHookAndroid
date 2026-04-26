package com.dct.hooklogger

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

internal object HookRuntime {
    private const val TAG = "DCT-HOOK"
    private const val LOG_FILE_NAME = "dct_hook.log"

    @Volatile
    var appContext: Context? = null

    @Volatile
    var useLogcat = true

    private val io = Executors.newSingleThreadExecutor()
    private val timeFormatter = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    }

    fun write(tag: String, message: String) {
        safe {
            val line = "${timeFormatter.get().format(Date())} [$tag] $message"
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

    fun getLogFile(): File? {
        val ctx = appContext

        if (ctx != null) {
            val dir = ctx.getExternalFilesDir(null) ?: ctx.filesDir
            return File(dir, LOG_FILE_NAME)
        }

        @Suppress("DEPRECATION")
        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        return File(File(docs, "DCTHookLogger"), LOG_FILE_NAME)
    }

    fun safeString(value: Any?): String {
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

    inline fun safe(block: () -> Unit) {
        try {
            block()
        } catch (ignored: Throwable) {
            if (useLogcat) {
                Log.e(TAG, "suppressed: ${ignored.javaClass.name}: ${ignored.message}")
            }
        }
    }
}
