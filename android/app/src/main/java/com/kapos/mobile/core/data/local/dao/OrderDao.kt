package com.kapos.mobile.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kapos.mobile.core.data.local.entity.OrderEntity
import com.kapos.mobile.core.data.local.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow

data class OrderWithItems(
    val order: OrderEntity,
    val items: List<OrderItemEntity>
)

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getItemsForOrder(orderId: String): List<OrderItemEntity>

    @Query("SELECT * FROM orders WHERE syncStatus = 'PENDING_SYNC' ORDER BY createdAt ASC")
    suspend fun getPendingOrders(): List<OrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("UPDATE orders SET syncStatus = :status WHERE id = :orderId")
    suspend fun updateSyncStatus(orderId: String, status: String)

    @Transaction
    suspend fun insertCompleteOrder(order: OrderEntity, items: List<OrderItemEntity>) {
        insertOrder(order)
        insertOrderItems(items)
    }

    @Query("SELECT COUNT(*) FROM orders WHERE syncStatus = 'PENDING_SYNC'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT SUM(total) FROM orders WHERE paymentStatus = 'PAID' AND createdAt >= :startTime")
    fun observeTotalSalesSince(startTime: Long): Flow<Double?>

    @Query("SELECT * FROM orders WHERE paymentStatus = 'PAID' AND createdAt >= :startTime")
    suspend fun getAllOrdersAfter(startTime: Long): List<OrderEntity>

    @Query("UPDATE orders SET paymentStatus = 'VOID', syncStatus = 'PENDING_SYNC' WHERE id = :orderId")
    suspend fun voidOrder(orderId: String)

    @Query("UPDATE orders SET paymentStatus = 'VOID', voidReason = :reason, voidedAt = :timestamp, syncStatus = 'PENDING_SYNC' WHERE id = :orderId")
    suspend fun voidOrderWithReason(orderId: String, reason: String, timestamp: Long)
}
