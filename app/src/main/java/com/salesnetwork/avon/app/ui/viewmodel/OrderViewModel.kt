package com.salesnetwork.avon.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salesnetwork.avon.app.data.OrderRepository
import com.salesnetwork.avon.app.domain.model.Order
import com.salesnetwork.avon.app.domain.model.OrderItem
import com.salesnetwork.avon.app.domain.model.OrderStatus
import com.salesnetwork.avon.app.domain.model.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrderUiState(
    val orders: List<Order> = emptyList(),
    val totalSales: Double = 0.0,
    val directCommission: Double = 0.0,
    val networkCommission: Double = 0.0,
    val totalProfit: Double = 0.0,
    val pendingDebt: Double = 0.0,
    val pendingCount: Int = 0,
    val activeCampaign: String = "C-01-2026",
    val campaigns: List<String> = listOf("C-01-2026", "C-02-2026", "C-03-2026"),
    val showCreateDialog: Boolean = false,
    val statusMessage: String? = null
)

class OrderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OrderRepository.getInstance(application)

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    private var currentLeaderId: String = "leader-demo-01"
    private var isRootAdmin: Boolean = false

    fun setUser(userId: String, isRoot: Boolean = false) {
        currentLeaderId = userId
        isRootAdmin = isRoot
        viewModelScope.launch {
            repository.refreshFromSupabase()
            loadOrders()
        }
    }

    fun setLeaderId(leaderId: String) {
        currentLeaderId = leaderId
        loadOrders()
    }

    fun deleteOrder(orderId: String) {
        repository.deleteOrder(orderId)
        loadOrders()
    }

    fun setCampaign(campaign: String) {
        _uiState.value = _uiState.value.copy(activeCampaign = campaign)
        loadOrders()
    }

    fun openCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun closeCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun createOrder(
        customerId: String,
        customerName: String,
        items: List<OrderItem>,
        paymentMethod: PaymentMethod = PaymentMethod.PENDIENTE,
        amountPaid: Double = 0.0
    ) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val result = repository.checkoutRemote(currentLeaderId, customerId, items)
            _uiState.value = _uiState.value.copy(
                showCreateDialog = false,
                statusMessage = result.fold({ "Pedido confirmado." }, { "No pudimos confirmar el pedido: " + (it.message ?: "revisa tu conexión") })
            )
            if (result.isSuccess) loadOrders()
        }
    }

    fun updateStatus(orderId: String, status: OrderStatus) {
        repository.updateOrderStatus(orderId, status)
        loadOrders()
    }

    fun registerPayment(orderId: String, method: PaymentMethod, amount: Double) {
        repository.registerPayment(orderId, method, amount)
        loadOrders()
    }

    fun buildWhatsAppTicket(order: Order): String {
        val sb = StringBuilder()
        sb.append("*VV CHICLAYO - COMPROBANTE DE PEDIDO*\n")
        sb.append("------------------------------------\n")
        sb.append("Cliente: ${order.customerName}\n")
        sb.append("Campana: ${order.campaignCode} | Fecha: ${order.createdAt}\n")
        sb.append("------------------------------------\n")
        sb.append("*DETALLE DE PRODUCTOS:*\n")
        order.items.forEach { item ->
            sb.append("- ${item.quantity}x ${item.productName}: S/ ${String.format("%.2f", item.subtotal)}\n")
        }
        sb.append("------------------------------------\n")
        sb.append("*TOTAL A PAGAR:* S/ ${String.format("%.2f", order.totalAmount)}\n")
        if (order.amountPaid > 0) {
            sb.append("Monto Abonado: S/ ${String.format("%.2f", order.amountPaid)} (${order.paymentMethod.name})\n")
        }
        if (order.remainingDebt > 0) {
            sb.append("*SALDO PENDIENTE:* S/ ${String.format("%.2f", order.remainingDebt)}\n")
            sb.append("\nPuedes cancelar tu saldo por Yape o Plin al numero registrado de tu lider VV.")
        } else {
            sb.append("Estado: CANCELADO CON EXITO\n")
        }
        sb.append("\nGracias por tu preferencia.")
        return sb.toString()
    }

    private fun loadOrders() {
        val list = if (isRootAdmin) {
            repository.getAllOrders(_uiState.value.activeCampaign)
        } else {
            repository.getOrdersForLeader(currentLeaderId, _uiState.value.activeCampaign)
        }
        val total = list.sumOf { it.totalAmount }
        val direct = list.sumOf { it.commissionLeader }
        val network = list.sumOf { it.networkCommissionLeader }
        val debt = list.sumOf { it.remainingDebt }
        val pending = list.count { it.status == OrderStatus.PENDIENTE }

        _uiState.value = _uiState.value.copy(
            orders = list,
            totalSales = total,
            directCommission = direct,
            networkCommission = network,
            totalProfit = direct + network,
            pendingDebt = debt,
            pendingCount = pending
        )
    }
}
