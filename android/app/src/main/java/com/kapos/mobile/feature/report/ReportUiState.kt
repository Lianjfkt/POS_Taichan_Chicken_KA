package com.kapos.mobile.feature.report

import com.kapos.mobile.domain.model.Order
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.PaymentStatus

enum class ReportPeriod(val displayName: String) {
    TODAY("Hari Ini"),
    WEEK("7 Hari"),
    MONTH("Bulan Ini")
}

data class ReportUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.TODAY,
    val orders: List<Order> = emptyList(),
    val cashierShiftName: String = "Kasir Utama"
) {
    private val periodCutoff: Long
        get() {
            val now = System.currentTimeMillis()
            return when (selectedPeriod) {
                ReportPeriod.TODAY -> {
                    val cal = java.util.Calendar.getInstance()
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.timeInMillis
                }
                ReportPeriod.WEEK -> now - 7L * 24 * 60 * 60 * 1000
                ReportPeriod.MONTH -> now - 30L * 24 * 60 * 60 * 1000
            }
        }

    val periodOrders: List<Order>
        get() = orders.filter { it.createdAt >= periodCutoff && it.paymentStatus == PaymentStatus.PAID }

    val totalOmzet: Double
        get() = periodOrders.sumOf { it.total }

    val totalTransactionsCount: Int
        get() = periodOrders.size

    val cashTotal: Double
        get() = periodOrders.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.total }

    val nonCashTotal: Double
        get() = periodOrders.filter { it.paymentMethod != PaymentMethod.CASH }.sumOf { it.total }

    val averageOrderValue: Double
        get() = if (totalTransactionsCount > 0) totalOmzet / totalTransactionsCount else 0.0
}
