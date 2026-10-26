package com.kapos.mobile.feature.report

import com.kapos.mobile.domain.model.CashMovement
import com.kapos.mobile.domain.model.CashMovementType
import com.kapos.mobile.domain.model.Order
import com.kapos.mobile.domain.model.OrderItem
import com.kapos.mobile.domain.model.OrderType
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.PaymentStatus
import com.kapos.mobile.domain.model.Product
import com.kapos.mobile.domain.model.ReportPeriod
import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialReportCalculationTest {

    private val now = System.currentTimeMillis()

    private val sampleProducts = listOf(
        Product(id = "p1", categoryId = "c1", name = "Sate Taichan Daging", price = 25000.0, costPrice = 14000.0),
        Product(id = "p2", categoryId = "c1", name = "Sate Taichan Kulit", price = 22000.0, costPrice = 11000.0),
        Product(id = "p3", categoryId = "c2", name = "Es Teh Manis", price = 5000.0, costPrice = 1500.0)
    )

    private val sampleOrders = listOf(
        Order(
            id = "o1",
            orderNo = "ORD-001",
            orderType = OrderType.DINE_IN,
            subtotal = 50000.0,
            discount = 5000.0,
            total = 45000.0,
            paymentMethod = PaymentMethod.CASH,
            paymentStatus = PaymentStatus.PAID,
            cashierName = "Kasir Utama",
            createdAt = now
        ),
        Order(
            id = "o2",
            orderNo = "ORD-002",
            orderType = OrderType.TAKE_AWAY,
            subtotal = 27000.0,
            discount = 0.0,
            total = 27000.0,
            paymentMethod = PaymentMethod.QRIS,
            paymentStatus = PaymentStatus.PAID,
            cashierName = "Kasir Pagi",
            createdAt = now
        ),
        Order(
            id = "o3",
            orderNo = "ORD-003",
            orderType = OrderType.DINE_IN,
            subtotal = 30000.0,
            discount = 0.0,
            total = 30000.0,
            paymentMethod = PaymentMethod.CASH,
            paymentStatus = PaymentStatus.VOID, // VOID should not count into net revenue
            cashierName = "Kasir Utama",
            createdAt = now
        )
    )

    private val sampleOrderItems = listOf(
        // Items for o1: 2x Taichan Daging (price 25k, cost 14k)
        OrderItem(id = "i1", orderId = "o1", productId = "p1", productName = "Sate Taichan Daging", price = 25000.0, quantity = 2, spiceLevel = "Pedas", notes = "", subtotal = 50000.0),
        // Items for o2: 1x Taichan Kulit (price 22k, cost 11k) + 1x Es Teh (price 5k, cost 1.5k)
        OrderItem(id = "i2", orderId = "o2", productId = "p2", productName = "Sate Taichan Kulit", price = 22000.0, quantity = 1, spiceLevel = "Sedang", notes = "", subtotal = 22000.0),
        OrderItem(id = "i3", orderId = "o2", productId = "p3", productName = "Es Teh Manis", price = 5000.0, quantity = 1, spiceLevel = "", notes = "", subtotal = 5000.0),
        // Items for o3 (VOID order items should be ignored)
        OrderItem(id = "i4", orderId = "o3", productId = "p1", productName = "Sate Taichan Daging", price = 25000.0, quantity = 1, spiceLevel = "Pedas", notes = "", subtotal = 25000.0)
    )

    private val sampleCashMovements = listOf(
        CashMovement(id = "cm1", shiftId = "s1", type = CashMovementType.CASH_OUT, category = "Beli Es Batu", amount = 15000.0, createdAt = now),
        CashMovement(id = "cm2", shiftId = "s1", type = CashMovementType.CASH_IN, category = "Setoran Tambahan", amount = 50000.0, createdAt = now)
    )

    @Test
    fun testFinancialStatementFormulas() {
        val state = ReportUiState(
            selectedPeriod = ReportPeriod.TODAY,
            orders = sampleOrders,
            orderItems = sampleOrderItems,
            cashMovements = sampleCashMovements,
            products = sampleProducts
        )

        // 1. Transactions: 2 PAID orders, 1 VOID order
        assertEquals(2, state.totalTransactionsCount)
        assertEquals(1, state.voidTransactionsCount)

        // 2. Gross Revenue: 50.000 + 27.000 = 77.000
        assertEquals(77000.0, state.grossRevenue, 0.001)

        // 3. Discount: 5.000
        assertEquals(5000.0, state.totalDiscount, 0.001)

        // 4. Net Revenue: 45.000 + 27.000 = 72.000
        assertEquals(72000.0, state.netRevenue, 0.001)

        // 5. Total COGS (HPP):
        // 2x Taichan Daging = 2 * 14.000 = 28.000
        // 1x Taichan Kulit = 1 * 11.000 = 11.000
        // 1x Es Teh Manis = 1 * 1.500 = 1.500
        // Total COGS = 28.000 + 11.000 + 1.500 = 40.500
        assertEquals(40500.0, state.totalCogs, 0.001)

        // 6. Gross Profit = Net Revenue (72.000) - COGS (40.500) = 31.500
        assertEquals(31500.0, state.grossProfit, 0.001)
        // Gross Profit Margin % = (31.500 / 72.000) * 100 = 43.75% -> 43%
        assertEquals(43, state.grossProfitMarginPercent)

        // 7. Operational Expenses (Kas Keluar) = 15.000
        assertEquals(15000.0, state.operationalExpenses, 0.001)

        // 8. Net Profit = Gross Profit (31.500) - Expenses (15.000) = 16.500
        assertEquals(16500.0, state.netProfit, 0.001)
        // Net Profit Margin % = (16.500 / 72.000) * 100 = 22.9% -> 22%
        assertEquals(22, state.netProfitMarginPercent)
    }

    @Test
    fun testPaymentMethodBreakdown() {
        val state = ReportUiState(
            selectedPeriod = ReportPeriod.TODAY,
            orders = sampleOrders,
            orderItems = sampleOrderItems,
            cashMovements = sampleCashMovements,
            products = sampleProducts
        )

        // Order 1 was CASH: 45.000
        assertEquals(45000.0, state.cashTotal, 0.001)
        // Order 2 was QRIS: 27.000
        assertEquals(27000.0, state.qrisTotal, 0.001)
        assertEquals(27000.0, state.nonCashTotal, 0.001)
    }

    @Test
    fun testTopSellingProductsRanking() {
        val state = ReportUiState(
            selectedPeriod = ReportPeriod.TODAY,
            orders = sampleOrders,
            orderItems = sampleOrderItems,
            cashMovements = sampleCashMovements,
            products = sampleProducts
        )

        val top = state.topSellingProducts
        assertEquals(3, top.size)
        // First is Taichan Daging (2 porsi)
        assertEquals("Sate Taichan Daging", top[0].productName)
        assertEquals(2, top[0].quantity)
        assertEquals(50000.0, top[0].totalSales, 0.001)
    }
}
