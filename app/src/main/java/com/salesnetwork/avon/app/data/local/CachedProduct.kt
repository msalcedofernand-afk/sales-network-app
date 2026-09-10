package com.salesnetwork.avon.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_products")
data class CachedProduct(
    @PrimaryKey
    val sku: String,
    val name: String,
    val brand: String,
    val category: String,
    val price: Double,
    val description: String,
    val imageUrl: String,
    val cachedAt: Long = System.currentTimeMillis()
)
