package com.kapos.mobile.feature.analytics

import androidx.lifecycle.ViewModel
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.core.data.repository.SettingsRepository
import com.kapos.mobile.domain.model.AnalyticsPeriod
import com.kapos.mobile.domain.model.DateFilterPreset
import com.kapos.mobile.domain.model.DateRangeFilter
import com.kapos.mobile.domain.model.FinancialSummary
import com.kapos.mobile.domain.model.Order
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.PaymentStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnalyticsViewModel(
    private val posRepository: PosRepository? = null,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var allOrders: List<Order> = emptyList()

    init {
        posRepository?.let { repo ->
            scope.launch {
                repo.observeOrders().collect { orders ->
                    allOrders = orders
                    recalculateMetrics()
                }
            }
        }
    }

    fun onPeriodChange(period: AnalyticsPeriod) {
        val preset = when (period) {
            AnalyticsPeriod.TODAY -> DateFilterPreset.TODAY
            AnalyticsPeriod.WEEK -> DateFilterPreset.LAST_7_DAYS
            AnalyticsPeriod.MONTH -> DateFilterPreset.THIS_MONTH
        }
        _uiState.value = _uiState.value.copy(
            selectedPeriod = period,
            dateFilter = _uiState.value.dateFilter.copy(preset = preset)
        )
        recalculateMetrics()
    }

    fun onDatePresetChange(preset: DateFilterPreset) {
        val period = when (preset) {
            DateFilterPreset.TODAY -> AnalyticsPeriod.TODAY
            DateFilterPreset.LAST_7_DAYS -> AnalyticsPeriod.WEEK
            DateFilterPreset.THIS_MONTH -> AnalyticsPeriod.MONTH
            else -> _uiState.value.selectedPeriod
        }
        _uiState.value = _uiState.value.copy(
            selectedPeriod = period,
            dateFilter = _uiState.value.dateFilter.copy(preset = preset)
        )
        recalculateMetrics()
    }

    fun onCustomDateRangeSelected(startMillis: Long, endMillis: Long) {
        _uiState.value = _uiState.value.copy(
            dateFilter = _uiState.value.dateFilter.copy(
                preset = DateFilterPreset.CUSTOM,
                customStartMillis = startMillis,
                customEndMillis = endMillis
            ),
            showDatePickerDialog = false
        )
        recalculateMetrics()
    }

    fun onShowDatePickerDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDatePickerDialog = show)
    }

    fun setOrdersForTest(orders: List<Order>) {
        allOrders = orders
        recalculateMetrics()
    }

    private fun recalculateMetrics() {
        val dateFilter = _uiState.value.dateFilter
        val (startCutoff, endCutoff) = dateFilter.getTimeRange()

        val days = when (dateFilter.preset) {
            DateFilterPreset.TODAY -> 1
            DateFilterPreset.LAST_7_DAYS -> 7
            DateFilterPreset.THIS_MONTH -> 30
            DateFilterPreset.ALL -> 30
            DateFilterPreset.CUSTOM -> {
                val diffDays = (((endCutoff - startCutoff) + 1000) / (24L * 60 * 60 * 1000)).toInt()
                maxOf(1, diffDays)
            }
        }

        val periodOrders = allOrders.filter { it.createdAt in startCutoff..endCutoff && it.paymentStatus == PaymentStatus.PAID }
        val grossRev = periodOrders.sumOf { it.total }
        val settings = settingsRepository.settingsFlow.value

        // Estimasi HPP bahan: 55% dari Omzet jika detail BOM belum diinput kasir
        val estimatedCogs = grossRev * 0.55
        val fixedCost = settings.dailyFixedCost * days
        val bepTarget = settings.dailyRevenueTarget * days

        val cash = periodOrders.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.total }
        val nonCash = periodOrders.filter { it.paymentMethod != PaymentMethod.CASH }.sumOf { it.total }

        val summary = FinancialSummary(
            grossRevenue = grossRev,
            totalCogs = estimatedCogs,
            fixedCost = fixedCost,
            bepRevenueTarget = bepTarget,
            transactionCount = periodOrders.size
        )

        _uiState.value = _uiState.value.copy(
            summary = summary,
            cashTotal = cash,
            nonCashTotal = nonCash
        )
    }
}
