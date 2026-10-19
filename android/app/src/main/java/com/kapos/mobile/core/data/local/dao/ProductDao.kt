package com.kapos.mobile.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kapos.mobile.core.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun observeAllIncludeInactive(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isActive = 1 AND categoryId = :categoryId ORDER BY name ASC")
    fun observeByCategory(categoryId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId ORDER BY name ASC")
    fun observeByCategoryIncludeInactive(categoryId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isActive = 1")
    suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllIncludeInactive(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getById(productId: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(products: List<ProductEntity>)

    @Update
    suspend fun update(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteById(productId: String)

    @Query("UPDATE products SET isActive = :isActive WHERE id = :productId")
    suspend fun toggleActive(productId: String, isActive: Boolean)

    @Query("DELETE FROM products")
    suspend fun clearAll()
}
