package com.kapos.mobile.feature.transactions

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

class TransactionsDateFilterTest {
    private fun createOrder(id: String, createdAt: Long, status: PaymentStatus = PaymentStatus.PAID, orderNo: String = "ORD-$id"): Order {
        return Order(
            id = id,
            orderNo = orderNo,
            cashierName = "Kasir",
            orderType = OrderType.DINE_IN,
            paymentMethod = PaymentMethod.CASH,
            paymentStatus = status,
            subtotal = 10000.0,
            discount = 0.0,
            total = 10000.0,
            syncStatus = SyncStatus.SYNCED,
            createdAt = createdAt
        )
    }

    @Test
    fun testFilteredOrders_filtersByDateRange() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }
        val todayMillis = cal.timeInMillis
        val yesterdayMillis = cal.apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis

        val orderToday = createOrder("1", todayMillis)
        val orderYesterday = createOrder("2", yesterdayMillis)

        val state = TransactionsUiState(
            orders = listOf(orderToday, orderYesterday),
            dateFilter = DateRangeFilter(preset = DateFilterPreset.TODAY)
        )

        val filtered = state.filteredOrders
        assertEquals(1, filtered.size)
        assertEquals("1", filtered[0].id)
    }

    @Test
    fun testFilteredOrders_combinedWithStatusAndSearch() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }
        val todayMillis = cal.timeInMillis

        val orderPaidToday = createOrder("1", todayMillis, PaymentStatus.PAID, "ORD-PAID-01")
        val orderUnpaidToday = createOrder("2", todayMillis, PaymentStatus.UNPAID, "ORD-UNPAID-02")

        val state = TransactionsUiState(
            orders = listOf(orderPaidToday, orderUnpaidToday),
            dateFilter = DateRangeFilter(preset = DateFilterPreset.TODAY),
            filter = TransactionFilter.PAID,
            searchQuery = "01"
        )

        val filtered = state.filteredOrders
        assertEquals(1, filtered.size)
        assertEquals("1", filtered[0].id)
    }
}
