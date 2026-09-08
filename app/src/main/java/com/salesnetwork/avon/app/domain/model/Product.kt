package com.salesnetwork.avon.app.domain.model

data class Product(
    val id: String,
    val sku: String,
    val name: String,
    val category: String,
    val price: Double,
    val imageUrl: String,
    val description: String,
    val sourceUrl: String = ""
)
