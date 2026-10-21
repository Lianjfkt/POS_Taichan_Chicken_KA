package com.kapos.mobile.feature.checkout

import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitPaymentCalculationTest {

    private val sampleProduct = Product(
        id = "p-1",
        categoryId = "cat-1",
        name = "Sate Taichan Paha",
        price = 75000.0
    )

    @Test
    fun testExactSplitPayment() {
        val state = CheckoutUiState(
            cartItems = listOf(CartItem(product = sampleProduct, quantity = 1)),
            isSplitPayment = true,
            splitCashPortion = 50000.0,
            splitCashGiven = 50000.0,
            splitNonCashMethod = PaymentMethod.QRIS
        )

        assertEquals(75000.0, state.total, 0.01)
        assertEquals(25000.0, state.splitNonCashAmount, 0.01)
        assertEquals(0.0, state.splitCashChange, 0.01)
        assertEquals(0.0, state.changeDue, 0.01)
        assertTrue(state.isCashSufficient)
    }

    @Test
    fun testSplitPaymentWithCashOverpayment() {
        val state = CheckoutUiState(
            cartItems = listOf(CartItem(product = sampleProduct, quantity = 1)),
            isSplitPayment = true,
            splitCashPortion = 40000.0,
            splitCashGiven = 50000.0,
            splitNonCashMethod = PaymentMethod.TRANSFER
        )

        assertEquals(75000.0, state.total, 0.01)
        assertEquals(35000.0, state.splitNonCashAmount, 0.01)
        assertEquals(10000.0, state.splitCashChange, 0.01)
        assertEquals(10000.0, state.changeDue, 0.01)
        assertTrue(state.isCashSufficient)
    }

    @Test
    fun testSplitPaymentWithInsufficientCashGiven() {
        val state = CheckoutUiState(
            cartItems = listOf(CartItem(product = sampleProduct, quantity = 1)),
            isSplitPayment = true,
            splitCashPortion = 50000.0,
            splitCashGiven = 30000.0,
            splitNonCashMethod = PaymentMethod.QRIS
        )

        assertEquals(75000.0, state.total, 0.01)
        assertEquals(25000.0, state.splitNonCashAmount, 0.01)
        assertEquals(0.0, state.splitCashChange, 0.01)
        assertFalse(state.isCashSufficient)
    }
}
