package com.salesnetwork.avon.app.data

import android.content.Context
import com.salesnetwork.avon.app.domain.model.Order
import com.salesnetwork.avon.app.domain.model.OrderItem
import com.salesnetwork.avon.app.domain.model.OrderStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class OrderRepository private constructor(context: Context) {

    private val _orders = MutableStateFlow<List<Order>>(generateInitialOrders())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    fun getOrdersForLeader(leaderUserId: String): List<Order> {
        return _orders.value.filter { it.leaderUserId == leaderUserId || it.leaderUserId.isEmpty() }
    }

    fun getOrdersForCustomer(customerId: String): List<Order> {
        return _orders.value.filter { it.customerId == customerId }
    }

    fun createOrder(
        customerId: String,
        customerName: String,
        leaderUserId: String,
        campaignCode: String,
        items: List<OrderItem>
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
            commissionMember = total * 0.20,
            status = OrderStatus.PENDIENTE
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

    fun calculateTotalCommission(leaderUserId: String): Double {
        return getOrdersForLeader(leaderUserId).sumOf { it.commissionLeader }
    }

    private fun generateInitialOrders(): List<Order> {
        return listOf(
            Order(
                id = "ord-1001",
                customerId = "c-001",
                customerName = "María Elena Flores",
                leaderUserId = "leader-demo-01",
                campaignCode = "C-01-2026",
                items = listOf(
                    OrderItem("PERF-01", "Far Away Royale EDP 50ml", 89.90, 1),
                    OrderItem("FACIAL-01", "Crema Facial Anew Ultimate 50g", 119.90, 1)
                ),
                totalAmount = 209.80,
                commissionLeader = 62.94,
                commissionMember = 41.96,
                status = OrderStatus.ENTREGADO,
                createdAt = "2026-09-05"
            ),
            Order(
                id = "ord-1002",
                customerId = "c-002",
                customerName = "Carmen Rosa Gutiérrez",
                leaderUserId = "leader-demo-01",
                campaignCode = "C-01-2026",
                items = listOf(
                    OrderItem("MAQ-01", "Labial Ultra Matte Avon Red", 34.90, 2)
                ),
                totalAmount = 69.80,
                commissionLeader = 20.94,
                commissionMember = 13.96,
                status = OrderStatus.PENDIENTE,
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
