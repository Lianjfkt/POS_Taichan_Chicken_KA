package com.kapos.mobile.feature.shift

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftViewModelTest {
    @Test
    fun testBlindCountSum() {
        val breakdown = mapOf(
            100_000 to 5, // 500.000
            50_000 to 4,  // 200.000
            20_000 to 5,  // 100.000
            10_000 to 0,
            5_000 to 0,
            2_000 to 0,
            1_000 to 0
        )
        val coin = 15_000.0
        val total = breakdown.entries.sumOf { it.key * it.value }.toDouble() + coin
        assertEquals(815_000.0, total, 0.001)
    }

    @Test
    fun testReconciliationStatusDisplay() {
        val expected = 500_000.0
        val exact = 500_000.0
        val shortage = 480_000.0
        val overage = 520_000.0

        assertEquals(0.0, exact - expected, 0.001)
        assertEquals(-20_000.0, shortage - expected, 0.001)
        assertEquals(20_000.0, overage - expected, 0.001)
    }
}
