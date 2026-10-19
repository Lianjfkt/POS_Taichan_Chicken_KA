package com.kapos.mobile.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kapos.mobile.core.data.local.entity.RawMaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RawMaterialDao {
    @Query("SELECT * FROM raw_materials ORDER BY name ASC")
    fun observeAll(): Flow<List<RawMaterialEntity>>

    @Query("SELECT * FROM raw_materials WHERE category = :category ORDER BY name ASC")
    fun observeByCategory(category: String): Flow<List<RawMaterialEntity>>

    @Query("SELECT * FROM raw_materials WHERE currentStock <= minStock ORDER BY name ASC")
    fun observeLowStock(): Flow<List<RawMaterialEntity>>

    @Query("SELECT * FROM raw_materials ORDER BY name ASC")
    suspend fun getAll(): List<RawMaterialEntity>

    @Query("SELECT * FROM raw_materials WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RawMaterialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(material: RawMaterialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(materials: List<RawMaterialEntity>)

    @Update
    suspend fun update(material: RawMaterialEntity)

    @Query("DELETE FROM raw_materials WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE raw_materials SET currentStock = MAX(0.0, currentStock + :delta), lastRestockedAt = :timestamp WHERE id = :id")
    suspend fun adjustStock(id: String, delta: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE raw_materials SET currentStock = :newStock, lastRestockedAt = :timestamp WHERE id = :id")
    suspend fun setStock(id: String, newStock: Double, timestamp: Long = System.currentTimeMillis())
}
