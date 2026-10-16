package com.kapos.mobile.feature.analytics

import com.kapos.mobile.core.data.repository.AppSettings
import com.kapos.mobile.core.data.repository.SettingsRepository
import com.kapos.mobile.domain.model.AnalyticsPeriod
import com.kapos.mobile.domain.model.Order
import com.kapos.mobile.domain.model.OrderType
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.PaymentStatus
import com.kapos.mobile.domain.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnalyticsViewModelTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: AnalyticsViewModel

    @Before
    fun setUp() {
        settingsRepository = SettingsRepository(context = null)
        settingsRepository.saveSettings(
            AppSettings(
                dailyFixedCost = 350000.0,
                dailyRevenueTarget = 1500000.0
            )
        )
        viewModel = AnalyticsViewModel(
            posRepository = null,
            settingsRepository = settingsRepository
        )
    }

    @Test
    fun testInitialZeroRevenueBepMetrics() {
        val summary = viewModel.uiState.value.summary
        assertEquals(0.0, summary.grossRevenue, 0.01)
        assertEquals(0.0, summary.bepProgressPercentage, 0.01)
        assertEquals(1500000.0, summary.bepRemainingRevenue, 0.01)
        assertFalse(summary.isBepAchieved)
    }

    @Test
    fun testRevenueBelowBepTarget() {
        val now = System.currentTimeMillis()
        val mockOrders = listOf(
            createMockOrder("1", 700000.0, now),
            createMockOrder("2", 500000.0, now)
        )
        viewModel.setOrdersForTest(mockOrders)

        val summary = viewModel.uiState.value.summary
        // Total Omzet = 1.200.000
        assertEquals(1200000.0, summary.grossRevenue, 0.01)
        assertEquals(1500000.0, summary.bepRevenueTarget, 0.01)
        // BEP progress = (1.200.000 / 1.500.000) * 100 = 80.0%
        assertEquals(80.0, summary.bepProgressPercentage, 0.01)
        assertEquals(300000.0, summary.bepRemainingRevenue, 0.01)
        assertFalse(summary.isBepAchieved)

        // COGS 55% = 660.000
        assertEquals(660000.0, summary.totalCogs, 0.01)
        // Gross Profit = 1.200.000 - 660.000 = 540.000
        assertEquals(540000.0, summary.grossProfit, 0.01)
        // Fixed Cost = 350.000
        assertEquals(350000.0, summary.fixedCost, 0.01)
        // Net Profit = 540.000 - 350.000 = 190.000
        assertEquals(190000.0, summary.netProfit, 0.01)
    }

    @Test
    fun testRevenueAboveBepTarget() {
        val now = System.currentTimeMillis()
        val mockOrders = listOf(
            createMockOrder("1", 1000000.0, now),
            createMockOrder("2", 800000.0, now)
        )
        viewModel.setOrdersForTest(mockOrders)

        val summary = viewModel.uiState.value.summary
        // Total Omzet = 1.800.000 (Target 1.500.000)
        assertEquals(1800000.0, summary.grossRevenue, 0.01)
        assertEquals(100.0, summary.bepProgressPercentage, 0.01)
        assertEquals(0.0, summary.bepRemainingRevenue, 0.01)
        assertTrue(summary.isBepAchieved)
    }

    @Test
    fun testPeriodChangeMultipliesFixedCostAndTarget() {
        viewModel.onPeriodChange(AnalyticsPeriod.WEEK)
        val summary = viewModel.uiState.value.summary
        // 7 Hari: target = 1.500.000 * 7 = 10.500.000
        assertEquals(10500000.0, summary.bepRevenueTarget, 0.01)
        assertEquals(350000.0 * 7, summary.fixedCost, 0.01)
    }

    private fun createMockOrder(id: String, total: Double, createdAt: Long): Order {
        return Order(
            id = id,
            orderNo = "ORD-$id",
            customerName = "Pelanggan",
            customerPhone = null,
            orderType = OrderType.DINE_IN,
            tableNo = "1",
            subtotal = total,
            discount = 0.0,
            total = total,
            paymentMethod = PaymentMethod.CASH,
            paymentStatus = PaymentStatus.PAID,
            cashierName = "Kasir Utama",
            notes = null,
            createdAt = createdAt,
            paidAt = createdAt,
            syncStatus = SyncStatus.SYNCED
        )
    }
}
