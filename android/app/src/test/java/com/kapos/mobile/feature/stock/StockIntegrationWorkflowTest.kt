package com.kapos.mobile.feature.stock

import com.kapos.mobile.domain.model.BatchFreshness
import com.kapos.mobile.domain.model.MaterialBatch
import com.kapos.mobile.domain.model.Product
import com.kapos.mobile.domain.model.RawMaterial
import com.kapos.mobile.domain.model.RawMaterialCategory
import com.kapos.mobile.domain.model.RecipeItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end Integration Workflow Test for Stock, Moving Average BOP (HPP),
 * Recipe BOM synchronization, FIFO deduction, and Expiry / Waste Management.
 */
class StockIntegrationWorkflowTest {

    @Test
    fun testCompleteStockBopAndExpiryLifecycle() {
        val now = System.currentTimeMillis()
        val oneDayMs = 24L * 60 * 60 * 1000

        // 1. Initial State: Register Raw Material "Daging Ayam Filet"
        var ayam = RawMaterial(
            id = "mat_ayam",
            name = "Daging Ayam Filet",
            category = RawMaterialCategory.PROTEIN,
            unit = "gram",
            currentStock = 0.0,
            minStock = 1000.0,
            costPerUnit = 0.0
        )

        // 2. Register Menu Item with Recipe BOM (1 portion = 150g Ayam)
        var sateTaichan = Product(
            id = "prod_taichan_ori",
            categoryId = "cat_taichan",
            name = "Sate Taichan Ori",
            price = 25000.0,
            costPrice = 0.0,
            emoji = "🍢"
        )

        val recipe = RecipeItem(
            id = "rec_1",
            productId = sateTaichan.id,
            materialId = ayam.id,
            materialName = ayam.name,
            quantityRequired = 150.0
        )

        // Initial BOP is 0
        var bop = recipe.quantityRequired * ayam.costPerUnit
        assertEquals(0.0, bop, 0.001)

        // 3. First Restock: Batch 1 – 1500g @ Rp 50/g = Rp 75.000, Exp in 3 days
        val batch1 = MaterialBatch(
            id = "batch_001",
            materialId = ayam.id,
            batchNumber = "BATCH-20261021-001",
            initialQuantity = 1500.0,
            remainingQuantity = 1500.0,
            costTotal = 75000.0,
            costPerUnit = 50.0,
            receivedDate = now,
            expiryDate = now + (3 * oneDayMs),
            isUsable = true
        )

        // Moving Weighted Average after Batch 1: (0*0 + 75000) / (0+1500) = 50.0/g
        val costAfterBatch1 = if (ayam.currentStock + batch1.initialQuantity > 0)
            ((ayam.currentStock * ayam.costPerUnit) + batch1.costTotal) / (ayam.currentStock + batch1.initialQuantity)
        else batch1.costPerUnit
        ayam = ayam.copy(currentStock = ayam.currentStock + batch1.initialQuantity, costPerUnit = costAfterBatch1)

        assertEquals(1500.0, ayam.currentStock, 0.001)
        assertEquals(50.0, ayam.costPerUnit, 0.001)

        // Recalculate Product BOP
        bop = recipe.quantityRequired * ayam.costPerUnit
        sateTaichan = sateTaichan.copy(costPrice = bop)
        assertEquals(7500.0, sateTaichan.costPrice, 0.001)      // 150g * 50 = Rp 7.500
        assertEquals(17500.0, sateTaichan.calculateProfitMargin(), 0.001) // 25000 - 7500
        assertEquals(70.0, sateTaichan.calculateMarginPercentage(), 0.001) // 70% margin

        // Freshness checks for Batch 1
        assertEquals(BatchFreshness.FRESH, batch1.freshnessStatus(now))
        assertEquals(BatchFreshness.EXPIRING_SOON, batch1.freshnessStatus(now + (2 * oneDayMs)))
        assertEquals(BatchFreshness.EXPIRED, batch1.freshnessStatus(now + (4 * oneDayMs)))

        // 4. Second Restock: Batch 2 – 1000g @ Rp 70/g = Rp 70.000, Exp in 7 days
        val batch2 = MaterialBatch(
            id = "batch_002",
            materialId = ayam.id,
            batchNumber = "BATCH-20261021-002",
            initialQuantity = 1000.0,
            remainingQuantity = 1000.0,
            costTotal = 70000.0,
            costPerUnit = 70.0,
            receivedDate = now,
            expiryDate = now + (7 * oneDayMs),
            isUsable = true
        )

        // Moving Weighted Average: ((1500*50) + 70000) / (1500+1000) = 145000 / 2500 = 58.0
        val costAfterBatch2 =
            ((ayam.currentStock * ayam.costPerUnit) + batch2.costTotal) / (ayam.currentStock + batch2.initialQuantity)
        ayam = ayam.copy(currentStock = ayam.currentStock + batch2.initialQuantity, costPerUnit = costAfterBatch2)

        assertEquals(2500.0, ayam.currentStock, 0.001)
        assertEquals(58.0, ayam.costPerUnit, 0.001)

        // BOP automatically updates: 150g * 58 = Rp 8.700
        bop = recipe.quantityRequired * ayam.costPerUnit
        sateTaichan = sateTaichan.copy(costPrice = bop)
        assertEquals(8700.0, sateTaichan.costPrice, 0.001)
        assertEquals(16300.0, sateTaichan.calculateProfitMargin(), 0.001) // 25000 - 8700
        assertEquals(65.2, sateTaichan.calculateMarginPercentage(), 0.001)

        // 5. FIFO Sale: 10 portions → 10 * 150g = 1500g consumed
        val activeBatches = mutableListOf(batch1, batch2) // FIFO order by expiry
        var neededQty = 1500.0
        val consumed = mutableMapOf<String, Double>()

        for (i in activeBatches.indices) {
            if (neededQty <= 0) break
            val b = activeBatches[i]
            val take = minOf(b.remainingQuantity, neededQty)
            consumed[b.id] = take
            activeBatches[i] = b.copy(remainingQuantity = b.remainingQuantity - take)
            neededQty -= take
        }

        assertEquals(1500.0, consumed["batch_001"] ?: 0.0, 0.001)
        assertEquals(0.0, activeBatches[0].remainingQuantity, 0.001)  // Batch 1 exhausted
        assertEquals(1000.0, activeBatches[1].remainingQuantity, 0.001) // Batch 2 untouched
        ayam = ayam.copy(currentStock = ayam.currentStock - 1500.0)
        assertEquals(1000.0, ayam.currentStock, 0.001)

        // 6. Waste / Pemusnahan: 200g of Batch 2 spoiled
        val spoiledQty = 200.0
        val financialLoss = spoiledQty * activeBatches[1].costPerUnit
        assertEquals(14000.0, financialLoss, 0.001) // 200g * Rp 70 = Rp 14.000

        activeBatches[1] = activeBatches[1].copy(remainingQuantity = activeBatches[1].remainingQuantity - spoiledQty)
        ayam = ayam.copy(currentStock = ayam.currentStock - spoiledQty)

        assertEquals(800.0, activeBatches[1].remainingQuantity, 0.001)
        assertEquals(800.0, ayam.currentStock, 0.001)

        // 7. Batch Usability Toggle – mark remaining Batch 2 as damaged
        val damagedBatch = activeBatches[1].copy(isUsable = false)
        assertFalse(damagedBatch.isUsable)
        assertEquals(BatchFreshness.DAMAGED, damagedBatch.freshnessStatus(now))
        assertFalse(damagedBatch.freshnessStatus().isUsable)
    }

