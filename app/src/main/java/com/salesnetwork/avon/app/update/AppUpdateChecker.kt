package com.salesnetwork.avon.app.update

import android.net.Uri
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
        val branch = if (channel == "beta") "beta" else "main"
        val endpoints = listOf(
            "https://raw.githubusercontent.com/msalcedofernand-afk/sales-network-app-releases/${branch}/updates/${channel}.json",
            "https://sales-network-app.vercel.app/updates/${channel}.json"
        )
        try {
            var payload: String? = null
            for (endpoint in endpoints) {
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000
                    readTimeout = 5_000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "SalesNetwork-Android/${BuildConfig.VERSION_NAME}")
                }
                try {
                    if (connection.responseCode in 200..299) {
                        payload = connection.inputStream.bufferedReader().use { it.readText() }
                        break
                    }
                } finally { connection.disconnect() }
            }
            if (payload == null) return@withContext null
            val json = JSONObject(payload)
            val apkUrl = json.optString("apkUrl", "")
            val available = AppUpdateInfo(
                versionCode = json.optInt("versionCode", 0),
                versionName = json.optString("versionName", ""),
                channel = json.optString("channel", channel),
                apkUrl = apkUrl,
                releaseNotes = json.optString("releaseNotes", "Nueva versión disponible."),
                mandatory = json.optBoolean("mandatory", false)
            )
            if (
                available.channel != channel ||
                available.versionCode <= BuildConfig.VERSION_CODE ||
                !isApprovedDownload(apkUrl, channel)
            ) null else available
        } catch (_: Exception) { null }
    }

    private fun isApprovedDownload(value: String, channel: String): Boolean {
        val uri = Uri.parse(value)
        if (uri.scheme != "https" || uri.host != "raw.githubusercontent.com") return false
        val expectedPrefix = "/msalcedofernand-afk/sales-network-app-releases/"
        val expectedChannelPath = "/releases/sales-network-${channel}.apk"
        return uri.path?.startsWith(expectedPrefix) == true && uri.path?.endsWith(expectedChannelPath) == true
    }
}

