package com.salesnetwork.avon.app.data

import android.content.Context
import com.salesnetwork.avon.app.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class ProductCatalogRepository private constructor(context: Context) {

    private val remoteApi = SupabaseCatalogApi(context.applicationContext)
    private val cache = context.getSharedPreferences("catalog_cache", Context.MODE_PRIVATE)
    private val _products = MutableStateFlow(loadCache())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isScraping = MutableStateFlow(false)
    val isScraping: StateFlow<Boolean> = _isScraping.asStateFlow()

    suspend fun refreshFromSupabase(): Result<Int> = runCatching {
        _isScraping.value = true
        val products = remoteApi.fetchProducts()
        if (products.isNotEmpty()) {
            _products.value = products
            saveCache(products)
        }
        products.size
    }.also { _isScraping.value = false }

    fun searchProducts(query: String, category: String? = null): List<Product> {
        return _products.value.filter { product ->
            val matchesQuery = query.isBlank() || 
                product.name.contains(query, ignoreCase = true) ||
                product.sku.contains(query, ignoreCase = true)
            val matchesCategory = category.isNullOrBlank() || category == "Todos" ||
                product.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }

    private fun loadCache(): List<Product> {
        return runCatching {
            val rows = JSONArray(cache.getString("products", "[]"))
            buildList(rows.length()) {
                for (index in 0 until rows.length()) {
                    val row = rows.getJSONObject(index)
                    add(Product(row.getString("id"), row.optString("sku"), row.optString("name"),
                        row.optString("category", "Otros"), row.optDouble("price", 0.0),
                        row.optString("imageUrl"), row.optString("description"),
                        row.optString("sourceUrl")))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveCache(products: List<Product>) {
        val rows = JSONArray()
        products.forEach { product ->
            rows.put(JSONObject().apply {
                put("id", product.id); put("sku", product.sku); put("name", product.name)
                put("category", product.category); put("price", product.price)
                put("imageUrl", product.imageUrl); put("description", product.description)
                put("sourceUrl", product.sourceUrl)
            })
        }
        cache.edit().putString("products", rows.toString()).apply()
    }

    companion object {
        @Volatile
        private var INSTANCE: ProductCatalogRepository? = null

        fun getInstance(context: Context): ProductCatalogRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ProductCatalogRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
