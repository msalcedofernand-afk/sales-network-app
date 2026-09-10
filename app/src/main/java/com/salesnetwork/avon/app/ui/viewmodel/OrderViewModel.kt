package com.salesnetwork.avon.app.ui.viewmodel

import android.app.Application
import android.net.Uri
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

    private var currentLeaderId: String = ""
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

    fun updateStatus(
        orderId: String,
        status: OrderStatus,
        reason: String? = null,
        proofUri: Uri? = null,
        paymentMethod: PaymentMethod? = null
    ) {
        val current = _uiState.value.orders.firstOrNull { it.id == orderId }
        if (current == null || !current.status.canTransitionTo(status)) {
            _uiState.value = _uiState.value.copy(statusMessage = "Ese cambio de estado no está permitido.")
            return
        }
        viewModelScope.launch {
            val proofPath = if (status == OrderStatus.COBRADO || (status == OrderStatus.ENTREGADO && proofUri != null)) {
                if (status == OrderStatus.COBRADO && paymentMethod != PaymentMethod.EFECTIVO && proofUri == null) {
                    _uiState.value = _uiState.value.copy(statusMessage = "Selecciona una foto del comprobante.")
                    return@launch
                }
                if (proofUri == null) null else {
                val upload = repository.uploadOrderProof(orderId, proofUri, if (status == OrderStatus.COBRADO) "payment" else "delivery")
                if (upload.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "No pudimos subir el comprobante: ${upload.exceptionOrNull()?.message ?: "revisa tu conexión"}"
                    )
                    return@launch
                }
                upload.getOrThrow()
                }
            } else null
            val result = repository.transitionStatusRemote(
                orderId = orderId,
                status = status,
                reason = reason,
                proofPath = proofPath,
                paymentMethod = if (status == OrderStatus.COBRADO) paymentMethod ?: PaymentMethod.YAPE else null,
                amountPaid = if (status == OrderStatus.COBRADO) current.totalAmount else null
            )
            if (result.isFailure && proofPath != null) repository.deleteOrderProof(proofPath)
            _uiState.value = _uiState.value.copy(
                statusMessage = result.fold({ "Estado actualizado." }, { it.message ?: "No pudimos actualizar el pedido." })
            )
            if (result.isSuccess) {
                repository.refreshFromSupabase()
                loadOrders()
            }
        }
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
            sb.append("Estado: PAGADO\n")
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
