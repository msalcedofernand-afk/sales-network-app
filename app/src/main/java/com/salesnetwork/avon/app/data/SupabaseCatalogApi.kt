package com.salesnetwork.avon.app.data

import android.content.Context
import com.salesnetwork.avon.app.domain.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class SupabaseCatalogApi(context: Context) {
    private val secureTokenStore = SecureTokenStore(context)

    suspend fun fetchProducts(): List<Product> = withContext(Dispatchers.IO) {
        val token = secureTokenStore.get() ?: return@withContext emptyList()
        val endpoint = URL(SupabaseConfig.BASE_URL + "/rest/v1/products?select=id,team_id,sku,name,category,price_cents,currency,image_url,description,source_url,available,stock_quantity,updated_at,product_images(storage_path,sort_order)&order=name")
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("apikey", SupabaseConfig.PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext emptyList()
            parseProducts(connection.inputStream.bufferedReader().use { it.readText() })
        } finally { connection.disconnect() }
    }

    private fun parseProducts(raw: String): List<Product> {
        val rows = JSONArray(raw)
        return buildList(rows.length()) {
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                val gallery = row.optJSONArray("product_images")
                val images = buildList {
                    if (gallery != null) {
                        val ordered = (0 until gallery.length())
                            .map { gallery.getJSONObject(it) }
                            .sortedBy { it.optInt("sort_order", 0) }
                        ordered.mapNotNullTo(this) { image ->
                            image.optString("storage_path").takeIf { it.isNotBlank() }
                        }
                    }
                    row.optString("image_url").takeIf { it.isNotBlank() && it !in this }?.let(::add)
                }
                add(Product(
                    id = row.getString("id"),
                    teamId = row.getString("team_id"),
                    sku = row.optString("sku"),
                    name = row.optString("name"),
                    category = row.optString("category", "Otros"),
                    price = row.optInt("price_cents", 0) / 100.0,
                    imageUrls = images,
                    description = row.optString("description"),
                    sourceUrl = row.optString("source_url"),
                    available = row.optBoolean("available", true),
                    stockQuantity = if (row.isNull("stock_quantity")) null else row.optInt("stock_quantity"),
                    updatedAt = row.optString("updated_at")
                ))
            }
        }
    }

}