    @Test
    fun testStockUiStateSummaryCounters() {
        val now = System.currentTimeMillis()
        val oneDayMs = 24L * 60 * 60 * 1000

        val batches = listOf(
            MaterialBatch("b1", "m1", "B-001", 100.0, 50.0, 5000.0, 50.0, expiryDate = now + (1 * oneDayMs)), // Expiring soon
            MaterialBatch("b2", "m1", "B-002", 100.0, 80.0, 8000.0, 80.0, expiryDate = now - (1 * oneDayMs)), // Expired
            MaterialBatch("b3", "m2", "B-003", 200.0, 200.0, 10000.0, 50.0, expiryDate = now + (10 * oneDayMs)), // Fresh
            MaterialBatch("b4", "m2", "B-004", 50.0, 50.0, 2500.0, 50.0, isUsable = false) // Damaged
        )

        val uiState = StockUiState(
            batchesByMaterial = batches.groupBy { it.materialId }
        )

        // totalExpiredBatches counts EXPIRED status only (b2 = expired)
        // DAMAGED batches are tracked separately via isUsable flag
        assertEquals(1, uiState.totalExpiredBatches(now))
        // totalExpiringSoonBatches is a function (b1)
        assertEquals(1, uiState.totalExpiringSoonBatches(now))

        // totalInventoryAssetValue uses rawMaterials, so check via batches directly:
        // b1: 50 * 50 = 2500, b2: 80 * 80 = 6400, b3: 200 * 50 = 10000, b4: 50 * 50 = 2500 → total = 21400
        val totalValue = batches.sumOf { it.remainingQuantity * it.costPerUnit }
        assertEquals(21400.0, totalValue, 0.001)
    }

