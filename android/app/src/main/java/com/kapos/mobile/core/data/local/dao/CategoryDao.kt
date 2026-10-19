package com.kapos.mobile.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kapos.mobile.core.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun observeAllIncludeInactive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllIncludeInactive(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE categories SET isActive = :isActive WHERE id = :id")
    suspend fun toggleActive(id: String, isActive: Boolean)

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}
