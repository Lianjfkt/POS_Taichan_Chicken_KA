package com.kapos.mobile.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryBatchCalculationTest {

    @Test
    fun testMovingWeightedAverageCalculation() {
        val currentStock = 1000.0 // 1 kg
        val currentCost = 40.0    // Rp 40/g = Rp 40.000/kg
        val newQty = 2000.0       // 2 kg
        val newCostTotal = 100000.0 // Rp 100.000 (Rp 50/g)

        // Formula: ( (1000 * 40) + 100000 ) / (1000 + 2000) = 140000 / 3000 = 46.6667
        val expectedNewCost = ((currentStock * currentCost) + newCostTotal) / (currentStock + newQty)
        assertEquals(46.667, expectedNewCost, 0.001)
    }

    @Test
    fun testProductBopCalculationFromRecipes() {
        // Product 1 portion uses:
        // - 150g Ayam Fillet @ Rp 50/g = Rp 7.500
        // - 10 pcs Tusuk Sate @ Rp 30/pc = Rp 300
        // - 25g Cabe Rawit @ Rp 45/g = Rp 1.125
        val ayamCost = 150.0 * 50.0
        val tusukCost = 10.0 * 30.0
        val cabeCost = 25.0 * 45.0
        val totalBop = ayamCost + tusukCost + cabeCost

        assertEquals(8925.0, totalBop, 0.001)
    }

    @Test
    fun testFifoBatchDeductionAllocation() {
        // Batch 1: 50 remaining
        // Batch 2: 100 remaining
        // Deduction needed: 80
        val batches = mutableListOf(Pair("b1", 50.0), Pair("b2", 100.0))
        var needed = 80.0
        val deducted = mutableMapOf<String, Double>()

        for (i in batches.indices) {
            if (needed <= 0) break
            val b = batches[i]
            val take = minOf(b.second, needed)
            deducted[b.first] = take
            batches[i] = Pair(b.first, b.second - take)
            needed -= take
        }

        assertEquals(50.0, deducted["b1"] ?: 0.0, 0.001)
        assertEquals(30.0, deducted["b2"] ?: 0.0, 0.001)
        assertEquals(0.0, batches[0].second, 0.001)
        assertEquals(70.0, batches[1].second, 0.001)
    }
}
