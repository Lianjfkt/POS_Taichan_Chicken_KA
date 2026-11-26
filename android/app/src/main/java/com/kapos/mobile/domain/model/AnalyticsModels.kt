package com.kapos.mobile.domain.model

enum class ReportPeriod(val displayName: String, val days: Int) {
    TODAY("Hari Ini", 1),
    WEEK("7 Hari", 7),
    MONTH("Bulan Ini", 30),
    ALL("Semua", 30);

    fun toDateFilterPreset(): DateFilterPreset = when (this) {
        TODAY -> DateFilterPreset.TODAY
        WEEK -> DateFilterPreset.LAST_7_DAYS
        MONTH -> DateFilterPreset.THIS_MONTH
        ALL -> DateFilterPreset.ALL
    }

    companion object {
        fun fromDateFilterPreset(preset: DateFilterPreset): ReportPeriod? = when (preset) {
            DateFilterPreset.TODAY -> TODAY
            DateFilterPreset.LAST_7_DAYS -> WEEK
            DateFilterPreset.THIS_MONTH -> MONTH
            DateFilterPreset.ALL -> ALL
            DateFilterPreset.CUSTOM -> null
        }
    }
}

typealias AnalyticsPeriod = ReportPeriod

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
