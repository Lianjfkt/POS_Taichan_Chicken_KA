package com.kapos.mobile.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kapos.mobile.core.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE phone = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): CustomerEntity?

    @Query("SELECT * FROM customers")
    suspend fun getAll(): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(customers: List<CustomerEntity>)

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("""
        UPDATE customers 
        SET points = MAX(0, points + :pointsDelta),
            totalSpend = totalSpend + :spendDelta,
            orderCount = orderCount + :orderDelta
        WHERE id = :id
    """)
    suspend fun updateStats(id: String, pointsDelta: Int, spendDelta: Double, orderDelta: Int = 1)
}
