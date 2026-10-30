package com.kapos.mobile.feature.checkout

import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.Customer
import com.kapos.mobile.domain.model.DiscountType
import com.kapos.mobile.domain.model.OrderType
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.Promotion

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
    val isQuickAddCustomerDialogOpen: Boolean = false,
    val printerErrorMessage: String? = null,
    // Split Payment support
    val isSplitPayment: Boolean = false,
    val splitCashPortion: Double = 0.0,
    val splitCashGiven: Double = 0.0,
    val splitNonCashMethod: PaymentMethod = PaymentMethod.QRIS,
    // Marketing Promotion support
    val availablePromotions: List<Promotion> = emptyList(),
    val selectedPromotion: Promotion? = null,
    // SECURITY: PIN Otorisasi Owner untuk diskon manual besar (>10% dari subtotal)
    val discountPinInput: String = "",
    val discountPinError: String? = null,
    val showDiscountPinDialog: Boolean = false
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

    // SECURITY: Diskon manual > 10% dari subtotal memerlukan otorisasi PIN Owner
    val DISCOUNT_AUTH_THRESHOLD_PERCENT: Double = 10.0
    val requiresDiscountAuthorization: Boolean
        get() {
            if (subtotal <= 0) return false
            val discountPercent = (manualDiscount / subtotal) * 100.0
            return discountPercent > DISCOUNT_AUTH_THRESHOLD_PERCENT
        }

    val pointRate: Double = 500.0 // 10 Poin = Rp 5.000 (1 Poin = Rp 500)
    val minPointsForRedeem: Int = 10
    val pointRedeemStep: Int = 10

    val availablePoints: Int
        get() = selectedCustomer?.points ?: 0

    val maxPointsCanRedeem: Int
        get() {
            if (availablePoints < minPointsForRedeem) return 0
            val remainingBill = (subtotal - manualDiscount).coerceAtLeast(0.0)
            // Nilai 1 step (10 poin) = 10 * 500 = 5.000
            val stepValue = pointRedeemStep * pointRate
            val maxStepsForBill = (remainingBill / stepValue).toInt()
            val maxStepsAvailable = availablePoints / pointRedeemStep
            val effectiveSteps = maxStepsForBill.coerceAtMost(maxStepsAvailable).coerceAtLeast(0)
            return effectiveSteps * pointRedeemStep
        }

    val pointDiscount: Double
        get() {
            if (!isRedeemingPoints || selectedCustomer == null) return 0.0
            val validPoints = pointsToRedeem.coerceIn(0, maxPointsCanRedeem)
            val validSteps = validPoints / pointRedeemStep
            return (validSteps * pointRedeemStep * pointRate).coerceAtLeast(0.0)
        }

    val promoDiscount: Double
        get() {
            return selectedPromotion?.calculateDiscount(subtotal) ?: 0.0
        }

    val discount: Double
        get() = (manualDiscount + pointDiscount + promoDiscount).coerceAtMost(subtotal)

    val total: Double
        get() = (subtotal - discount).coerceAtLeast(0.0)

    val pointsEarned: Int
        get() {
            if (selectedCustomer == null) return 0
            // 1 Poin tiap kelipatan Rp 10.000 belanja akhir
            return (total / 10000.0).toInt().coerceAtLeast(0)
        }

    // Split Payment calculations
    val splitNonCashAmount: Double
        get() = (total - splitCashPortion).coerceAtLeast(0.0)

    val splitCashChange: Double
        get() = (splitCashGiven - splitCashPortion).coerceAtLeast(0.0)

    val changeDue: Double
        get() {
            if (isSplitPayment) {
                return splitCashChange
            }
            if (paymentMethod != PaymentMethod.CASH) return 0.0
            return (cashGiven - total).coerceAtLeast(0.0)
        }

    val isCashSufficient: Boolean
        get() {
            if (isSplitPayment) {
                return splitCashGiven >= splitCashPortion && (splitCashPortion + splitNonCashAmount >= total)
            }
            if (paymentMethod != PaymentMethod.CASH) return true
            return cashGiven >= total
        }
}
