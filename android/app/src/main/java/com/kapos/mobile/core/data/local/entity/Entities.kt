package com.kapos.mobile.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String,
    val color: String,
    val sortOrder: Int,
    val isActive: Boolean
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId")]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val categoryId: String?,
    val name: String,
    val price: Double,
    val costPrice: Double,
    val emoji: String,
    val spiceLevel: String,
    val description: String,
    val isActive: Boolean
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderNo: String,
    val customerName: String?,
    val customerPhone: String?,
    val orderType: String, // DINE_IN, TAKE_AWAY, DELIVERY
    val tableNo: String?,
    val subtotal: Double,
    val discount: Double,
    val total: Double,
    val paymentMethod: String, // CASH, QRIS, TRANSFER
    val paymentStatus: String, // PAID, UNPAID, VOID
    val cashierName: String,
    val notes: String?,
    val createdAt: Long,
    val paidAt: Long?,
    val syncStatus: String // SYNCED, PENDING_SYNC
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId"), Index("productId")]
)
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val productId: String,
    val productName: String,
    val price: Double,
    val quantity: Int,
    val spiceLevel: String,
    val notes: String,
    val subtotal: Double
)

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val id: String,
    val cashierName: String,
    val startCash: Double,
    val endCash: Double,
    val systemSales: Double,
    val isOpen: Boolean,
    val openedAt: Long,
    val closedAt: Long?,
    val syncStatus: String
)
