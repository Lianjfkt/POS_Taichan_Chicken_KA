package com.kapos.mobile.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.kapos.mobile.core.data.local.entity.MaterialBatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialBatchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batch: MaterialBatchEntity)

    @Upsert
    suspend fun upsertAll(batches: List<MaterialBatchEntity>)

    @Query("SELECT * FROM material_batches WHERE materialId = :materialId ORDER BY receivedDate DESC")
    fun observeByMaterial(materialId: String): Flow<List<MaterialBatchEntity>>

    @Query("SELECT * FROM material_batches ORDER BY receivedDate DESC")
    fun observeAll(): Flow<List<MaterialBatchEntity>>

    @Query("SELECT * FROM material_batches WHERE materialId = :materialId AND remainingQuantity > 0 AND isUsable = 1 ORDER BY CASE WHEN expiryDate IS NULL THEN 1 ELSE 0 END, expiryDate ASC, receivedDate ASC")
    suspend fun getActiveBatchesForFIFO(materialId: String): List<MaterialBatchEntity>

    @Query("SELECT * FROM material_batches WHERE materialId = :materialId")
    suspend fun getByMaterial(materialId: String): List<MaterialBatchEntity>

    @Query("SELECT * FROM material_batches WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MaterialBatchEntity?

    @Query("UPDATE material_batches SET remainingQuantity = :newQuantity WHERE id = :batchId")
    suspend fun updateRemainingQuantity(batchId: String, newQuantity: Double)

    @Query("UPDATE material_batches SET isUsable = :isUsable WHERE id = :batchId")
    suspend fun setUsable(batchId: String, isUsable: Boolean)

    @Query("DELETE FROM material_batches WHERE id = :id")
    suspend fun deleteById(id: String)
}
