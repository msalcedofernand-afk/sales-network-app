package com.salesnetwork.avon.app.domain.model

data class Product(
    val id: String,
    val sku: String,
    val name: String,
    val category: String,
    val price: Double,
    val imageUrls: List<String> = emptyList(),
    val description: String,
    val sourceUrl: String = "",
    val usageMode: String = "Aplicar sobre la piel limpia con suaves masajes circulares.",
    val available: Boolean = true,
    val teamId: String = ""
) {
    val primaryImageUrl: String get() = imageUrls.firstOrNull().orEmpty()
}
