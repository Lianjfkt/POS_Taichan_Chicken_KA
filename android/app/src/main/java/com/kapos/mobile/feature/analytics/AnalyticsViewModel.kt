package com.kapos.mobile.feature.analytics

import androidx.lifecycle.ViewModel
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.core.data.repository.SettingsRepository
import com.kapos.mobile.domain.model.AnalyticsPeriod
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
import java.util.Calendar

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
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        recalculateMetrics()
    }

    fun setOrdersForTest(orders: List<Order>) {
        allOrders = orders
        recalculateMetrics()
    }

    private fun recalculateMetrics() {
        val period = _uiState.value.selectedPeriod
        val now = System.currentTimeMillis()
        val cutoff = when (period) {
            AnalyticsPeriod.TODAY -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }
            AnalyticsPeriod.WEEK -> now - 7L * 24 * 60 * 60 * 1000
            AnalyticsPeriod.MONTH -> now - 30L * 24 * 60 * 60 * 1000
        }

        val periodOrders = allOrders.filter { it.createdAt >= cutoff && it.paymentStatus == PaymentStatus.PAID }
        val grossRev = periodOrders.sumOf { it.total }
        val settings = settingsRepository.settingsFlow.value

        // Estimasi HPP bahan: 55% dari Omzet jika detail BOM belum diinput kasir
        val estimatedCogs = grossRev * 0.55
        val fixedCost = settings.dailyFixedCost * period.days
        val bepTarget = settings.dailyRevenueTarget * period.days

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
