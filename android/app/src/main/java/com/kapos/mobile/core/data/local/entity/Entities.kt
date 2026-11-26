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
    val isActive: Boolean,
    val allowSpiceLevel: Boolean = false,
    val allowSambal: Boolean = false,
    val allowComplimentary: Boolean = false,
    val allowToppings: Boolean = false
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
    val syncStatus: String, // SYNCED, PENDING_SYNC
    val voidReason: String? = null,
    val voidedAt: Long? = null
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

@Entity(
    tableName = "cash_movements",
    foreignKeys = [
        ForeignKey(
            entity = ShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("shiftId")]
)
data class CashMovementEntity(
    @PrimaryKey val id: String,
    val shiftId: String,
    val type: String, // CASH_IN, CASH_OUT
    val category: String,
    val amount: Double,
    val notes: String,
    val createdAt: Long
)

@Entity(
    tableName = "customers",
    indices = [Index("phone")]
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val points: Int = 0,
    val totalSpend: Double = 0.0,
    val orderCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "raw_materials",
    indices = [Index("category")]
)
data class RawMaterialEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // BUMBU_SAMBAL, PROTEIN, KARBO, MINUMAN, PACKAGING
    val unit: String,     // gram, kg, pcs, ml, botol, porsi
    val currentStock: Double,
    val minStock: Double,
    val costPerUnit: Double,
    val lastRestockedAt: Long = System.currentTimeMillis(),
    val earliestExpiryDate: Long? = null,
    val hasExpiredBatch: Boolean = false
)

@Entity(
    tableName = "recipe_items",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RawMaterialEntity::class,
            parentColumns = ["id"],
            childColumns = ["materialId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId"), Index("materialId")]
)
data class RecipeItemEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val materialId: String,
    val quantityRequired: Double
)

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = RawMaterialEntity::class,
            parentColumns = ["id"],
            childColumns = ["materialId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("materialId")]
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val materialId: String,
    val type: String, // RESTOCK, ORDER_DEDUCTION, WASTE, ADJUSTMENT
    val quantity: Double,
    val costTotal: Double = 0.0,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

