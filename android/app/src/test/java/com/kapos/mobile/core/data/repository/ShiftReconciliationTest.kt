package com.kapos.mobile.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftReconciliationTest {
    @Test
    fun testReconciliationFormula() {
        val startCash = 200_000.0
        val cashSales = 500_000.0
        val cashIn = 50_000.0
        val cashOut = 30_000.0
        val expectedCash = startCash + cashSales + cashIn - cashOut
        val actualCash = 720_000.0
        val selisih = actualCash - expectedCash

        assertEquals(720_000.0, expectedCash, 0.001)
        assertEquals(0.0, selisih, 0.001)
    }

    @Test
    fun testReconciliationDifferenceShortage() {
        val startCash = 200_000.0
        val cashSales = 500_000.0
        val cashIn = 0.0
        val cashOut = 50_000.0
        val expectedCash = startCash + cashSales + cashIn - cashOut
        val actualCash = 600_000.0
        val selisih = actualCash - expectedCash

        assertEquals(650_000.0, expectedCash, 0.001)
        assertEquals(-50_000.0, selisih, 0.001)
    }
}
