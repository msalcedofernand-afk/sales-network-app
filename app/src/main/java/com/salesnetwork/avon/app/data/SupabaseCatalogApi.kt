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
        val endpoint = URL("https://xceqwexdufdgnmctsxcg.supabase.co/rest/v1/products?select=id,sku,name,category,price_cents,currency,image_url,description,source_url,available&order=name")
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("apikey", ANON_KEY)
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
                if (!row.optBoolean("available", true)) continue
                add(Product(
                    id = row.getString("id"),
                    sku = row.optString("sku"),
                    name = row.optString("name"),
                    category = row.optString("category", "Otros"),
                    price = row.optInt("price_cents", 0) / 100.0,
                    imageUrl = row.optString("image_url"),
                    description = row.optString("description"),
                    sourceUrl = row.optString("source_url")
                ))
            }
        }
    }

    companion object {
        private const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhjZXE3ZXhkdWZkZ25tY3RzeGNnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg4MzgxMjgsImV4cCI6MjEwNDQxNDEyOH0.LqPTMoS3Q1-zdsOX9CMOahMynB5XAl-AxsjrVxSlse8"
    }
}