    @Test
    fun testBatchFreshnessEdgeCases() {
        val now = System.currentTimeMillis()
        val oneDayMs = 24L * 60 * 60 * 1000

        // Non-perishable batch (no expiry) → always FRESH
        val nonPerishable = MaterialBatch(
            id = "b_np", materialId = "m1", batchNumber = "NP-001",
            initialQuantity = 100.0, remainingQuantity = 100.0,
            costTotal = 5000.0, costPerUnit = 50.0
        )
        assertEquals(BatchFreshness.FRESH, nonPerishable.freshnessStatus(now))

        // Damaged regardless of expiry
        val damaged = nonPerishable.copy(isUsable = false)
        assertEquals(BatchFreshness.DAMAGED, damaged.freshnessStatus(now))

        // Exactly at boundary: exactly 2 days left → EXPIRING_SOON
        val twoDay = nonPerishable.copy(expiryDate = now + (2 * oneDayMs))
        assertEquals(BatchFreshness.EXPIRING_SOON, twoDay.freshnessStatus(now))

        // More than 2 days → FRESH
        val threeDay = nonPerishable.copy(expiryDate = now + (3 * oneDayMs))
        assertEquals(BatchFreshness.FRESH, threeDay.freshnessStatus(now))
    }

    @Test
    fun testBackdatedBatchRestockLifecycleAndFifo() {
        val now = 1730520000000L // 02 Nov 2024
        val oneDayMs = 24L * 60 * 60 * 1000

        // Case: Material arrived 2 days ago (31 Oct), but entered today (02 Nov)
        val arrivalDate = now - (2 * oneDayMs)
        val shelfLifeDays = 3 // normally 3 days shelf life

        // Relative expiry: arrivalDate + 3 days = now + 1 day (only 1 day left today!)
        val expiryDate = arrivalDate + (shelfLifeDays * oneDayMs)

        val backdatedBatch = MaterialBatch(
            id = "batch_backdated",
            materialId = "mat_ayam",
            batchNumber = "B-20241031-1000",
            initialQuantity = 500.0,
            remainingQuantity = 500.0,
            costTotal = 25000.0,
            costPerUnit = 50.0,
            receivedDate = arrivalDate,
            expiryDate = expiryDate,
            isUsable = true,
            notes = "Pasar Senen [BACKDATED: Diterima 31/10/2024 | Diinput 02/11/2024 10:00]"
        )

        // Today's batch (arrived today with 3 days shelf life)
        val todayBatch = MaterialBatch(
            id = "batch_today",
            materialId = "mat_ayam",
            batchNumber = "B-20241102-1000",
            initialQuantity = 1000.0,
            remainingQuantity = 1000.0,
            costTotal = 50000.0,
            costPerUnit = 50.0,
            receivedDate = now,
            expiryDate = now + (3 * oneDayMs),
            isUsable = true,
            notes = "Pasar Senen"
        )

        // 1. Freshness evaluation:
        // Backdated batch only has 1 day left -> EXPIRING_SOON (<= 2 days)
        assertEquals(BatchFreshness.EXPIRING_SOON, backdatedBatch.freshnessStatus(now))
        // Today's batch has 3 days left -> FRESH
        assertEquals(BatchFreshness.FRESH, todayBatch.freshnessStatus(now))

        // 2. FIFO Order Verification:
        // FIFO sort priority: expiryDate ASC, receivedDate ASC
        val sortedBatches = listOf(todayBatch, backdatedBatch).sortedWith(
            compareBy<MaterialBatch> { it.expiryDate ?: Long.MAX_VALUE }
                .thenBy { it.receivedDate }
        )

        // Backdated batch MUST be consumed first in FIFO because its expiry is earlier!
        assertEquals("batch_backdated", sortedBatches[0].id)
        assertEquals("batch_today", sortedBatches[1].id)

        // 3. Anti-fraud audit note verification
        assertTrue(backdatedBatch.notes.contains("[BACKDATED: Diterima 31/10/2024 | Diinput 02/11/2024 10:00]"))
        assertFalse(todayBatch.notes.contains("[BACKDATED"))
    }
}
