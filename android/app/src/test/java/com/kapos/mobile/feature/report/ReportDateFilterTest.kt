package com.kapos.mobile.feature.report

import com.kapos.mobile.domain.model.DateFilterPreset
import com.kapos.mobile.domain.model.DateRangeFilter
import com.kapos.mobile.domain.model.Order
import com.kapos.mobile.domain.model.OrderType
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.PaymentStatus
import com.kapos.mobile.domain.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ReportDateFilterTest {
    private fun createOrder(id: String, createdAt: Long, total: Double): Order {
        return Order(
            id = id,
            orderNo = "ORD-$id",
            cashierName = "Kasir",
            orderType = OrderType.DINE_IN,
            paymentMethod = PaymentMethod.CASH,
            paymentStatus = PaymentStatus.PAID,
            subtotal = total,
            discount = 0.0,
            total = total,
            syncStatus = SyncStatus.SYNCED,
            createdAt = createdAt
        )
    }

    @Test
    fun testPeriodOrders_calculatedWithinDateRange() {
        val cal = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 19, 10, 0) }
        val order1 = createOrder("1", cal.timeInMillis, 50000.0)

        cal.add(Calendar.DAY_OF_YEAR, -5)
        val order2 = createOrder("2", cal.timeInMillis, 30000.0)

        val stateToday = ReportUiState(
            orders = listOf(order1, order2),
            dateFilter = DateRangeFilter(preset = DateFilterPreset.TODAY)
        )
        assertEquals(50000.0, stateToday.netRevenue, 0.01)

        val state7Days = ReportUiState(
            orders = listOf(order1, order2),
            dateFilter = DateRangeFilter(preset = DateFilterPreset.LAST_7_DAYS)
        )
        assertEquals(80000.0, state7Days.netRevenue, 0.01)
    }

    @Test
    fun testPeriodOrders_customDateRange() {
        val startCal = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 10, 10, 0) }
        val orderOct10 = createOrder("1", startCal.timeInMillis, 75000.0)

        val outCal = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 15, 10, 0) }
        val orderOct15 = createOrder("2", outCal.timeInMillis, 40000.0)

        val customFilter = DateRangeFilter(
            preset = DateFilterPreset.CUSTOM,
            customStartMillis = startCal.timeInMillis,
            customEndMillis = startCal.timeInMillis
        )

        val state = ReportUiState(
            orders = listOf(orderOct10, orderOct15),
            dateFilter = customFilter
        )

        assertEquals(75000.0, state.netRevenue, 0.01)
        assertEquals(1, state.periodOrders.size)
    }
}
