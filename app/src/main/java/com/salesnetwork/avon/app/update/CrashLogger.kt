package com.salesnetwork.avon.app.update

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashLogger private constructor(private val context: Context) {

    private val logDir: File by lazy {
        File(context.filesDir, "crash_logs").also { it.mkdirs() }
    }

    fun log(level: Level, tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = buildString {
            appendLine("[$timestamp] $level/$tag: $message")
            if (throwable != null) {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                appendLine(sw.toString())
            }
        }
        appendToFile(entry)
    }

    private fun appendToFile(content: String) {
        try {
            val file = File(logDir, "app_${getDateString()}.log")
            file.appendText(content)
        } catch (_: Exception) { }
    }

    private fun getDateString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun getLatestLogFile(): File? {
        val files = logDir.listFiles()?.filter { it.name.endsWith(".log") }?.sortedByDescending { it.name }
        return files?.firstOrNull()
    }

    fun getAllLogFiles(): List<File> {
        return logDir.listFiles()?.filter { it.name.endsWith(".log") }?.sortedByDescending { it.name } ?: emptyList()
    }

    fun clearOldLogs(keepDays: Int = 7) {
        val cutoff = System.currentTimeMillis() - (keepDays * 24 * 60 * 60 * 1000L)
        logDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) file.delete()
        }
    }

    fun getCrashSummary(): String {
        val latest = getLatestLogFile() ?: return "No hay logs disponibles"
        val lines = latest.readLines()
        val crashLines = lines.filter { it.contains("FATAL") || it.contains("Exception") || it.contains("Error") }
        return if (crashLines.isNotEmpty()) {
            crashLines.takeLast(20).joinToString("\n")
        } else {
            "No se encontraron crashes en el log más reciente."
        }
    }

    enum class Level { DEBUG, INFO, WARN, ERROR, FATAL }

    companion object {
        @Volatile
        private var INSTANCE: CrashLogger? = null

        fun getInstance(context: Context): CrashLogger {
            return INSTANCE ?: synchronized(this) {
                val instance = CrashLogger(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        fun install(context: Context) {
            val logger = getInstance(context)
            logger.clearOldLogs()

            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    logger.log(
                        Level.FATAL,
                        "UncaughtException",
                        "Thread: ${thread.name}, State: ${thread.state}",
                        throwable
                    )
                    logger.log(Level.INFO, "DeviceInfo",
                        "Model: ${Build.MANUFACTURER} ${Build.MODEL}, " +
                        "SDK: ${Build.VERSION.SDK_INT}, " +
                        "Release: ${Build.VERSION.RELEASE}, " +
                        "App: ${context.packageName}"
                    )
                } catch (_: Exception) { }
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
