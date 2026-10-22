package com.kapos.mobile.domain.model

import java.io.Serializable

enum class OrderType(val displayName: String) {
    DINE_IN("Dine-In"),
    TAKE_AWAY("Take-Away"),
    DELIVERY("Delivery")
}

enum class PaymentMethod(val displayName: String) {
    CASH("Tunai"),
    QRIS("QRIS"),
    TRANSFER("Transfer")
}

enum class PaymentStatus(val displayName: String) {
    PAID("Lunas"),
    UNPAID("Belum Lunas"),
    VOID("Dibatalkan")
}

enum class SyncStatus {
    SYNCED,
    PENDING_SYNC
}

enum class DiscountType(val displayName: String) {
    NOMINAL("Rp"),
    PERCENT("%")
}

enum class CashMovementType(val displayName: String) {
    CASH_IN("Kas Masuk"),
    CASH_OUT("Kas Keluar")
}

data class CashMovement(
    val id: String,
    val shiftId: String,
    val type: CashMovementType,
    val category: String,
    val amount: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

data class Category(
    val id: String,
    val name: String,
    val emoji: String = "🍽️",
    val color: String = "#0B5C73",
    val sortOrder: Int = 0,
    val isActive: Boolean = true
) : Serializable

data class Customer(
    val id: String,
    val name: String,
    val phone: String,
    val points: Int = 0,
    val totalSpend: Double = 0.0,
    val orderCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

data class Product(
    val id: String,
    val categoryId: String?,
    val name: String,
    val price: Double,
    val costPrice: Double = 0.0,
    val emoji: String = "🍢",
    val spiceLevel: String = "",
    val description: String = "",
    val isActive: Boolean = true,
    val isSoldOut: Boolean = false
) : Serializable {
    fun calculateProfitMargin(): Double = (price - costPrice).coerceAtLeast(0.0)
    fun calculateMarginPercentage(): Double {
        if (price <= 0.0) return 0.0
        return ((price - costPrice) / price * 100.0).coerceAtLeast(0.0)
    }
}

data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val spiceLevel: String = "Sedang",
    val selectedModifiers: List<ModifierOption> = emptyList(),
    val notes: String = ""
) : Serializable {
    val unitPrice: Double
        get() = product.price + selectedModifiers.sumOf { it.price }

    val subtotal: Double
        get() = unitPrice * quantity
}

data class Order(
    val id: String,
    val orderNo: String,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val orderType: OrderType = OrderType.DINE_IN,
    val tableNo: String? = null,
    val subtotal: Double,
    val discount: Double = 0.0,
    val total: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val paymentStatus: PaymentStatus = PaymentStatus.PAID,
    val cashierName: String = "Kasir",
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val paidAt: Long? = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,
    val voidReason: String? = null,
    val voidedAt: Long? = null,
    val items: List<OrderItem> = emptyList()
) : Serializable

data class PaymentSplit(
    val cashTotal: Double = 0.0,
    val qrisTotal: Double = 0.0,
    val transferTotal: Double = 0.0,
    val totalRevenue: Double = 0.0
) {
    val nonCashTotal: Double get() = qrisTotal + transferTotal
}

fun Iterable<Order>.calculatePaymentSplit(): PaymentSplit {
    var cash = 0.0
    var qris = 0.0
    var transfer = 0.0
    for (order in this) {
        if (order.paymentStatus == PaymentStatus.PAID) {
            when (order.paymentMethod) {
                PaymentMethod.CASH -> cash += order.total
                PaymentMethod.QRIS -> qris += order.total
                PaymentMethod.TRANSFER -> transfer += order.total
            }
        }
    }
    return PaymentSplit(
        cashTotal = cash,
        qrisTotal = qris,
        transferTotal = transfer,
        totalRevenue = cash + qris + transfer
    )
}

data class OrderItem(
    val id: String,
    val orderId: String,
    val productId: String,
    val productName: String,
    val price: Double,
    val quantity: Int,
    val spiceLevel: String = "",
    val notes: String = "",
    val subtotal: Double
) : Serializable

data class Shift(
    val id: String,
    val cashierName: String,
    val startCash: Double,
    val endCash: Double = 0.0,
    val systemSales: Double = 0.0,
    val isOpen: Boolean = true,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_SYNC
) : Serializable

data class SavedOrder(
    val id: String = "HOLD-" + System.currentTimeMillis(),
    val customerName: String = "Umum",
    val tableNo: String = "",
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

enum class RawMaterialCategory(val displayName: String) {
    BUMBU_SAMBAL("Bumbu & Sambal"),
    PROTEIN("Daging & Protein"),
    KARBO("Beras & Karbo"),
    MINUMAN("Bahan Minuman"),
    PACKAGING("Kemasan & Alat")
}

data class RawMaterial(
    val id: String,
    val name: String,
    val category: RawMaterialCategory = RawMaterialCategory.BUMBU_SAMBAL,
    val unit: String = "gram", // gram, kg, pcs, ml, botol, porsi
    val currentStock: Double = 0.0,
    val minStock: Double = 100.0,
    val costPerUnit: Double = 0.0,
    val lastRestockedAt: Long = System.currentTimeMillis(),
    val earliestExpiryDate: Long? = null,
    val hasExpiredBatch: Boolean = false
) : Serializable {
    val isLowStock: Boolean
        get() = currentStock <= minStock
}

data class RecipeItem(
    val id: String,
    val productId: String,
    val materialId: String,
    val materialName: String = "",
    val unit: String = "gram",
    val quantityRequired: Double = 1.0
) : Serializable

enum class StockMovementType(val displayName: String) {
    RESTOCK("Stok Masuk / Pembelian"),
    ORDER_DEDUCTION("Potong Otomatis (BOM)"),
    WASTE("Stok Rusak / Terbuang"),
    ADJUSTMENT("Penyesuaian Opname"),
    VOID_RESTORATION("Pengembalian Void")
}

data class StockMovement(
    val id: String,
    val materialId: String,
    val materialName: String = "",
    val type: StockMovementType,
    val quantity: Double,
    val costTotal: Double = 0.0,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
