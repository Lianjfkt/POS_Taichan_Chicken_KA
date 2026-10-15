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

data class Category(
    val id: String,
    val name: String,
    val emoji: String = "🍽️",
    val color: String = "#0B5C73",
    val sortOrder: Int = 0,
    val isActive: Boolean = true
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
    val isActive: Boolean = true
) : Serializable

data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val spiceLevel: String = "Sedang",
    val notes: String = ""
) : Serializable {
    val subtotal: Double
        get() = product.price * quantity
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
    val items: List<OrderItem> = emptyList()
) : Serializable

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
