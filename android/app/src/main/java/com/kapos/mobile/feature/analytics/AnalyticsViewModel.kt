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
import com.kapos.mobile.domain.model.OrderItem
import com.kapos.mobile.domain.model.Product
import com.kapos.mobile.domain.model.calculatePaymentSplit
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
    private var allOrderItems: List<OrderItem> = emptyList()
    private var allProducts: List<Product> = emptyList()

    init {
        posRepository?.let { repo ->
            scope.launch {
                repo.observeOrders().collect { orders ->
                    allOrders = orders
                    recalculateMetrics()
                }
            }
            scope.launch {
                repo.observeAllOrderItems().collect { items ->
                    allOrderItems = items
                    recalculateMetrics()
                }
            }
            scope.launch {
                repo.observeAllProducts(includeInactive = true).collect { prods ->
                    allProducts = prods
                    recalculateMetrics()
                }
            }
        }
    }

    fun onPeriodChange(period: AnalyticsPeriod) {
        val preset = period.toDateFilterPreset()
        _uiState.value = _uiState.value.copy(
            selectedPeriod = period,
            dateFilter = _uiState.value.dateFilter.copy(preset = preset)
        )
        recalculateMetrics()
    }

    fun onDatePresetChange(preset: DateFilterPreset) {
        val period = AnalyticsPeriod.fromDateFilterPreset(preset) ?: _uiState.value.selectedPeriod
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

        // Kalkulasi HPP: Gunakan data riil costPrice dari Room database jika tersedia
        val productCostMap = allProducts.associate { it.id to it.costPrice }
        val periodOrderIds = periodOrders.map { it.id }.toSet()
        val periodItems = allOrderItems.filter { it.orderId in periodOrderIds }

        val actualCogs = if (periodItems.isNotEmpty()) {
            periodItems.sumOf { item ->
                val unitCost = productCostMap[item.productId] ?: (item.price * 0.55)
                unitCost * item.quantity
            }
        } else {
            // Estimasi HPP bahan fallback: 55% jika order items belum tercatat atau mode test
            grossRev * 0.55
        }

        val fixedCost = settings.dailyFixedCost * days
        val bepTarget = settings.dailyRevenueTarget * days

        val paymentSplit = periodOrders.calculatePaymentSplit()

        val summary = FinancialSummary(
            grossRevenue = grossRev,
            totalCogs = actualCogs,
            fixedCost = fixedCost,
            bepRevenueTarget = bepTarget,
            transactionCount = periodOrders.size
        )

        _uiState.value = _uiState.value.copy(
            summary = summary,
            cashTotal = paymentSplit.cashTotal,
            nonCashTotal = paymentSplit.nonCashTotal
        )
    }
}
