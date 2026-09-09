package com.salesnetwork.avon.app.data

import android.content.Context
import com.salesnetwork.avon.app.domain.model.Order
import com.salesnetwork.avon.app.domain.model.OrderItem
import com.salesnetwork.avon.app.domain.model.OrderStatus
import com.salesnetwork.avon.app.domain.model.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class OrderRepository private constructor(context: Context) {

    private val _orders = MutableStateFlow<List<Order>>(generateInitialOrders())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    fun getOrdersForLeader(leaderUserId: String, campaignCode: String? = null): List<Order> {
        val list = _orders.value.filter { it.leaderUserId == leaderUserId || it.leaderUserId.isEmpty() }
        return if (campaignCode.isNullOrBlank()) list else list.filter { it.campaignCode.equals(campaignCode, ignoreCase = true) }
    }

    fun getAllOrders(campaignCode: String? = null): List<Order> {
        val list = _orders.value
        return if (campaignCode.isNullOrBlank()) list else list.filter { it.campaignCode.equals(campaignCode, ignoreCase = true) }
    }

    fun deleteOrder(orderId: String) {
        _orders.value = _orders.value.filter { it.id != orderId }
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
            commissionLeader = total * 0.30,
            networkCommissionLeader = total * 0.05,
            commissionMember = total * 0.20,
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
            if (order.id == orderId) {
                order.copy(status = newStatus)
            } else {
                order
            }
        }
    }

    fun registerPayment(orderId: String, method: PaymentMethod, amount: Double) {
        _orders.value = _orders.value.map { order ->
            if (order.id == orderId) {
                val newPaid = (order.amountPaid + amount).coerceAtMost(order.totalAmount)
                val newStatus = if (newPaid >= order.totalAmount) OrderStatus.COBRADO else order.status
                order.copy(paymentMethod = method, amountPaid = newPaid, status = newStatus)
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

    private fun generateInitialOrders(): List<Order> {
        return listOf(
            Order(
                id = "ord-1001",
                customerId = "c-001",
                customerName = "Maria Elena Flores",
                leaderUserId = "leader-demo-01",
                campaignCode = "C-01-2026",
                items = listOf(
                    OrderItem("PERF-01", "Far Away Royale EDP 50ml", 89.90, 1),
                    OrderItem("FACIAL-01", "Crema Facial Anew Ultimate 50g", 119.90, 1)
                ),
                totalAmount = 209.80,
                commissionLeader = 62.94,
                networkCommissionLeader = 10.49,
                commissionMember = 41.96,
                status = OrderStatus.COBRADO,
                paymentMethod = PaymentMethod.YAPE,
                amountPaid = 209.80,
                createdAt = "2026-09-05"
            ),
            Order(
                id = "ord-1002",
                customerId = "c-002",
                customerName = "Carmen Rosa Gutierrez",
                leaderUserId = "leader-demo-01",
                campaignCode = "C-01-2026",
                items = listOf(
                    OrderItem("MAQ-01", "Labial Ultra Matte VV Red", 34.90, 2)
                ),
                totalAmount = 69.80,
                commissionLeader = 20.94,
                networkCommissionLeader = 3.49,
                commissionMember = 13.96,
                status = OrderStatus.PENDIENTE,
                paymentMethod = PaymentMethod.PENDIENTE,
                amountPaid = 0.0,
                createdAt = "2026-09-06"
            )
        )
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
