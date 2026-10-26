package com.kapos.mobile.feature.stock

import com.kapos.mobile.core.data.repository.StockRepository
import com.kapos.mobile.domain.model.ChickenPart
import com.kapos.mobile.domain.model.StockFreshness
import com.kapos.mobile.domain.model.TaichanBatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StockViewModelTest {

    private lateinit var stockRepository: StockRepository
    private lateinit var viewModel: StockViewModel

    @Before
    fun setUp() {
        stockRepository = StockRepository(context = null)
        viewModel = StockViewModel(stockRepository)
    }

    @Test
    fun testFreshnessCalculation() {
        val now = System.currentTimeMillis()
        val oneDayMs = 24L * 60 * 60 * 1000

        val freshBatch = TaichanBatch("1", "2026-10-15", now, 100)
        assertEquals(StockFreshness.FRESH, freshBatch.freshness(now))
        assertTrue(freshBatch.freshness(now).isSellable)

        val day2Batch = TaichanBatch("2", "2026-10-14", now - oneDayMs, 100)
        assertEquals(StockFreshness.DAY_2, day2Batch.freshness(now))
        assertTrue(day2Batch.freshness(now).isSellable)

        val day3Batch = TaichanBatch("3", "2026-10-13", now - (2 * oneDayMs), 100)
        assertEquals(StockFreshness.DAY_3, day3Batch.freshness(now))
        assertTrue(day3Batch.freshness(now).isSellable)

        val expiredBatch = TaichanBatch("4", "2026-10-11", now - (4 * oneDayMs), 100)
        assertEquals(StockFreshness.EXPIRED, expiredBatch.freshness(now))
        assertFalse(expiredBatch.freshness(now).isSellable)
    }

    @Test
    fun testTaichanBatchAddAndRemainingStockCalculation() {
        val initialCount = viewModel.uiState.value.taichanBatches.size
        viewModel.addTaichanBatch(raw = 150, notes = "Batch siang")
        
        var attempts = 0
        while (viewModel.uiState.value.taichanBatches.size == initialCount && attempts < 20) {
            Thread.sleep(25)
            attempts++
        }

        val batches = viewModel.uiState.value.taichanBatches
        val latest = batches.last()
        assertEquals(150, latest.raw)

        // Goreng 80 tusuk
        viewModel.updateTaichanFried(latest.id, 80)
        attempts = 0
        while (viewModel.uiState.value.taichanBatches.find { it.id == latest.id }?.fried != 80 && attempts < 20) {
            Thread.sleep(25)
            attempts++
        }

        val updatedState = viewModel.uiState.value
        val updatedBatch = updatedState.taichanBatches.find { it.id == latest.id }
        assertEquals(80, updatedBatch?.fried)

        // Simulasikan 30 tusuk terjual hari ini
        viewModel.setTaichanSoldTodayForTest(30)
        attempts = 0
        while (viewModel.uiState.value.taichanSoldToday != 30 && attempts < 20) {
            Thread.sleep(25)
            attempts++
        }

        val stateAfterSales = viewModel.uiState.value
        assertEquals(30, stateAfterSales.taichanSoldToday)
        // Sisa stok siap jual: Total Fried (80 baru) - 30 sold = 50
        assertEquals(50, stateAfterSales.taichanRemaining)
    }

    @Test
    fun testChickenBatchOperations() {
        viewModel.addChickenBatch(part = ChickenPart.PAHA_ATAS, raw = 40, notes = "Restock paha")
        Thread.sleep(50)
        val batches = viewModel.uiState.value.chickenBatches
        val pahaBatch = batches.find { it.part == ChickenPart.PAHA_ATAS }
        assertTrue(pahaBatch != null)
        assertEquals(40, pahaBatch?.raw)

        viewModel.updateChickenFried(pahaBatch!!.id, 25)
        Thread.sleep(50)
        val updatedBatch = viewModel.uiState.value.chickenBatches.find { it.id == pahaBatch.id }
        assertEquals(25, updatedBatch?.fried)

        // Simulasikan penjualan 10 potong paha atas hari ini
        viewModel.setChickenSoldTodayForTest(mapOf(ChickenPart.PAHA_ATAS to 10))
        var attempts = 0
        while ((viewModel.uiState.value.chickenSoldToday[ChickenPart.PAHA_ATAS] ?: 0) != 10 && attempts < 20) {
            Thread.sleep(25)
            attempts++
        }

        val summary = viewModel.uiState.value.chickenSummaries[ChickenPart.PAHA_ATAS]
        assertEquals(40, summary?.totalRaw)
        assertEquals(25, summary?.totalFried)
        assertEquals(10, summary?.totalSold)
        // Sisa siap saji: 25 fried - 10 sold = 15
        assertEquals(15, summary?.remaining)
    }

    @Test
    fun testStockTabAuditAndFilter() {
        viewModel.onTabSelect(StockTab.AUDIT)
        var attempts = 0
        while (viewModel.uiState.value.activeTab != StockTab.AUDIT && attempts < 20) {
            Thread.sleep(25)
            attempts++
        }
        assertEquals(StockTab.AUDIT, viewModel.uiState.value.activeTab)

        viewModel.onSelectMovementFilter(com.kapos.mobile.domain.model.StockMovementType.RESTOCK)
        attempts = 0
        while (viewModel.uiState.value.movementFilterType != com.kapos.mobile.domain.model.StockMovementType.RESTOCK && attempts < 20) {
            Thread.sleep(25)
            attempts++
        }
        assertEquals(com.kapos.mobile.domain.model.StockMovementType.RESTOCK, viewModel.uiState.value.movementFilterType)
    }
}
