package com.salesnetwork.avon.app.domain.model

data class OrderItem(
    val productSku: String,
    val productName: String,
    val unitPrice: Double,
    val quantity: Int
) {
    val subtotal: Double get() = unitPrice * quantity
}

enum class OrderStatus {
    PENDIENTE,
    CONFIRMADO,
    ENTREGADO,
    COBRADO,
    CANCELADO,
    DEVUELTO;

    fun allowedNext(): List<OrderStatus> = when (this) {
        PENDIENTE -> listOf(CONFIRMADO, CANCELADO)
        CONFIRMADO -> listOf(COBRADO, CANCELADO)
        COBRADO -> listOf(ENTREGADO, CANCELADO)
        ENTREGADO -> listOf(DEVUELTO)
        CANCELADO, DEVUELTO -> emptyList()
    }

    fun canTransitionTo(next: OrderStatus) = next in allowedNext()
}

enum class PaymentMethod {
    YAPE,
    PLIN,
    EFECTIVO,
    TRANSFERENCIA,
    OTRO,
    PENDIENTE
}

data class Order(
    val id: String,
    val customerId: String,
    val customerName: String,
    val leaderUserId: String,
    val campaignCode: String = "C-01-2026",
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = items.sumOf { it.subtotal },
    /** Server-calculated amounts. Zero means the order has not been hydrated from Supabase yet. */
    val commissionLeader: Double = 0.0,
    val networkCommissionLeader: Double = 0.0,
    val commissionMember: Double = 0.0,
    val status: OrderStatus = OrderStatus.PENDIENTE,
    val paymentMethod: PaymentMethod = PaymentMethod.PENDIENTE,
    val amountPaid: Double = 0.0,
    val paymentProofPath: String = "",
    val deliveryProofPath: String = "",
    val cancellationReason: String = "",
    val returnReason: String = "",
    val createdAt: String = "2026-09-08"
) {
    val remainingDebt: Double get() = (totalAmount - amountPaid).coerceAtLeast(0.0)
    val isFullyPaid: Boolean get() = remainingDebt <= 0.0
}
