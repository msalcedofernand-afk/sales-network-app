package com.salesnetwork.avon.app.data

import android.content.Context
import com.salesnetwork.avon.app.domain.model.CustomerContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SupabaseCustomerApi(context: Context) {
    private val prefs = context.getSharedPreferences("leader_network_prefs", Context.MODE_PRIVATE)
    private val base = "https://xceqwexdufdgnmctsxcg.supabase.co"
    private val token get() = prefs.getString("supabase_access_token", null)

    suspend fun fetch(): List<CustomerContact> = withContext(Dispatchers.IO) {
        val auth = token ?: return@withContext emptyList()
        val body = request("/rest/v1/customers?select=id,name,phone,notes,owner_user_id&archived=eq.false&order=name", "GET", auth)
        val rows = JSONArray(body)
        buildList(rows.length()) { for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            add(CustomerContact(row.getString("id"), row.optString("name"), row.optString("phone"), row.optString("phone"), "", notes = row.optString("notes"), addedByUserId = row.optString("owner_user_id")))
        } }
    }

    suspend fun add(customer: CustomerContact, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val auth = token ?: error("Sesión no disponible")
            val membership = request("/rest/v1/team_members?user_id=eq.$userId&select=team_id&limit=1", "GET", auth)
            val teamId = JSONArray(membership).optJSONObject(0)?.optString("team_id") ?: error("Tu cuenta todavía no pertenece a un equipo.")
            request("/rest/v1/customers", "POST", auth, JSONObject().apply {
                put("team_id", teamId); put("owner_user_id", userId); put("name", customer.name)
                put("phone", customer.phone); put("notes", customer.notes)
            }.toString())
            Unit
        }
    }

    suspend fun archive(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { request("/rest/v1/customers?id=eq.$id", "PATCH", token ?: error("Sesión no disponible"), "{\"archived\":true}"); Unit }
    }

    private fun request(path: String, method: String, auth: String, payload: String? = null): String {
        val connection = (URL(base + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 8_000; readTimeout = 8_000; doOutput = payload != null
            setRequestProperty("apikey", ANON_KEY); setRequestProperty("Authorization", "Bearer $auth"); setRequestProperty("Content-Type", "application/json"); setRequestProperty("Prefer", "return=minimal")
        }
        payload?.let { connection.outputStream.use { stream -> stream.write(it.toByteArray()) } }
        val response = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) error("No pudimos guardar el cliente")
        connection.disconnect(); return response
    }

    companion object { private const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhjZXE3ZXhkdWZkZ25tY3RzeGNnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg4MzgxMjgsImV4cCI6MjEwNDQxNDEyOH0.LqPTMoS3Q1-zdsOX9CMOahMynB5XAl-AxsjrVxSlse8" }
}
