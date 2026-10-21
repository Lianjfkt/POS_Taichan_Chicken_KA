package com.kapos.mobile.core.data.repository

import com.kapos.mobile.domain.model.OrderItem
import org.junit.Assert.assertEquals
import org.junit.Test

class VoidOrderReversalTest {

    @Test
    fun testReversalPointsCalculation() {
        val pointsEarned = 5
        val pointsRedeemed = 20
        // When reversing: redeemed points are restored (+pointsRedeemed), earned points are revoked (-pointsEarned)
        val pointsDelta = pointsRedeemed - pointsEarned
        assertEquals(15, pointsDelta)
    }

    @Test
    fun testOrderItemMappingForRestoration() {
        val item = OrderItem(
            id = "item-1",
            orderId = "order-1",
            productId = "prod-taichan",
            productName = "Taichan Original",
            price = 18000.0,
            quantity = 2,
            subtotal = 36000.0
        )
        val recipeQtyRequired = 10.0 // 10 skewers per portion
        val expectedRestoredStock = recipeQtyRequired * item.quantity
        assertEquals(20.0, expectedRestoredStock, 0.001)
    }
}
