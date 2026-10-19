package com.kapos.mobile.feature.analytics

import com.kapos.mobile.domain.model.AnalyticsPeriod
import com.kapos.mobile.domain.model.DateFilterPreset
import com.kapos.mobile.domain.model.DateRangeFilter
import com.kapos.mobile.domain.model.FinancialSummary

data class AnalyticsUiState(
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.TODAY,
    val dateFilter: DateRangeFilter = DateRangeFilter(preset = DateFilterPreset.TODAY),
    val showDatePickerDialog: Boolean = false,
    val summary: FinancialSummary = FinancialSummary(),
    val cashTotal: Double = 0.0,
    val nonCashTotal: Double = 0.0,
    val isLoading: Boolean = false
)
