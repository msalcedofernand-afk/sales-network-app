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

data class Order(
    val id: String,
    val customerId: String,
    val customerName: String,
    val leaderUserId: String,
    val campaignCode: String = "C-01-2026",
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = items.sumOf { it.subtotal },
    val commissionLeader: Double = totalAmount * 0.30, // 30% comisión líder
    val commissionMember: Double = totalAmount * 0.20, // 20% comisión miembro
    val status: OrderStatus = OrderStatus.PENDIENTE,
    val createdAt: String = "2026-09-07"
)
