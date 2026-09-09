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
    ENTREGADO,
    COBRADO,
    CANCELADO
}

enum class PaymentMethod {
    YAPE,
    PLIN,
    EFECTIVO,
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
    val commissionLeader: Double = totalAmount * 0.30, // 30% comisión directa
    val networkCommissionLeader: Double = totalAmount * 0.05, // 5% sobrecomisión de red
    val commissionMember: Double = totalAmount * 0.20, // 20% comisión miembro
    val status: OrderStatus = OrderStatus.PENDIENTE,
    val paymentMethod: PaymentMethod = PaymentMethod.PENDIENTE,
    val amountPaid: Double = 0.0,
    val createdAt: String = "2026-09-08"
) {
    val remainingDebt: Double get() = (totalAmount - amountPaid).coerceAtLeast(0.0)
    val isFullyPaid: Boolean get() = remainingDebt <= 0.0
}
