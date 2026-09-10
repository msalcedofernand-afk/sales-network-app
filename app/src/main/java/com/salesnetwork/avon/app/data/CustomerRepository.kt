package com.salesnetwork.avon.app.data

import android.content.Context
import com.salesnetwork.avon.app.domain.model.CustomerContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CustomerRepository private constructor(context: Context) {
    private val remoteApi = SupabaseCustomerApi(context.applicationContext)

    private val _customers = MutableStateFlow<List<CustomerContact>>(emptyList())
    val customers: StateFlow<List<CustomerContact>> = _customers.asStateFlow()

    suspend fun refreshFromSupabase(): Result<Int> = runCatching {
        val remote = remoteApi.fetch()
        _customers.value = remote
        remote.size
    }

    suspend fun addRemote(customer: CustomerContact, userId: String): Result<Unit> = remoteApi.add(customer, userId).onSuccess { refreshFromSupabase() }

    suspend fun archiveRemote(id: String): Result<Unit> = remoteApi.archive(id).onSuccess { _customers.value = _customers.value.filterNot { it.id == id } }

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
