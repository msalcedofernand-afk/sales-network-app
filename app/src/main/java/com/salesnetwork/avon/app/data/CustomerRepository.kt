package com.salesnetwork.avon.app.data

import android.content.Context
import com.salesnetwork.avon.app.domain.model.CustomerContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CustomerRepository private constructor(context: Context) {

    private val _customers = MutableStateFlow<List<CustomerContact>>(emptyList())
    val customers: StateFlow<List<CustomerContact>> = _customers.asStateFlow()

    fun getCustomersForUser(userId: String): List<CustomerContact> {
        return _customers.value.filter { it.addedByUserId.isEmpty() || it.addedByUserId == userId }
    }

    fun getAllCustomers(): List<CustomerContact> = _customers.value

    fun deleteCustomer(id: String) {
        _customers.value = _customers.value.filter { it.id != id }
    }

    fun addCustomer(customer: CustomerContact, userId: String) {
        val newCustomer = customer.copy(addedByUserId = userId)
        _customers.value = listOf(newCustomer) + _customers.value
    }

    private fun generateInitialCustomers(): List<CustomerContact> {
        return listOf(
            CustomerContact(
                id = "c-001",
                name = "MarÃ­a Elena Flores",
                phone = "+51979123456",
                whatsapp = "+51979123456",
                address = "Av. JosÃ© Balta 1240, Chiclayo",
                city = "Chiclayo",
                latitude = -6.7725,
                longitude = -79.8390,
                notes = "Cliente frecuente de PerfumerÃ­a Avon y Cuidado Facial Anew.",
                estimatedMinutes = 8,
                estimatedDistanceKm = 1.8,
                addedByUserId = "leader-demo-01"
            ),
            CustomerContact(
                id = "c-002",
                name = "Carmen Rosa GutiÃ©rrez",
                phone = "+51974567890",
                whatsapp = "+51974567890",
                address = "Av. Bolognesi 450, Chiclayo",
                city = "Chiclayo",
                latitude = -6.7750,
                longitude = -79.8420,
                notes = "SolicitÃ³ catÃ¡logo de CampaÃ±a 02. Entrega de pedidos por la tarde.",
                estimatedMinutes = 12,
                estimatedDistanceKm = 2.4,
                addedByUserId = "leader-demo-01"
            ),
            CustomerContact(
                id = "c-003",
                name = "LucÃ­a Mendoza RÃ­os",
                phone = "+51978901234",
                whatsapp = "+51978901234",
                address = "Av. Luis GonzÃ¡les 890, Chiclayo",
                city = "Chiclayo",
                latitude = -6.7680,
                longitude = -79.8375,
                notes = "Interesada en unirse a la red de ventas como vendedora de grupo.",
                estimatedMinutes = 15,
                estimatedDistanceKm = 3.6,
                addedByUserId = "leader-demo-01"
            ),
            CustomerContact(
                id = "c-004",
                name = "Rosa MarÃ­a Sandoval",
                phone = "+51971239876",
                whatsapp = "+51971239876",
                address = "Av. Miguel Grau 350, La Victoria, Chiclayo",
                city = "Chiclayo",
                latitude = -6.7820,
                longitude = -79.8460,
                notes = "Pedido de crema corporal Encanto SeducciÃ³n y labiales matte.",
                estimatedMinutes = 18,
                estimatedDistanceKm = 4.8,
                addedByUserId = "leader-demo-01"
            )
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: CustomerRepository? = null

        fun getInstance(context: Context): CustomerRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = CustomerRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
