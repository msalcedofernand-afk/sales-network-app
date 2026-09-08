package com.salesnetwork.avon.app.data

import android.content.Context
import com.salesnetwork.avon.app.domain.model.Product
import com.salesnetwork.avon.app.scraper.CatalogScraperEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProductCatalogRepository private constructor(context: Context) {

    private val scraperEngine = CatalogScraperEngine()
    private val _products = MutableStateFlow<List<Product>>(scraperEngine.generateDefaultAvonCatalog())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isScraping = MutableStateFlow(false)
    val isScraping: StateFlow<Boolean> = _isScraping.asStateFlow()

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

    fun syncFromWebPage(htmlContent: String, sourceUrl: String) {
        _isScraping.value = true
        try {
            val extracted = scraperEngine.parseHtmlCatalog(htmlContent, sourceUrl)
            val currentList = _products.value.toMutableList()
            extracted.forEach { newItem ->
                val existingIndex = currentList.indexOfFirst { it.sku == newItem.sku }
                if (existingIndex >= 0) {
                    currentList[existingIndex] = newItem
                } else {
                    currentList.add(0, newItem)
                }
            }
            _products.value = currentList
        } finally {
            _isScraping.value = false
        }
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
