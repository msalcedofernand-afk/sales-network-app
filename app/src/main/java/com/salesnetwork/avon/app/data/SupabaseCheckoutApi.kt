package com.salesnetwork.avon.app.data

import android.content.Context
import com.salesnetwork.avon.app.domain.model.OrderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SupabaseCheckoutApi(context: Context) {
    private val secureTokenStore = SecureTokenStore(context)
    private val checkoutPrefs = context.getSharedPreferences("checkout_attempts", Context.MODE_PRIVATE)
    private val base = "https://xceqwexdufdgnmctsxcg.supabase.co"
    private val token get() = secureTokenStore.get()

    suspend fun checkout(userId: String, customerId: String, items: List<OrderItem>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val auth = token ?: error("Sesión no disponible")
            val teamRows = JSONArray(request("/rest/v1/team_members?user_id=eq.$userId&select=team_id&limit=1", "GET", auth))
            val teamId = teamRows.optJSONObject(0)?.optString("team_id") ?: error("Tu cuenta todavía no pertenece a un equipo.")
            val carts = JSONArray(request("/rest/v1/carts?user_id=eq.$userId&team_id=eq.$teamId&status=eq.ACTIVE&select=id&limit=1", "GET", auth))
            val cartId = if (carts.length() > 0) carts.getJSONObject(0).getString("id") else {
                val created = JSONArray(request("/rest/v1/carts", "POST", auth, JSONObject().apply { put("team_id", teamId); put("user_id", userId) }.toString(), "return=representation"))
                created.getJSONObject(0).getString("id")
            }
            items.forEach { item ->
                val products = JSONArray(request("/rest/v1/products?sku=eq." + URLEncoder.encode(item.productSku, "UTF-8") + "&select=id&limit=1", "GET", auth))
                val productId = products.optJSONObject(0)?.optString("id") ?: error("Producto no disponible: ${item.productSku}")
                request("/rest/v1/cart_items", "POST", auth, JSONObject().apply {
                    put("cart_id", cartId); put("product_id", productId); put("quantity", item.quantity)
                }.toString(), "resolution=merge-duplicates,return=minimal")
            }
            // Reuse the key until the server confirms the checkout. If the response is
            // lost, retrying the same cart remains idempotent instead of creating a duplicate.
            val keyName = "checkout_key_$cartId"
            val key = checkoutPrefs.getString(keyName, null) ?: ("android-" + java.util.UUID.randomUUID()).also {
                checkoutPrefs.edit().putString(keyName, it).apply()
            }
            request("/functions/v1/checkout-cart", "POST", auth, JSONObject().apply {
                put("cart_id", cartId); put("customer_id", customerId); put("idempotency_key", key)
            }.toString())
            checkoutPrefs.edit().remove(keyName).apply()
            Unit
        }
    }

    private fun request(path: String, method: String, auth: String, payload: String? = null, prefer: String? = null): String {
        val connection = (URL(base + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 8_000; readTimeout = 15_000; doOutput = payload != null
            setRequestProperty("apikey", ANON_KEY); setRequestProperty("Authorization", "Bearer " + auth); setRequestProperty("Content-Type", "application/json")
            prefer?.let { setRequestProperty("Prefer", it) }
        }
        payload?.let { connection.outputStream.use { stream -> stream.write(it.toByteArray()) } }
        val response = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) error("No pudimos confirmar el pedido")
        connection.disconnect()
        return response
    }

    companion object { private const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhjZXE3ZXhkdWZkZ25tY3RzeGNnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg4MzgxMjgsImV4cCI6MjEwNDQxNDEyOH0.LqPTMoS3Q1-zdsOX9CMOahMynB5XAl-AxsjrVxSlse8" }
}
