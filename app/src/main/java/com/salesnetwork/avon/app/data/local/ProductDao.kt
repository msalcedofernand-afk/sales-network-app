package com.salesnetwork.avon.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductDao {
    @Query("SELECT * FROM cached_products WHERE userId = :userId ORDER BY name ASC")
    suspend fun getAll(userId: String): List<CachedProduct>

    @Query("SELECT * FROM cached_products WHERE category = :category ORDER BY name ASC")
    suspend fun getByCategory(category: String): List<CachedProduct>

    @Query("SELECT * FROM cached_products WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun search(query: String): List<CachedProduct>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<CachedProduct>)

    @Query("DELETE FROM cached_products WHERE userId = :userId")
    suspend fun deleteAll(userId: String)

    @Query("SELECT COUNT(*) FROM cached_products")
    suspend fun count(): Int

    @androidx.room.Transaction
    suspend fun replaceForUser(userId: String, products: List<CachedProduct>) {
        deleteAll(userId)
        insertAll(products)
    }
}
