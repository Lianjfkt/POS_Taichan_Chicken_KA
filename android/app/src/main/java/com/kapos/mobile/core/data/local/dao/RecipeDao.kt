package com.kapos.mobile.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kapos.mobile.core.data.local.entity.RecipeItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipe_items")
    fun observeAll(): Flow<List<RecipeItemEntity>>

    @Query("SELECT * FROM recipe_items WHERE productId = :productId")
    fun observeByProduct(productId: String): Flow<List<RecipeItemEntity>>

    @Query("SELECT * FROM recipe_items WHERE productId = :productId")
    suspend fun getByProduct(productId: String): List<RecipeItemEntity>

    @Query("SELECT * FROM recipe_items")
    suspend fun getAll(): List<RecipeItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: RecipeItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RecipeItemEntity>)

    @Query("DELETE FROM recipe_items WHERE productId = :productId")
    suspend fun deleteByProduct(productId: String)

    @Query("DELETE FROM recipe_items WHERE id = :id")
    suspend fun deleteById(id: String)
}
