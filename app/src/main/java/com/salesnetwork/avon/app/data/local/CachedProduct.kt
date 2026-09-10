package com.salesnetwork.avon.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_products")
data class CachedProduct(
    @PrimaryKey
    val id: String,
    val teamId: String,
    val userId: String,
    val sku: String,
    val name: String,
    val brand: String,
    val category: String,
    val price: Double,
    val description: String,
    val imageUrl: String,
    val imageUrlsJson: String = "[]",
    val available: Boolean = true,
    val stockQuantity: Int? = null,
    val updatedAt: String,
    val syncedAt: Long = System.currentTimeMillis()
)
