package com.kapos.mobile.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kapos.mobile.core.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts WHERE isOpen = 1 ORDER BY openedAt DESC LIMIT 1")
    fun observeActiveShift(): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE isOpen = 1 ORDER BY openedAt DESC LIMIT 1")
    suspend fun getActiveShift(): ShiftEntity?

    @Query("SELECT * FROM shifts ORDER BY openedAt DESC")
    fun observeAllShifts(): Flow<List<ShiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity)

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE syncStatus = 'PENDING_SYNC' ORDER BY openedAt ASC")
    suspend fun getPendingShifts(): List<ShiftEntity>

    @Query("UPDATE shifts SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
