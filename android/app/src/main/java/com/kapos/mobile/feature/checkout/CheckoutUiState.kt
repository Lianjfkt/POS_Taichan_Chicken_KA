package com.kapos.mobile.feature.checkout

import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.OrderType
import com.kapos.mobile.domain.model.PaymentMethod

data class CheckoutUiState(
    val orderNo: String = "",
    val orderType: OrderType = OrderType.DINE_IN,
    val tableNo: String = "",
    val customerName: String = "",
    val notes: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val cashGiven: Double = 0.0,
    val discount: Double = 0.0,
    val isSubmitting: Boolean = false,
    val isCompleted: Boolean = false,
    val cartItems: List<CartItem> = emptyList()
) {
    val subtotal: Double
        get() = cartItems.sumOf { it.subtotal }

    val total: Double
        get() = (subtotal - discount).coerceAtLeast(0.0)

    val changeDue: Double
        get() {
            if (paymentMethod != PaymentMethod.CASH) return 0.0
            return (cashGiven - total).coerceAtLeast(0.0)
        }

    val isCashSufficient: Boolean
        get() {
            if (paymentMethod != PaymentMethod.CASH) return true
            return cashGiven >= total
        }
}
