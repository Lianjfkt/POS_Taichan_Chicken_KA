package com.kapos.mobile.feature.checkout

import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.Customer
import com.kapos.mobile.domain.model.DiscountType
import com.kapos.mobile.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerLoyaltyPointsTest {

    private val testProduct1 = Product(
        id = "p1",
        categoryId = "cat1",
        name = "Sate Taichan Daging",
        price = 25000.0,
        costPrice = 14000.0
    )

    private val testProduct2 = Product(
        id = "p2",
        categoryId = "cat1",
        name = "Es Teh Manis",
        price = 5000.0,
        costPrice = 1500.0
    )

    @Test
    fun testCustomerLoyaltyPointsEarned() {
        val cartItems = listOf(
            CartItem(product = testProduct1, quantity = 3), // 75.000
            CartItem(product = testProduct2, quantity = 2)  // 10.000
        )
        // Subtotal = 85.000
        val state = CheckoutUiState(
            cartItems = cartItems,
            selectedCustomer = Customer(
                id = "c1",
                name = "Budi Santoso",
                phone = "081234567890",
                points = 50
            )
        )

        assertEquals(85000.0, state.subtotal, 0.001)
        assertEquals(85000.0, state.total, 0.001)
        // 85.000 / 10.000 = 8 Poin
        assertEquals(8, state.pointsEarned)
    }

    @Test
    fun testLoyaltyPointsRedemptionDiscount() {
        val cartItems = listOf(
            CartItem(product = testProduct1, quantity = 2) // 50.000
        )
        val customer = Customer(
            id = "c1",
            name = "Siti Aminah",
            phone = "081987654321",
            points = 65 // 65 poin -> 6 step (60 poin) = Rp 30.000
        )
        val state = CheckoutUiState(
            cartItems = cartItems,
            selectedCustomer = customer,
            isRedeemingPoints = true,
            pointsToRedeem = 60
        )

        assertEquals(50000.0, state.subtotal, 0.001)
        assertEquals(30000.0, state.pointDiscount, 0.001)
        assertEquals(20000.0, state.total, 0.001)
        // Points earned based on 20.000 total = 2 Poin
        assertEquals(2, state.pointsEarned)
    }

    @Test
    fun testMaxRedeemablePointsDoesNotExceedBill() {
        val cartItems = listOf(
            CartItem(product = testProduct2, quantity = 1) // 5.000
        )
        val customer = Customer(
            id = "c1",
            name = "Ahmad",
            phone = "085678901234",
            points = 50 // Rp 25.000 (5 steps of 10)
        )
        val state = CheckoutUiState(
            cartItems = cartItems,
            selectedCustomer = customer
        )

        // Bill is only 5.000, so max redeemable is 1 step of 10 points = 10 points (Rp 5.000)
        assertEquals(10, state.maxPointsCanRedeem)
    }

    @Test
    fun testCombinedManualAndLoyaltyDiscount() {
        val cartItems = listOf(
            CartItem(product = testProduct1, quantity = 4) // 100.000
        )
        val customer = Customer(
            id = "c1",
            name = "Dewi",
            phone = "087712345678",
            points = 50 // 50 pts = Rp 25.000
        )
        val state = CheckoutUiState(
            cartItems = cartItems,
            selectedCustomer = customer,
            discountType = DiscountType.PERCENT,
            discountInput = 10.0, // 10% = 10.000
            isRedeemingPoints = true,
            pointsToRedeem = 40 // 40 pts = 20.000
        )

        assertEquals(100000.0, state.subtotal, 0.001)
        assertEquals(10000.0, state.manualDiscount, 0.001)
        assertEquals(20000.0, state.pointDiscount, 0.001)
        assertEquals(30000.0, state.discount, 0.001)
        assertEquals(70000.0, state.total, 0.001)
        assertEquals(7, state.pointsEarned)
    }
}
