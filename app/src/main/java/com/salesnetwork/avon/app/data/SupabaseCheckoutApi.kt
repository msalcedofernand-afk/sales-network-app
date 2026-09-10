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
    private val base = SupabaseConfig.BASE_URL
    private val token get() = secureTokenStore.get()

    suspend fun checkout(userId: String, customerId: String, items: List<OrderItem>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val auth = token ?: error("Sesión no disponible")
            val teamRows = JSONArray(request("/rest/v1/team_members?user_id=eq.$userId&select=team_id&limit=1", "GET", auth))
            val teamId = teamRows.optJSONObject(0)?.optString("team_id") ?: error("Tu cuenta todavía no pertenece a un equipo.")
            val cart = responseObject(request(
                "/rest/v1/rpc/get_or_create_active_cart",
                "POST",
                auth,
                JSONObject().put("input_team_id", teamId).toString()
            ))
            val cartId = cart.getString("id")
            val desiredItems = JSONArray()
            items.forEach { item ->
                val products = JSONArray(request("/rest/v1/products?sku=eq." + URLEncoder.encode(item.productSku, "UTF-8") + "&select=id&limit=1", "GET", auth))
                val productId = products.optJSONObject(0)?.optString("id") ?: error("Producto no disponible: ${item.productSku}")
                desiredItems.put(JSONObject().put("product_id", productId).put("quantity", item.quantity))
            }
            request("/rest/v1/rpc/replace_cart_items", "POST", auth, JSONObject().apply {
                put("input_cart_id", cartId)
                put("input_items", desiredItems)
            }.toString())
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

    private fun responseObject(raw: String): JSONObject {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("[")) JSONArray(trimmed).getJSONObject(0) else JSONObject(trimmed)
    }

    private fun request(path: String, method: String, auth: String, payload: String? = null, prefer: String? = null): String {
        val connection = (URL(base + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 8_000; readTimeout = 15_000; doOutput = payload != null
            setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY); setRequestProperty("Authorization", "Bearer " + auth); setRequestProperty("Content-Type", "application/json")
            prefer?.let { setRequestProperty("Prefer", it) }
        }
        payload?.let { connection.outputStream.use { stream -> stream.write(it.toByteArray()) } }
        val response = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) error("No pudimos confirmar el pedido")
        connection.disconnect()
        return response
    }

}
