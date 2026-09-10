package com.salesnetwork.avon.app.update

import android.content.Context
import android.os.Build
import com.salesnetwork.avon.app.BuildConfig
import com.salesnetwork.avon.app.data.SecureTokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL

/** Stores only a sanitised, encrypted pending event until an authenticated session can send it. */
class CrashLogger private constructor(private val context: Context) {
    private val secureStore = SecureTokenStore(context)

    fun log(level: Level, tag: String, message: String, throwable: Throwable? = null) {
        // Deliberately keep local diagnostics minimal: no account data, orders or tokens.
        android.util.Log.println(level.androidPriority, tag.take(40), message.take(240))
        throwable?.let { android.util.Log.e(tag.take(40), it.javaClass.simpleName) }
    }

    fun reportCrashToServer(throwable: Throwable) {
        val stacktrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
            .sanitize().take(MAX_STACKTRACE_CHARS)
        val payload = JSONObject().apply {
            put("app_version", BuildConfig.VERSION_NAME)
            put("version_code", BuildConfig.VERSION_CODE)
            put("build_type", BuildConfig.BUILD_TYPE)
            put("channel", BuildConfig.UPDATE_CHANNEL)
            put("platform", "android")
            put("exception_type", throwable.javaClass.name.take(160))
            put("stacktrace", stacktrace)
            put("device_info", JSONObject().apply {
                put("model", Build.MODEL.take(100))
                put("os_version", Build.VERSION.RELEASE.take(40))
                put("sdk", Build.VERSION.SDK_INT)
            })
        }.toString()
        secureStore.savePendingCrash(payload)
        flushPending()
    }

    fun flushPending() {
        val session = secureStore.getSession() ?: return
        val payload = secureStore.getPendingCrash() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val delivered = runCatching {
                val connection = (URL("$SUPABASE_URL/functions/v1/report-crash").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; doOutput = true; connectTimeout = 5_000; readTimeout = 5_000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer ${session.accessToken}")
                }
                connection.outputStream.use { it.write(payload.toByteArray()) }
                val successful = connection.responseCode in 200..299
                connection.disconnect()
                successful
            }.getOrDefault(false)
            if (delivered) secureStore.clearPendingCrash()
        }
    }

    enum class Level(val androidPriority: Int) {
        DEBUG(android.util.Log.DEBUG), INFO(android.util.Log.INFO), WARN(android.util.Log.WARN),
        ERROR(android.util.Log.ERROR), FATAL(android.util.Log.ASSERT)
    }

    companion object {
        private const val SUPABASE_URL = "https://xceqwexdufdgnmctsxcg.supabase.co"
        private const val SUPABASE_ANON_KEY = "sb_publishable_5lm6ZqlAg7Is_DlrJ5mNnA_0DKcDmGp"
        private const val MAX_STACKTRACE_CHARS = 30_000

        @Volatile private var INSTANCE: CrashLogger? = null
        fun getInstance(context: Context): CrashLogger = INSTANCE ?: synchronized(this) {
            CrashLogger(context.applicationContext).also { INSTANCE = it }
        }

        fun install(context: Context) {
            val logger = getInstance(context)
            logger.flushPending()
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
                runCatching { logger.reportCrashToServer(throwable) }
                previous?.uncaughtException(Thread.currentThread(), throwable)
            }
        }

        private fun String.sanitize(): String = this
            .replace(Regex("Bearer\\s+[A-Za-z0-9._-]+", RegexOption.IGNORE_CASE), "Bearer [redacted]")
            .replace(Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE), "[email redacted]")
    }
}
