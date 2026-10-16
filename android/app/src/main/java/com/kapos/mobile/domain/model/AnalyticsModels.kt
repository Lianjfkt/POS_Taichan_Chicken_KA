package com.kapos.mobile.domain.model

enum class AnalyticsPeriod(val displayName: String, val days: Int) {
    TODAY("Hari Ini", 1),
    WEEK("7 Hari", 7),
    MONTH("30 Hari", 30)
}

data class FinancialSummary(
    val grossRevenue: Double = 0.0,
    val totalCogs: Double = 0.0,
    val fixedCost: Double = 0.0,
    val bepRevenueTarget: Double = 1500000.0,
    val transactionCount: Int = 0
) {
    val grossProfit: Double
        get() = grossRevenue - totalCogs

    val netProfit: Double
        get() = grossProfit - fixedCost

    val netMarginPercentage: Double
        get() = if (grossRevenue > 0) (netProfit / grossRevenue) * 100.0 else 0.0

    val bepProgressPercentage: Double
        get() = if (bepRevenueTarget > 0) ((grossRevenue / bepRevenueTarget) * 100.0).coerceIn(0.0, 100.0) else 0.0

    val bepRemainingRevenue: Double
        get() = (bepRevenueTarget - grossRevenue).coerceAtLeast(0.0)

    val isBepAchieved: Boolean
        get() = grossRevenue >= bepRevenueTarget

    val averageOrderValue: Double
        get() = if (transactionCount > 0) grossRevenue / transactionCount else 0.0
}
