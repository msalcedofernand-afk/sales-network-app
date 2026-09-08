package com.salesnetwork.avon.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.salesnetwork.avon.app.data.OrderRepository
import com.salesnetwork.avon.app.domain.model.Order
import com.salesnetwork.avon.app.domain.model.OrderItem
import com.salesnetwork.avon.app.domain.model.OrderStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OrderUiState(
    val orders: List<Order> = emptyList(),
    val totalSales: Double = 0.0,
    val totalCommission: Double = 0.0,
    val pendingCount: Int = 0,
    val showCreateDialog: Boolean = false
)

class OrderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OrderRepository.getInstance(application)

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    private var currentLeaderId: String = "leader-demo-01"

    fun setLeaderId(leaderId: String) {
        currentLeaderId = leaderId
        loadOrders()
    }

    fun openCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun closeCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun createOrder(customerId: String, customerName: String, campaignCode: String, items: List<OrderItem>) {
        if (items.isEmpty()) return
        repository.createOrder(customerId, customerName, currentLeaderId, campaignCode, items)
        closeCreateDialog()
        loadOrders()
    }

    fun updateStatus(orderId: String, status: OrderStatus) {
        repository.updateOrderStatus(orderId, status)
        loadOrders()
    }

    fun buildWhatsAppSummary(order: Order): String {
        val sb = StringBuilder()
        sb.append("📋 *PEDIDO AVON CHICLAYO*\n")
        sb.append("👤 *Cliente*: ${order.customerName}\n")
        sb.append("🗓️ *Campaña*: ${order.campaignCode}\n\n")
        sb.append("*Detalle del Pedido*:\n")
        order.items.forEach { item ->
            sb.append("• ${item.quantity}x ${item.productName} - S/ ${String.format("%.2f", item.subtotal)}\n")
        }
        sb.append("\n💰 *TOTAL A PAGAR*: S/ ${String.format("%.2f", order.totalAmount)}\n")
        sb.append("✨ *Comisión Ganada*: S/ ${String.format("%.2f", order.commissionLeader)}\n")
        return sb.toString()
    }

    private fun loadOrders() {
        val list = repository.getOrdersForLeader(currentLeaderId)
        val total = list.sumOf { it.totalAmount }
        val comm = list.sumOf { it.commissionLeader }
        val pending = list.count { it.status == OrderStatus.PENDIENTE }
        _uiState.value = OrderUiState(
            orders = list,
            totalSales = total,
            totalCommission = comm,
            pendingCount = pending
        )
    }
}
