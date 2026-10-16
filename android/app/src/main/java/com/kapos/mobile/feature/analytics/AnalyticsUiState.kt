package com.kapos.mobile.feature.analytics

import com.kapos.mobile.domain.model.AnalyticsPeriod
import com.kapos.mobile.domain.model.FinancialSummary

data class AnalyticsUiState(
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.TODAY,
    val summary: FinancialSummary = FinancialSummary(),
    val cashTotal: Double = 0.0,
    val nonCashTotal: Double = 0.0,
    val isLoading: Boolean = false
)
