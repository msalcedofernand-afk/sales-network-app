package com.salesnetwork.avon.app.update

import android.content.Context
import android.content.SharedPreferences
import com.salesnetwork.avon.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URI

data class AppUpdateInfo(
    val schemaVersion: Int,
    val versionCode: Int,
    val versionName: String,
    val channel: String,
    val apkUrl: String,
    val sha256: String,
    val minSupportedVersionCode: Int,
    val releasedAt: String,
    val releaseNotes: List<String>
) {
    val mandatory: Boolean get() = BuildConfig.VERSION_CODE < minSupportedVersionCode
    val notesText: String get() = releaseNotes.joinToString(" · ")
}

class AppUpdateChecker(private val context: Context? = null) {
    private val prefs: SharedPreferences?
        get() = context?.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val CHECK_INTERVAL_MS = 60 * 60 * 1000L
        private const val CACHE_KEY = "last_valid_manifest"
        private const val CHECKED_AT_KEY = "last_successful_check"

        internal fun parseManifest(payload: String, expectedChannel: String): AppUpdateInfo? = runCatching {
            val json = JSONObject(payload)
            val notesJson = json.getJSONArray("releaseNotes")
            val notes = buildList { for (index in 0 until notesJson.length()) add(notesJson.getString(index).trim()) }
                .filter { it.isNotBlank() }
            val info = AppUpdateInfo(
                schemaVersion = json.getInt("schemaVersion"),
                versionCode = json.getInt("versionCode"),
                versionName = json.getString("versionName").trim(),
                channel = json.getString("channel").trim(),
                apkUrl = json.getString("apkUrl").trim(),
                sha256 = json.getString("sha256").lowercase(),
                minSupportedVersionCode = json.getInt("minSupportedVersionCode"),
                releasedAt = json.getString("releasedAt").trim(),
                releaseNotes = notes
            )
            require(info.schemaVersion == 1)
            require(info.versionCode > 0 && info.minSupportedVersionCode in 1..info.versionCode)
            require(info.versionName.isNotBlank() && info.channel == expectedChannel)
            require(info.releasedAt.isNotBlank() && info.releaseNotes.isNotEmpty())
            require(info.sha256.matches(Regex("^[a-f0-9]{64}$")) && info.sha256.any { it != '0' })
            require(isApprovedDownload(info.apkUrl, expectedChannel))
            info
        }.getOrNull()

        internal fun selectUpdate(candidates: List<AppUpdateInfo>, currentVersionCode: Int): AppUpdateInfo? =
            candidates.maxWithOrNull(compareBy<AppUpdateInfo> { it.versionCode }.thenBy { it.releasedAt })
                ?.takeIf { it.versionCode > currentVersionCode || currentVersionCode < it.minSupportedVersionCode }

        internal fun isApprovedDownload(value: String, channel: String): Boolean {
            val uri = runCatching { URI(value) }.getOrNull() ?: return false
            if (uri.scheme != "https" || uri.host != "raw.githubusercontent.com") return false
            val branch = if (channel == "beta") "beta" else "main"
            val expected = "/msalcedofernand-afk/sales-network-app-releases/$branch/releases/sales-network-$channel.apk"
            return uri.path == expected && uri.query == null && uri.fragment == null
        }
    }

    suspend fun check(forceNetwork: Boolean = false): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val channel = BuildConfig.UPDATE_CHANNEL
        val now = System.currentTimeMillis()
        val lastSuccess = prefs?.getLong(CHECKED_AT_KEY, 0L) ?: 0L
        if (!forceNetwork && now - lastSuccess < CHECK_INTERVAL_MS) {
            return@withContext prefs?.getString(CACHE_KEY, null)
                ?.let { parseManifest(it, channel) }
                ?.let { selectUpdate(listOf(it), BuildConfig.VERSION_CODE) }
        }

        val branch = if (channel == "beta") "beta" else "main"
        val endpoints = listOf(
            "https://raw.githubusercontent.com/msalcedofernand-afk/sales-network-app-releases/$branch/updates/$channel.json",
            "https://sales-network-app.vercel.app/updates/$channel.json"
        )
        val validResponses = endpoints.mapNotNull { endpoint -> fetch(endpoint)?.let { parseManifest(it, channel) } }
        if (validResponses.isEmpty()) return@withContext null

        val selected = validResponses.maxWith(compareBy<AppUpdateInfo> { it.versionCode }.thenBy { it.releasedAt })
        prefs?.edit()
            ?.putLong(CHECKED_AT_KEY, now)
            ?.putString(CACHE_KEY, toJson(selected))
            ?.apply()
        selectUpdate(validResponses, BuildConfig.VERSION_CODE)
    }

    private fun fetch(endpoint: String): String? = runCatching {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "SalesNetwork-Android/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (connection.responseCode !in 200..299) null
            else connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    private fun toJson(info: AppUpdateInfo) = JSONObject().apply {
        put("schemaVersion", info.schemaVersion)
        put("versionCode", info.versionCode)
        put("versionName", info.versionName)
        put("channel", info.channel)
        put("apkUrl", info.apkUrl)
        put("sha256", info.sha256)
        put("minSupportedVersionCode", info.minSupportedVersionCode)
        put("releasedAt", info.releasedAt)
        put("releaseNotes", org.json.JSONArray(info.releaseNotes))
    }.toString()
}
