package com.salesnetwork.avon.app.data

import android.content.Context
import android.net.Uri
import com.salesnetwork.avon.app.domain.model.Order
import com.salesnetwork.avon.app.domain.model.OrderItem
import com.salesnetwork.avon.app.domain.model.OrderStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject

class SupabaseOrderApi(context: Context) {
    private val context = context.applicationContext
    private val secureTokenStore = SecureTokenStore(context)
    private val base = SupabaseConfig.BASE_URL
    private val token get() = secureTokenStore.get()

    suspend fun uploadPaymentProof(orderId: String, source: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val auth = token ?: error("Sesión vencida. Vuelve a iniciar sesión.")
            val compressed = com.salesnetwork.avon.app.utils.PhotoCompressor.compress(context, source).getOrThrow()
            try {
                require(compressed.bytes <= 2_500_000) { "La foto sigue siendo demasiado grande." }
                val userId = secureTokenStore.getSession()?.userId ?: error("Sesión no disponible")
                val objectPath = "$userId/$orderId-${java.util.UUID.randomUUID()}.jpg"
                val encodedPath = objectPath.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8") }
                val connection = (URL(base + "/storage/v1/object/order-proofs/$encodedPath").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; doOutput = true; connectTimeout = 8_000; readTimeout = 15_000
                    setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                    setRequestProperty("Authorization", "Bearer $auth")
                    setRequestProperty("Content-Type", compressed.mimeType)
                    setRequestProperty("x-upsert", "false")
                }
                compressed.file.inputStream().use { input -> connection.outputStream.use { output -> input.copyTo(output) } }
                if (connection.responseCode !in 200..299) error("No pudimos subir el comprobante.")
                connection.disconnect()
                objectPath
            } finally { compressed.file.delete() }
        }
    }

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

    suspend fun transitionStatus(orderId: String, status: OrderStatus, reason: String? = null, proofPath: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val auth = token ?: error("Sesión vencida. Vuelve a iniciar sesión.")
            val connection = (URL(base + "/rest/v1/rpc/transition_order_status_with_details").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                setRequestProperty("Authorization", "Bearer $auth")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }
            connection.outputStream.use { output ->
                output.write(JSONObject().put("input_order_id", orderId).put("next_status", status.name).put("reason", reason).put("proof_path", proofPath).toString().toByteArray())
            }
            val response = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299) error("No pudimos actualizar el pedido: $response")
            connection.disconnect()
        }
    }

    private fun request(path: String, auth: String): String {
        val connection = (URL(base + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 8_000; readTimeout = 8_000
            setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY); setRequestProperty("Authorization", "Bearer " + auth); setRequestProperty("Accept", "application/json")
        }
        val response = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) error("No pudimos cargar los pedidos")
        connection.disconnect()
        return response
    }

}
