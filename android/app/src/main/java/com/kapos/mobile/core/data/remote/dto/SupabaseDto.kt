package com.kapos.mobile.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseCategoryDto(
    val id: String,
    val name: String,
    val emoji: String? = "🍽️",
    val color: String? = "#0B5C73",
    @SerialName("sort_order") val sortOrder: Int? = 0,
    @SerialName("is_active") val isActive: Boolean? = true
)

@Serializable
data class SupabaseProductDto(
    val id: String,
    @SerialName("category_id") val categoryId: String? = null,
    val name: String,
    val price: Double,
    @SerialName("cost_price") val costPrice: Double? = 0.0,
    val emoji: String? = "🍢",
    @SerialName("spice_level") val spiceLevel: String? = "",
    val description: String? = "",
    @SerialName("is_active") val isActive: Boolean? = true
)

@Serializable
data class SupabaseOrderPayload(
    val id: String,
    @SerialName("order_no") val orderNo: String,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("customer_phone") val customerPhone: String? = null,
    @SerialName("order_type") val orderType: String,
    @SerialName("table_no") val tableNo: String? = null,
    val subtotal: Double,
    val discount: Double = 0.0,
    val total: Double,
    @SerialName("payment_method") val paymentMethod: String,
    val status: String = "completed",
    @SerialName("cashier_id") val cashierId: String? = "Kasir",
    val notes: String? = null,
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SupabaseOrderItemPayload(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    val price: Double,
    val quantity: Int,
    @SerialName("spice_level") val spiceLevel: String? = "",
    val notes: String? = "",
    val subtotal: Double
)
