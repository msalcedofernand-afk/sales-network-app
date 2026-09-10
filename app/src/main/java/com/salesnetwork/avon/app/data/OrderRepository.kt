package com.salesnetwork.avon.app.data

import android.content.Context
import android.net.Uri
import com.salesnetwork.avon.app.domain.model.Order
import com.salesnetwork.avon.app.domain.model.OrderItem
import com.salesnetwork.avon.app.domain.model.OrderStatus
import com.salesnetwork.avon.app.domain.model.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class OrderRepository private constructor(context: Context) {
    private val remoteApi = SupabaseOrderApi(context.applicationContext)
    private val checkoutApi = SupabaseCheckoutApi(context.applicationContext)

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    suspend fun refreshFromSupabase(): Result<Int> = runCatching {
        val remote = remoteApi.fetch()
        _orders.value = remote
        remote.size
    }

    suspend fun checkoutRemote(userId: String, customerId: String, items: List<OrderItem>): Result<Unit> =
        checkoutApi.checkout(userId, customerId, items)

    suspend fun transitionStatusRemote(
        orderId: String,
        status: OrderStatus,
        reason: String? = null,
        proofPath: String? = null,
        paymentMethod: PaymentMethod? = null,
        amountPaid: Double? = null
    ): Result<Unit> = remoteApi.transitionStatus(orderId, status, reason, proofPath, paymentMethod, amountPaid)

    suspend fun uploadOrderProof(orderId: String, source: Uri, kind: String): Result<String> =
        remoteApi.uploadOrderProof(orderId, source, kind)

    suspend fun deleteOrderProof(path: String): Result<Unit> = remoteApi.deleteOrderProof(path)

    fun getOrdersForLeader(leaderUserId: String, campaignCode: String? = null): List<Order> {
        val list = _orders.value.filter { it.leaderUserId == leaderUserId || it.leaderUserId.isEmpty() }
        return if (campaignCode.isNullOrBlank()) list else list.filter { it.campaignCode.equals(campaignCode, ignoreCase = true) }
    }

    fun getAllOrders(campaignCode: String? = null): List<Order> {
        val list = _orders.value
        return if (campaignCode.isNullOrBlank()) list else list.filter { it.campaignCode.equals(campaignCode, ignoreCase = true) }
    }

    fun getOrdersForCustomer(customerId: String): List<Order> {
        return _orders.value.filter { it.customerId == customerId }
    }

    fun createOrder(
        customerId: String,
        customerName: String,
        leaderUserId: String,
        campaignCode: String,
        items: List<OrderItem>,
        paymentMethod: PaymentMethod = PaymentMethod.PENDIENTE,
        amountPaid: Double = 0.0
    ): Order {
        val total = items.sumOf { it.subtotal }
        val order = Order(
            id = "ord-${UUID.randomUUID().toString().take(6)}",
            customerId = customerId,
            customerName = customerName,
            leaderUserId = leaderUserId,
            campaignCode = campaignCode.ifBlank { "C-01-2026" },
            items = items,
            totalAmount = total,
            commissionLeader = 0.0,
            networkCommissionLeader = 0.0,
            commissionMember = 0.0,
            status = if (amountPaid >= total && total > 0) OrderStatus.COBRADO else OrderStatus.PENDIENTE,
            paymentMethod = paymentMethod,
            amountPaid = amountPaid,
            createdAt = "2026-09-08"
        )
        _orders.value = listOf(order) + _orders.value
        return order
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        _orders.value = _orders.value.map { order ->
            if (order.id == orderId && order.status.canTransitionTo(newStatus)) {
                order.copy(status = newStatus)
            } else {
                order
            }
        }
    }

    fun calculateTotalDirectCommission(leaderUserId: String, campaignCode: String? = null): Double {
        return getOrdersForLeader(leaderUserId, campaignCode).sumOf { it.commissionLeader }
    }

    fun calculateTotalNetworkCommission(leaderUserId: String, campaignCode: String? = null): Double {
        return getOrdersForLeader(leaderUserId, campaignCode).sumOf { it.networkCommissionLeader }
    }
    companion object {
        @Volatile
        private var INSTANCE: OrderRepository? = null

        fun getInstance(context: Context): OrderRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = OrderRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
