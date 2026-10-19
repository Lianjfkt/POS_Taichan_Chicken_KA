package com.kapos.mobile.feature.checkout

import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.Customer
import com.kapos.mobile.domain.model.DiscountType
import com.kapos.mobile.domain.model.OrderType
import com.kapos.mobile.domain.model.PaymentMethod

data class CheckoutUiState(
    val orderNo: String = "",
    val orderType: OrderType = OrderType.DINE_IN,
    val tableNo: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val selectedCustomer: Customer? = null,
    val isRedeemingPoints: Boolean = false,
    val pointsToRedeem: Int = 0,
    val notes: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val cashGiven: Double = 0.0,
    val discountType: DiscountType = DiscountType.NOMINAL,
    val discountInput: Double = 0.0,
    val isSubmitting: Boolean = false,
    val isCompleted: Boolean = false,
    val cartItems: List<CartItem> = emptyList(),
    val availableCustomers: List<Customer> = emptyList(),
    val isSelectCustomerDialogOpen: Boolean = false,
    val isQuickAddCustomerDialogOpen: Boolean = false
) {
    val subtotal: Double
        get() = cartItems.sumOf { it.subtotal }

    val manualDiscount: Double
        get() {
            return if (discountType == DiscountType.PERCENT) {
                (subtotal * (discountInput / 100.0)).coerceAtLeast(0.0)
            } else {
                discountInput.coerceAtLeast(0.0)
            }
        }

    val pointRate: Double = 100.0 // 1 Poin = Rp 100

    val availablePoints: Int
        get() = selectedCustomer?.points ?: 0

    val maxPointsCanRedeem: Int
        get() {
            val remainingBill = (subtotal - manualDiscount).coerceAtLeast(0.0)
            val maxPointsForBill = (remainingBill / pointRate).toInt()
            return availablePoints.coerceAtMost(maxPointsForBill).coerceAtLeast(0)
        }

    val pointDiscount: Double
        get() {
            if (!isRedeemingPoints || selectedCustomer == null) return 0.0
            val validPoints = pointsToRedeem.coerceIn(0, maxPointsCanRedeem)
            return (validPoints * pointRate).coerceAtLeast(0.0)
        }

    val discount: Double
        get() = (manualDiscount + pointDiscount).coerceAtMost(subtotal)

    val total: Double
        get() = (subtotal - discount).coerceAtLeast(0.0)

    val pointsEarned: Int
        get() {
            if (selectedCustomer == null) return 0
            // 1 Poin tiap kelipatan Rp 10.000 belanja akhir
            return (total / 10000.0).toInt().coerceAtLeast(0)
        }

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
