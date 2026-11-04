package com.kapos.mobile.feature.stock

import com.kapos.mobile.domain.model.RawMaterial
import com.kapos.mobile.domain.model.RawMaterialCategory
import com.kapos.mobile.domain.model.StockFreshness
import com.kapos.mobile.domain.model.StockMovementType
import com.kapos.mobile.domain.model.TaichanBatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StockViewModelTest {

    private lateinit var viewModel: StockViewModel

    @Before
    fun setUp() {
        viewModel = StockViewModel()
    }

    private fun waitUntil(maxWaitMs: Long = 1000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - start > maxWaitMs) {
                break
            }
            Thread.sleep(10)
        }
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
    fun testTabSelectionAndMovementFilter() {
        assertEquals(StockTab.INVENTORY, viewModel.uiState.value.activeTab)

        viewModel.onTabSelect(StockTab.AUDIT)
        waitUntil { viewModel.uiState.value.activeTab == StockTab.AUDIT }
        assertEquals(StockTab.AUDIT, viewModel.uiState.value.activeTab)

        viewModel.onSelectMovementFilter(StockMovementType.RESTOCK)
        waitUntil { viewModel.uiState.value.movementFilterType == StockMovementType.RESTOCK }
        assertEquals(StockMovementType.RESTOCK, viewModel.uiState.value.movementFilterType)

        viewModel.onSelectMovementFilter(null)
        waitUntil { viewModel.uiState.value.movementFilterType == null }
        assertEquals(null, viewModel.uiState.value.movementFilterType)
    }

    @Test
    fun testCategoryAndSearchFilters() {
        viewModel.onMaterialCategorySelect(RawMaterialCategory.PROTEIN)
        waitUntil { viewModel.uiState.value.selectedMaterialCategory == RawMaterialCategory.PROTEIN }
        assertEquals(RawMaterialCategory.PROTEIN, viewModel.uiState.value.selectedMaterialCategory)

        viewModel.onMaterialSearchQueryChange("Ayam")
        waitUntil { viewModel.uiState.value.materialSearchQuery == "Ayam" }
        assertEquals("Ayam", viewModel.uiState.value.materialSearchQuery)

        viewModel.onMaterialCategorySelect(null)
        waitUntil { viewModel.uiState.value.selectedMaterialCategory == null }
        assertEquals(null, viewModel.uiState.value.selectedMaterialCategory)
    }

    @Test
    fun testAddEditDialogState() {
        assertFalse(viewModel.uiState.value.isAddEditMaterialOpen)

        viewModel.openAddMaterial()
        waitUntil { viewModel.uiState.value.isAddEditMaterialOpen }
        assertTrue(viewModel.uiState.value.isAddEditMaterialOpen)
        assertEquals(null, viewModel.uiState.value.materialToEdit)

        val sampleMaterial = RawMaterial(
            id = "m1",
            name = "Beras Premium",
            category = RawMaterialCategory.KARBO,
            unit = "kg",
            currentStock = 10.0,
            minStock = 5.0,
            costPerUnit = 14000.0
        )
        viewModel.openEditMaterial(sampleMaterial)
        waitUntil { viewModel.uiState.value.materialToEdit?.name == "Beras Premium" }
        assertTrue(viewModel.uiState.value.isAddEditMaterialOpen)
        assertEquals("Beras Premium", viewModel.uiState.value.materialToEdit?.name)

        viewModel.closeAddEditMaterial()
        waitUntil { !viewModel.uiState.value.isAddEditMaterialOpen && viewModel.uiState.value.materialToEdit == null }
        assertFalse(viewModel.uiState.value.isAddEditMaterialOpen)
        assertEquals(null, viewModel.uiState.value.materialToEdit)
    }
}
