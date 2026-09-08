package com.salesnetwork.avon.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salesnetwork.avon.app.data.ProductCatalogRepository
import com.salesnetwork.avon.app.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogUiState(
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "Todos",
    val searchQuery: String = "",
    val isScraping: Boolean = false,
    val statusMessage: String? = null
)

class CatalogViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProductCatalogRepository.getInstance(application)

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.products.collect { productList ->
                val categories = listOf("Todos") + productList.map { it.category }.distinct()
                _uiState.value = _uiState.value.copy(
                    products = productList,
                    categories = categories
                )
                applyFilters()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun onCategorySelected(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilters()
    }

    fun scrapeOfficialWebCatalog() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScraping = true, statusMessage = "Extrayendo productos de la web...")
            val before = repository.products.value.size
            repository.syncFromWebPage("", "https://www.avon.com.pe")
            val newCount = (repository.products.value.size - before).coerceAtLeast(0)
            _uiState.value = _uiState.value.copy(
                isScraping = false,
                statusMessage = "Sincronizados $newCount productos del catálogo oficial."
            )
        }
    }

    private fun applyFilters() {
        val query = _uiState.value.searchQuery.trim().lowercase()
        val cat = _uiState.value.selectedCategory

        var filtered = _uiState.value.products
        if (cat != "Todos") {
            filtered = filtered.filter { it.category.equals(cat, ignoreCase = true) }
        }
        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.lowercase().contains(query) ||
                it.sku.lowercase().contains(query) ||
                it.category.lowercase().contains(query)
            }
        }
        _uiState.value = _uiState.value.copy(filteredProducts = filtered)
    }
}
