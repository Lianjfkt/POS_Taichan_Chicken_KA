package com.kapos.mobile.feature.checkout

import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.Product
import com.kapos.mobile.domain.model.Promotion
import com.kapos.mobile.domain.model.PromotionType
import org.junit.Assert.assertEquals
import org.junit.Test

class PromotionDiscountTest {

    private val sampleProduct = Product(
        id = "p-1",
        categoryId = "cat-1",
        name = "Sate Taichan Paha",
        price = 35000.0
    )

    @Test
    fun testNominalPromotionAppliedWhenMinSpendMet() {
        val promo = Promotion(
            id = "p-5k",
            title = "Hemat 5K",
            description = "Min spend 30k",
            type = PromotionType.NOMINAL,
            value = 5000.0,
            minSpend = 30000.0,
            isActive = true
        )

        val state = CheckoutUiState(
            cartItems = listOf(CartItem(product = sampleProduct, quantity = 1)), // subtotal 35.000
            selectedPromotion = promo
        )

        assertEquals(35000.0, state.subtotal, 0.01)
        assertEquals(5000.0, state.promoDiscount, 0.01)
        assertEquals(5000.0, state.discount, 0.01)
        assertEquals(30000.0, state.total, 0.01)
    }

    @Test
    fun testPromotionNotAppliedWhenMinSpendNotMet() {
        val promo = Promotion(
            id = "p-10k",
            title = "Hemat 10K",
            description = "Min spend 50k",
            type = PromotionType.NOMINAL,
            value = 10000.0,
            minSpend = 50000.0,
            isActive = true
        )

        val state = CheckoutUiState(
            cartItems = listOf(CartItem(product = sampleProduct, quantity = 1)), // subtotal 35.000 (< 50.000)
            selectedPromotion = promo
        )

        assertEquals(35000.0, state.subtotal, 0.01)
        assertEquals(0.0, state.promoDiscount, 0.01)
        assertEquals(35000.0, state.total, 0.01)
    }

    @Test
    fun testPercentPromotionCalculation() {
        val promo = Promotion(
            id = "p-10pct",
            title = "Diskon 10%",
            description = "Diskon 10% min spend 30k",
            type = PromotionType.PERCENT,
            value = 10.0,
            minSpend = 30000.0,
            isActive = true
        )

        val state = CheckoutUiState(
            cartItems = listOf(CartItem(product = sampleProduct, quantity = 2)), // subtotal 70.000
            selectedPromotion = promo
        )

        assertEquals(70000.0, state.subtotal, 0.01)
        assertEquals(7000.0, state.promoDiscount, 0.01)
        assertEquals(63000.0, state.total, 0.01)
    }
}
