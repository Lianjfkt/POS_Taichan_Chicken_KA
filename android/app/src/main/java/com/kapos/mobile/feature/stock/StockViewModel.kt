package com.kapos.mobile.feature.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.core.data.repository.StockRepository
import com.kapos.mobile.domain.model.ChickenPart
import com.kapos.mobile.domain.model.PaymentStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class StockViewModel(
    private val stockRepository: StockRepository,
    private val posRepository: PosRepository? = null,
    private val scope: kotlinx.coroutines.CoroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
) : ViewModel() {

    private val _activeTab = MutableStateFlow(StockTab.TAICHAN)
    private val _isRestockDialogOpen = MutableStateFlow(false)
    private val _restockChickenPart = MutableStateFlow(ChickenPart.SAYAP)
    private val _taichanSoldToday = MutableStateFlow(0)

    init {
        posRepository?.let { repo ->
            scope.launch {
                repo.observeOrders().collect { orders ->
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val todayStart = cal.timeInMillis

                    val paidTodayOrders = orders.filter { it.createdAt >= todayStart && it.paymentStatus == PaymentStatus.PAID }
                    var totalTaichanTusuk = 0

                    for (order in paidTodayOrders) {
                        val items = repo.getOrderItems(order.id)
                        for (item in items) {
                            if (item.productId.contains("taichan", ignoreCase = true) || item.productName.contains("taichan", ignoreCase = true)) {
                                totalTaichanTusuk += item.quantity * 10
                            }
                        }
                    }
                    _taichanSoldToday.value = totalTaichanTusuk
                }
            }
        }
    }

    val uiState: StateFlow<StockUiState> = combine(
        combine(_activeTab, _isRestockDialogOpen, _restockChickenPart) { tab, dialog, part ->
            Triple(tab, dialog, part)
        },
        stockRepository.taichanBatches,
        stockRepository.chickenBatches,
        _taichanSoldToday
    ) { (tab, isDialogOpen, chickenPart), tcBatches, ckBatches, soldToday ->
        StockUiState(
            activeTab = tab,
            taichanBatches = tcBatches,
            chickenBatches = ckBatches,
            taichanSoldToday = soldToday,
            isRestockDialogOpen = isDialogOpen,
            restockChickenPart = chickenPart
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = StockUiState(
            taichanBatches = stockRepository.taichanBatches.value,
            chickenBatches = stockRepository.chickenBatches.value
        )
    )

    fun onTabSelect(tab: StockTab) {
        _activeTab.value = tab
    }

    fun openRestockDialog(part: ChickenPart = ChickenPart.SAYAP) {
        _restockChickenPart.value = part
        _isRestockDialogOpen.value = true
    }

    fun closeRestockDialog() {
        _isRestockDialogOpen.value = false
    }

    fun addTaichanBatch(raw: Int, notes: String = "") {
        stockRepository.addTaichanBatch(raw, notes)
        closeRestockDialog()
    }

    fun updateTaichanFried(batchId: String, fried: Int) {
        stockRepository.updateTaichanFried(batchId, fried)
    }

    fun deleteTaichanBatch(batchId: String) {
        stockRepository.deleteTaichanBatch(batchId)
    }

    fun addChickenBatch(part: ChickenPart, raw: Int, notes: String = "") {
        stockRepository.addChickenBatch(part, raw, notes)
        closeRestockDialog()
    }

    fun updateChickenFried(batchId: String, fried: Int) {
        stockRepository.updateChickenFried(batchId, fried)
    }

    fun deleteChickenBatch(batchId: String) {
        stockRepository.deleteChickenBatch(batchId)
    }

    fun setTaichanSoldTodayForTest(sold: Int) {
        _taichanSoldToday.value = sold
    }
}
