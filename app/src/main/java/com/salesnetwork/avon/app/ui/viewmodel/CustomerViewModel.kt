package com.salesnetwork.avon.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salesnetwork.avon.app.data.CustomerRepository
import com.salesnetwork.avon.app.data.RouteEtaService
import com.salesnetwork.avon.app.domain.model.CustomerContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class CustomerUiState(
    val customers: List<CustomerContact> = emptyList(),
    val filteredCustomers: List<CustomerContact> = emptyList(),
    val searchQuery: String = "",
    val isCalculatingEta: Boolean = false,
    val showAddDialog: Boolean = false,
    val statusMessage: String? = null
)

class CustomerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CustomerRepository.getInstance(application)
    private val routeEtaService = RouteEtaService()

    private val _uiState = MutableStateFlow(CustomerUiState())
    val uiState: StateFlow<CustomerUiState> = _uiState.asStateFlow()

    private var currentUserId: String = "leader-demo-01"

    fun setUserId(userId: String) {
        currentUserId = userId
        loadCustomers()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun openAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun closeAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun addCustomer(name: String, phone: String, address: String, notes: String) {
        if (name.isBlank() || phone.isBlank()) return
        val newCustomer = CustomerContact(
            id = "c-${UUID.randomUUID().toString().take(6)}",
            name = name.trim(),
            phone = phone.trim(),
            whatsapp = phone.trim(),
            address = address.trim().ifEmpty { "Chiclayo, Perú" },
            city = "Chiclayo",
            latitude = -6.7714, // Coordenadas default de Chiclayo
            longitude = -79.8409,
            notes = notes.trim(),
            addedByUserId = currentUserId
        )
        repository.addCustomer(newCustomer, currentUserId)
        closeAddDialog()
        loadCustomers()
    }

    fun recalculateEtas() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCalculatingEta = true, statusMessage = "Calculando rutas por calles de Chiclayo...")
            val list = repository.getCustomersForUser(currentUserId)
            val enriched = list.map { routeEtaService.enrichCustomerWithEta(it) }
            _uiState.value = _uiState.value.copy(
                customers = enriched,
                isCalculatingEta = false,
                statusMessage = "Rutas actualizadas por red vial."
            )
            applyFilters()
        }
    }

    private fun loadCustomers() {
        val list = repository.getCustomersForUser(currentUserId)
        _uiState.value = _uiState.value.copy(customers = list)
        applyFilters()
    }

    private fun applyFilters() {
        val query = _uiState.value.searchQuery.trim().lowercase()
        var filtered = _uiState.value.customers
        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.lowercase().contains(query) ||
                it.address.lowercase().contains(query) ||
                it.phone.contains(query)
            }
        }
        _uiState.value = _uiState.value.copy(filteredCustomers = filtered)
    }
}
