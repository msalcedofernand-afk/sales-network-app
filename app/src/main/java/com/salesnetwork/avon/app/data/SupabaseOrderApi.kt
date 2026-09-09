package com.salesnetwork.avon.app.data

import android.content.Context
import com.salesnetwork.avon.app.domain.model.Order
import com.salesnetwork.avon.app.domain.model.OrderItem
import com.salesnetwork.avon.app.domain.model.OrderStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class SupabaseOrderApi(context: Context) {
    private val prefs = context.getSharedPreferences("leader_network_prefs", Context.MODE_PRIVATE)
    private val base = "https://xceqwexdufdgnmctsxcg.supabase.co"
    private val token get() = prefs.getString("supabase_access_token", null)

    suspend fun fetch(): List<Order> = withContext(Dispatchers.IO) {
        val auth = token ?: return@withContext emptyList()
        val orders = JSONArray(request("/rest/v1/orders?select=id,customer_id,user_id,status,total_cents,commission_cents,created_at&order=created_at.desc", auth))
        val ids = (0 until orders.length()).map { orders.getJSONObject(it).getString("id") }
        val items = if (ids.isEmpty()) JSONArray() else JSONArray(request("/rest/v1/order_items?select=order_id,sku,product_name,quantity,unit_price_cents&order_id=in.(" + ids.joinToString(",") + ")", auth))
        buildList(orders.length()) {
            for (i in 0 until orders.length()) {
                val row = orders.getJSONObject(i)
                val id = row.getString("id")
                val lines = buildList {
                    for (j in 0 until items.length()) {
                        val item = items.getJSONObject(j)
                        if (item.optString("order_id") == id) add(OrderItem(item.optString("sku"), item.optString("product_name"), item.optInt("unit_price_cents") / 100.0, item.optInt("quantity")))
                    }
                }
                val status = runCatching { OrderStatus.valueOf(row.optString("status")) }.getOrDefault(OrderStatus.PENDIENTE)
                add(Order(id, row.optString("customer_id"), "Cliente", row.optString("user_id"), totalAmount = row.optInt("total_cents") / 100.0, commissionLeader = row.optInt("commission_cents") / 100.0, items = lines, status = status, createdAt = row.optString("created_at").take(10)))
            }
        }
    }

    private fun request(path: String, auth: String): String {
        val connection = (URL(base + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 8_000; readTimeout = 8_000
            setRequestProperty("apikey", ANON_KEY); setRequestProperty("Authorization", "Bearer " + auth); setRequestProperty("Accept", "application/json")
        }
        val response = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) error("No pudimos cargar los pedidos")
        connection.disconnect()
        return response
    }

    companion object {
        private const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhjZXE3ZXhkdWZkZ25tY3RzeGNnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg4MzgxMjgsImV4cCI6MjEwNDQxNDEyOH0.LqPTMoS3Q1-zdsOX9CMOahMynB5XAl-AxsjrVxSlse8"
    }
}
