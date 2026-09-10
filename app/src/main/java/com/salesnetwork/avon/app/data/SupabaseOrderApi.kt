package com.salesnetwork.avon.app.data

import android.content.Context
import android.net.Uri
import com.salesnetwork.avon.app.domain.model.Order
import com.salesnetwork.avon.app.domain.model.OrderItem
import com.salesnetwork.avon.app.domain.model.OrderStatus
import com.salesnetwork.avon.app.domain.model.PaymentMethod
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

    suspend fun uploadOrderProof(orderId: String, source: Uri, kind: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val auth = token ?: error("Sesión vencida. Vuelve a iniciar sesión.")
            val compressed = com.salesnetwork.avon.app.utils.PhotoCompressor.compress(context, source).getOrThrow()
            try {
                require(compressed.bytes <= 2_500_000) { "La foto sigue siendo demasiado grande." }
                val userId = secureTokenStore.getSession()?.userId ?: error("Sesión no disponible")
                require(kind in setOf("payment", "delivery")) { "Tipo de comprobante inválido" }
                val objectPath = "$userId/$orderId-$kind-${java.util.UUID.randomUUID()}.jpg"
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
        val orders = JSONArray(request("/rest/v1/orders?select=id,customer_id,user_id,status,total_cents,commission_cents,amount_paid_cents,payment_method,payment_proof_path,delivery_proof_path,cancellation_reason,return_reason,created_at,customers(name)&order=created_at.desc", auth))
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
                val paymentMethod = runCatching { PaymentMethod.valueOf(row.optString("payment_method")) }.getOrDefault(PaymentMethod.PENDIENTE)
                add(Order(
                    id = id,
                    customerId = row.optString("customer_id"),
                    customerName = row.optJSONObject("customers")?.optString("name").orEmpty().ifBlank { "Cliente archivado" },
                    leaderUserId = row.optString("user_id"),
                    totalAmount = row.optInt("total_cents") / 100.0,
                    commissionLeader = row.optInt("commission_cents") / 100.0,
                    items = lines,
                    status = status,
                    paymentMethod = paymentMethod,
                    amountPaid = if (row.isNull("amount_paid_cents")) 0.0 else row.optInt("amount_paid_cents") / 100.0,
                    paymentProofPath = row.optString("payment_proof_path"),
                    deliveryProofPath = row.optString("delivery_proof_path"),
                    cancellationReason = row.optString("cancellation_reason"),
                    returnReason = row.optString("return_reason"),
                    createdAt = row.optString("created_at").take(10)
                ))
            }
        }
    }

    suspend fun deleteOrderProof(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val auth = token ?: error("Sesión vencida. Vuelve a iniciar sesión.")
            val encodedPath = path.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8") }
            val connection = (URL(base + "/storage/v1/object/order-proofs/$encodedPath").openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                setRequestProperty("Authorization", "Bearer $auth")
            }
            if (connection.responseCode !in 200..299 && connection.responseCode != 404) {
                error("No pudimos limpiar el comprobante temporal.")
            }
            connection.disconnect()
        }
    }

    suspend fun transitionStatus(
        orderId: String,
        status: OrderStatus,
        reason: String? = null,
        proofPath: String? = null,
        paymentMethod: PaymentMethod? = null,
        amountPaid: Double? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val auth = token ?: error("Sesión vencida. Vuelve a iniciar sesión.")
            val connection = (URL(base + "/rest/v1/rpc/transition_order_status_v2").openConnection() as HttpURLConnection).apply {
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
                output.write(JSONObject().apply {
                    put("input_order_id", orderId)
                    put("next_status", status.name)
                    put("reason", reason)
                    put("payment_method", paymentMethod?.name)
                    put("input_amount_paid_cents", amountPaid?.let { kotlin.math.round(it * 100).toInt() })
                    put("proof_path", if (status == OrderStatus.COBRADO) proofPath else null)
                    put("input_delivery_proof_path", if (status == OrderStatus.ENTREGADO) proofPath else null)
                    put("event_source", "ANDROID")
                }.toString().toByteArray())
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
