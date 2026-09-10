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

data class ChiclayoZone(
    val name: String,
    val addressHint: String,
    val lat: Double,
    val lng: Double
)

data class CustomerUiState(
    val customers: List<CustomerContact> = emptyList(),
    val filteredCustomers: List<CustomerContact> = emptyList(),
    val searchQuery: String = "",
    val isCalculatingEta: Boolean = false,
    val showAddDialog: Boolean = false,
    val statusMessage: String? = null,
    val chiclayoZones: List<ChiclayoZone> = listOf(
        ChiclayoZone("Centro / Balta", "Av. Jose Balta, Chiclayo", -6.7725, -79.8390),
        ChiclayoZone("Bolognesi / Santa Victoria", "Av. Bolognesi 450, Chiclayo", -6.7750, -79.8420),
        ChiclayoZone("Luis Gonzales / Mercado", "Av. Luis Gonzales 890, Chiclayo", -6.7680, -79.8375),
        ChiclayoZone("La Victoria / Grau", "Av. Miguel Grau 350, La Victoria", -6.7820, -79.8460),
        ChiclayoZone("Jose Leonardo Ortiz / Moshoqueque", "Av. Augusto B. Leguia, JLO", -6.7580, -79.8350),
        ChiclayoZone("Pimentel / Balneario", "Av. Quiñones, Pimentel", -6.8333, -79.9333)
    )
)

class CustomerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CustomerRepository.getInstance(application)
    private val routeEtaService = RouteEtaService()

    private val _uiState = MutableStateFlow(CustomerUiState())
    val uiState: StateFlow<CustomerUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""
    private var isRootAdmin: Boolean = false

    fun setUser(userId: String, isRoot: Boolean = false) {
        currentUserId = userId
        isRootAdmin = isRoot
        refreshFromSupabase()
    }

    fun setUserId(userId: String) {
        currentUserId = userId
        loadCustomers()
    }

    fun deleteCustomer(id: String) {
        viewModelScope.launch {
            repository.archiveRemote(id).onFailure { _uiState.value = _uiState.value.copy(statusMessage = "No pudimos archivar el cliente.") }
            loadCustomers()
        }
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

    fun addCustomer(
        name: String,
        phone: String,
        address: String,
        notes: String,
        latitude: Double? = -6.7714,
        longitude: Double? = -79.8409
    ) {
        if (name.isBlank() || phone.isBlank()) return
        viewModelScope.launch {
            var newCustomer = CustomerContact(
                id = "c-${UUID.randomUUID().toString().take(6)}",
                name = name.trim(),
                phone = phone.trim(),
                whatsapp = phone.trim(),
                address = address.trim().ifEmpty { "Chiclayo, Peru" },
                city = "Chiclayo",
                latitude = latitude,
                longitude = longitude,
                notes = notes.trim(),
                addedByUserId = currentUserId
            )
            // Calculate instant route ETA
            newCustomer = routeEtaService.enrichCustomerWithEta(newCustomer)
            val result = repository.addRemote(newCustomer, currentUserId)
            if (result.isFailure) { _uiState.value = _uiState.value.copy(statusMessage = "No pudimos guardar el cliente."); return@launch }
            closeAddDialog()
            loadCustomers()
        }
    }

    private fun refreshFromSupabase() {
        viewModelScope.launch {
            val result = repository.refreshFromSupabase()
            _uiState.value = _uiState.value.copy(statusMessage = result.fold({ "Clientes sincronizados." }, { "Sin conexión: mostrando la última copia." }))
            loadCustomers()
        }
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
        val list = if (isRootAdmin) repository.getAllCustomers() else repository.getCustomersForUser(currentUserId)
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
