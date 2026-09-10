package com.salesnetwork.avon.app.data

import android.content.Context
import com.salesnetwork.avon.app.data.local.AppDatabase
import com.salesnetwork.avon.app.data.local.CachedProduct
import com.salesnetwork.avon.app.domain.model.Product
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductCatalogRepository private constructor(context: Context) {

    private val remoteApi = SupabaseCatalogApi(context.applicationContext)
    private val productDao = AppDatabase.getInstance(context).productDao()
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isScraping = MutableStateFlow(false)
    val isScraping: StateFlow<Boolean> = _isScraping.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            loadFromCache()
        }
    }

    private suspend fun loadFromCache() {
        val cached = productDao.getAll()
        if (cached.isNotEmpty()) {
            _products.value = cached.map { it.toDomain() }
        }
    }

    suspend fun refreshFromSupabase(): Result<Int> = runCatching {
        _isScraping.value = true
        val products = remoteApi.fetchProducts()
        if (products.isNotEmpty()) {
            _products.value = products
            saveToCache(products)
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

    private suspend fun saveToCache(products: List<Product>) {
        productDao.deleteAll()
        productDao.insertAll(products.map { it.toCached() })
    }

    private fun CachedProduct.toDomain() = Product(
        id = sku,
        sku = sku,
        name = name,
        category = category,
        price = price,
        imageUrl = imageUrl,
        description = description,
        sourceUrl = ""
    )

    private fun Product.toCached() = CachedProduct(
        sku = sku,
        name = name,
        brand = "",
        category = category,
        price = price,
        description = description,
        imageUrl = imageUrl
    )

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
