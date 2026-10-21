package com.kapos.mobile.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MaterialBatchDomainTest {

    @Test
    fun testBatchFreshnessEvaluation() {
        val now = 1000000000L
        val oneDayMs = 24L * 60 * 60 * 1000

        // Expired batch
        val expiredBatch = MaterialBatch(
            id = "b1",
            materialId = "m1",
            batchNumber = "B-001",
            initialQuantity = 1000.0,
            remainingQuantity = 500.0,
            costTotal = 45000.0,
            costPerUnit = 45.0,
            receivedDate = now - (10 * oneDayMs),
            expiryDate = now - oneDayMs,
            isUsable = true
        )
        assertEquals(BatchFreshness.EXPIRED, expiredBatch.freshnessStatus(now))

        // Expiring soon batch (within 2 days)
        val soonBatch = expiredBatch.copy(
            id = "b2",
            expiryDate = now + (oneDayMs)
        )
        assertEquals(BatchFreshness.EXPIRING_SOON, soonBatch.freshnessStatus(now))

        // Fresh batch (> 2 days)
        val freshBatch = expiredBatch.copy(
            id = "b3",
            expiryDate = now + (5 * oneDayMs)
        )
        assertEquals(BatchFreshness.FRESH, freshBatch.freshnessStatus(now))

        // Manually damaged/unusable batch
        val damagedBatch = freshBatch.copy(
            id = "b4",
            isUsable = false
        )
        assertEquals(BatchFreshness.DAMAGED, damagedBatch.freshnessStatus(now))
    }

    @Test
    fun testProductMarginCalculation() {
        val product = Product(
            id = "p1",
            categoryId = "c1",
            name = "Sate Taichan Porsi",
            price = 25000.0,
            costPrice = 15000.0
        )
        assertEquals(10000.0, product.calculateProfitMargin(), 0.001)
        assertEquals(40.0, product.calculateMarginPercentage(), 0.001)
    }
}
