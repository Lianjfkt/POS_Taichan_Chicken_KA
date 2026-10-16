package com.kapos.mobile.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kapos.mobile.core.data.local.entity.CashMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashMovementDao {
    @Query("SELECT * FROM cash_movements WHERE shiftId = :shiftId ORDER BY createdAt DESC")
    fun observeByShift(shiftId: String): Flow<List<CashMovementEntity>>

    @Query("SELECT * FROM cash_movements WHERE shiftId = :shiftId ORDER BY createdAt DESC")
    suspend fun getByShift(shiftId: String): List<CashMovementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: CashMovementEntity)
}
