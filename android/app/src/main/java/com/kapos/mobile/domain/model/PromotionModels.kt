package com.kapos.mobile.domain.model

import java.io.Serializable

enum class PromotionType(val displayName: String) {
    PERCENT("Diskon Persen"),
    NOMINAL("Potongan Nominal")
}

data class Promotion(
    val id: String,
    val title: String,
    val description: String,
    val type: PromotionType,
    val value: Double,
    val minSpend: Double = 0.0,
    val promoCode: String = "",
    val isActive: Boolean = true
) : Serializable {
    fun calculateDiscount(subtotal: Double): Double {
        if (!isActive || subtotal < minSpend) return 0.0
        return when (type) {
            PromotionType.PERCENT -> (subtotal * (value / 100.0)).coerceAtLeast(0.0)
            PromotionType.NOMINAL -> value.coerceAtLeast(0.0)
        }
    }
}
