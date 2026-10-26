package com.kapos.mobile.core.data.repository

import org.junit.Assert.assertTrue
import org.junit.Test

class CleanSlateRepositoryTest {
    @Test
    fun stockRepository_startsEmptyWhenNoPreferences() {
        val repo = StockRepository(context = null)
        assertTrue("Taichan batches should be empty by default", repo.taichanBatches.value.isEmpty())
        assertTrue("Chicken batches should be empty by default", repo.chickenBatches.value.isEmpty())
    }
}
