package com.kapos.mobile.core.data.repository

import com.kapos.mobile.domain.model.ChickenPart
import org.junit.Assert.assertTrue
import org.junit.Test

class StockRepositoryClearTest {
    @Test
    fun clearAllBatches_emptiesBatchesStateFlow() {
        val repo = StockRepository(context = null)
        repo.addTaichanBatch(100, "Test")
        repo.addChickenBatch(ChickenPart.SAYAP, 20, "Test")
        repo.clearAllBatches()

        assertTrue(repo.taichanBatches.value.isEmpty())
        assertTrue(repo.chickenBatches.value.isEmpty())
    }
}
