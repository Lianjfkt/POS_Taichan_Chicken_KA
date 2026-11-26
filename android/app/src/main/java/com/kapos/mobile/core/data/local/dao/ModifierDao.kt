package com.kapos.mobile.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kapos.mobile.core.data.local.entity.ModifierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModifierDao {
    @Query("SELECT * FROM modifiers ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<ModifierEntity>>

    @Query("SELECT * FROM modifiers WHERE isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun observeActive(): Flow<List<ModifierEntity>>

    @Query("SELECT * FROM modifiers WHERE category = :category ORDER BY sortOrder ASC, name ASC")
    fun observeByCategory(category: String): Flow<List<ModifierEntity>>

    @Query("SELECT * FROM modifiers WHERE isActive = 1 AND category = :category ORDER BY sortOrder ASC, name ASC")
    fun observeActiveByCategory(category: String): Flow<List<ModifierEntity>>

    @Query("SELECT * FROM modifiers ORDER BY sortOrder ASC, name ASC")
    suspend fun getAll(): List<ModifierEntity>

    @Query("SELECT * FROM modifiers WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ModifierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(modifier: ModifierEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(modifiers: List<ModifierEntity>)

    @Query("DELETE FROM modifiers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE modifiers SET isActive = :isActive WHERE id = :id")
    suspend fun toggleActive(id: String, isActive: Boolean)
}
