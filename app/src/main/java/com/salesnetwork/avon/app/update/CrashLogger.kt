package com.salesnetwork.avon.app.update

import android.content.Context
import android.os.Build
import com.salesnetwork.avon.app.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    fun reportCrashToServer(throwable: Throwable, userId: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stacktrace = sw.toString()

                val deviceInfo = JSONObject().apply {
                    put("manufacturer", Build.MANUFACTURER)
                    put("model", Build.MODEL)
                    put("sdk", Build.VERSION.SDK_INT)
                    put("release", Build.VERSION.RELEASE)
                    put("package", context.packageName)
                }

                val body = JSONObject().apply {
                    put("user_id", userId)
                    put("app_version", BuildConfig.VERSION_NAME)
                    put("version_code", BuildConfig.VERSION_CODE)
                    put("build_type", BuildConfig.BUILD_TYPE)
                    put("stacktrace", stacktrace)
                    put("device_info", deviceInfo)
                }

                val connection = (URL("https://xceqwexdufdgnmctsxcg.supabase.co/functions/v1/report-crash").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", SUPABASE_ANON_KEY)
                }
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                connection.responseCode
                connection.disconnect()
            } catch (_: Exception) { }
        }
    }

    enum class Level { DEBUG, INFO, WARN, ERROR, FATAL }

    companion object {
        private const val SUPABASE_ANON_KEY = "sb_publishable_5lm6ZqlAg7Is_DlrJ5mNnA_0DKcDmGp"

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
                    logger.reportCrashToServer(throwable)
                } catch (_: Exception) { }
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
