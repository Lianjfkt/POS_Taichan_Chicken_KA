package com.kapos.mobile.feature.stock

import com.kapos.mobile.domain.model.BatchFreshness
import com.kapos.mobile.domain.model.MaterialBatch
import com.kapos.mobile.domain.model.RawMaterial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StockBatchViewModelTest {

    @Test
    fun testStockUiStateExpirySummaryCounters() {
        val now = 1000000000L
        val day = 86400000L
        val batches = listOf(
            MaterialBatch("b1", "m1", "B-1", 100.0, 50.0, 5000.0, 50.0, now, now - day, true), // EXPIRED
            MaterialBatch("b2", "m1", "B-2", 100.0, 80.0, 4000.0, 40.0, now, now + day, true), // EXPIRING SOON
            MaterialBatch("b3", "m2", "B-3", 100.0, 100.0, 3000.0, 30.0, now, now + (5 * day), true) // FRESH
        )

        val state = StockUiState(
            batchesByMaterial = mapOf("m1" to batches.take(2), "m2" to batches.takeLast(1))
        )

        assertEquals(1, state.totalExpiredBatches(now))
        assertEquals(1, state.totalExpiringSoonBatches(now))
    }
}
