package com.salesnetwork.avon.app.update

import com.salesnetwork.avon.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val channel: String,
    val apkUrl: String,
    val releaseNotes: String,
    val mandatory: Boolean
)

class AppUpdateChecker {
    suspend fun check(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val channel = BuildConfig.UPDATE_CHANNEL
        // GitHub is private, so an installed APK cannot read raw files anonymously.
        // Vercel publishes these manifests from reviewed Git commits.
        val endpoint = "https://sales-network-app.vercel.app/updates/$channel.json"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "SalesNetwork-Android/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val payload = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(payload)
            val available = AppUpdateInfo(
                versionCode = json.optInt("versionCode", 0),
                versionName = json.optString("versionName", ""),
                channel = json.optString("channel", channel),
                apkUrl = json.optString("apkUrl", ""),
                releaseNotes = json.optString("releaseNotes", "Nueva versión disponible."),
                mandatory = json.optBoolean("mandatory", false)
            )
            if (available.channel != channel || available.versionCode <= BuildConfig.VERSION_CODE || available.apkUrl.isBlank()) null else available
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
