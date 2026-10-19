package com.kapos.mobile.feature.report

import com.kapos.mobile.domain.model.CashMovement
import com.kapos.mobile.domain.model.CashMovementType
import com.kapos.mobile.domain.model.DateFilterPreset
import com.kapos.mobile.domain.model.DateRangeFilter
import com.kapos.mobile.domain.model.Order
import com.kapos.mobile.domain.model.OrderItem
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.PaymentStatus
import com.kapos.mobile.domain.model.Product

enum class ReportPeriod(val displayName: String) {
    TODAY("Hari Ini"),
    WEEK("7 Hari"),
    MONTH("Bulan Ini"),
    ALL("Semua")
}

data class ProductSalesStat(
    val productName: String,
    val quantity: Int,
    val totalSales: Double
)

data class ReportUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.TODAY,
    val dateFilter: DateRangeFilter = when (selectedPeriod) {
        ReportPeriod.TODAY -> DateRangeFilter(DateFilterPreset.TODAY)
        ReportPeriod.WEEK -> DateRangeFilter(DateFilterPreset.LAST_7_DAYS)
        ReportPeriod.MONTH -> DateRangeFilter(DateFilterPreset.THIS_MONTH)
        ReportPeriod.ALL -> DateRangeFilter(DateFilterPreset.ALL)
    },
    val showDatePickerDialog: Boolean = false,
    val orders: List<Order> = emptyList(),
    val orderItems: List<OrderItem> = emptyList(),
    val cashMovements: List<CashMovement> = emptyList(),
    val products: List<Product> = emptyList(),
    val cashierShiftName: String = "Kasir Utama",
    val isExporting: Boolean = false,
    val snackbarMessage: String? = null
) {
    val activeDateFilter: DateRangeFilter
        get() = if (dateFilter.preset == DateFilterPreset.TODAY && selectedPeriod != ReportPeriod.TODAY) {
            when (selectedPeriod) {
                ReportPeriod.TODAY -> DateRangeFilter(DateFilterPreset.TODAY)
                ReportPeriod.WEEK -> DateRangeFilter(DateFilterPreset.LAST_7_DAYS)
                ReportPeriod.MONTH -> DateRangeFilter(DateFilterPreset.THIS_MONTH)
                ReportPeriod.ALL -> DateRangeFilter(DateFilterPreset.ALL)
            }
        } else {
            dateFilter
        }

    val periodTimeRange: Pair<Long, Long>
        get() = activeDateFilter.getTimeRange()

    val periodCutoff: Long
        get() = periodTimeRange.first

    val periodOrders: List<Order>
        get() {
            val (start, end) = periodTimeRange
            return orders.filter { it.createdAt in start..end && it.paymentStatus == PaymentStatus.PAID }
        }

    val periodVoidOrders: List<Order>
        get() {
            val (start, end) = periodTimeRange
            return orders.filter { it.createdAt in start..end && it.paymentStatus == PaymentStatus.VOID }
        }

    val periodCashMovements: List<CashMovement>
        get() {
            val (start, end) = periodTimeRange
            return cashMovements.filter { it.createdAt in start..end }
        }

    // Map orderId to Order for filtering items
    private val periodOrderIds: Set<String>
        get() = periodOrders.map { it.id }.toSet()

    val periodOrderItems: List<OrderItem>
        get() = orderItems.filter { it.orderId in periodOrderIds }

    // --- Financial P&L Components ---
    val grossRevenue: Double
        get() = periodOrders.sumOf { it.subtotal }

    val totalDiscount: Double
        get() = periodOrders.sumOf { it.discount }

    val netRevenue: Double
        get() = periodOrders.sumOf { it.total }

    val totalCogs: Double
        get() {
            val productCostMap = products.associate { it.id to it.costPrice }
            return periodOrderItems.sumOf { item ->
                val unitCost = productCostMap[item.productId] ?: (item.price * 0.5) // fallback to 50% if unmapped
                unitCost * item.quantity
            }
        }

    val grossProfit: Double
        get() = (netRevenue - totalCogs).coerceAtLeast(0.0)

    val grossProfitMarginPercent: Int
        get() = if (netRevenue > 0) ((grossProfit / netRevenue) * 100).toInt() else 0

    val operationalExpenses: Double
        get() = periodCashMovements.filter { it.type == CashMovementType.CASH_OUT }.sumOf { it.amount }

    val netProfit: Double
        get() = grossProfit - operationalExpenses

    val netProfitMarginPercent: Int
        get() = if (netRevenue > 0) ((netProfit / netRevenue) * 100).toInt() else 0

    // --- Transactions & Payments ---
    val totalTransactionsCount: Int
        get() = periodOrders.size

    val voidTransactionsCount: Int
        get() = periodVoidOrders.size

    val cashTotal: Double
        get() = periodOrders.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.total }

    val qrisTotal: Double
        get() = periodOrders.filter { it.paymentMethod == PaymentMethod.QRIS }.sumOf { it.total }

    val transferTotal: Double
        get() = periodOrders.filter { it.paymentMethod == PaymentMethod.TRANSFER }.sumOf { it.total }

    val nonCashTotal: Double
        get() = qrisTotal + transferTotal

    val averageOrderValue: Double
        get() = if (totalTransactionsCount > 0) netRevenue / totalTransactionsCount else 0.0

    // --- Top Products ---
    val topSellingProducts: List<ProductSalesStat>
        get() {
            return periodOrderItems
                .groupBy { it.productName }
                .map { (name, items) ->
                    ProductSalesStat(
                        productName = name,
                        quantity = items.sumOf { it.quantity },
                        totalSales = items.sumOf { it.subtotal }
                    )
                }
                .sortedByDescending { it.quantity }
                .take(5)
        }
}
