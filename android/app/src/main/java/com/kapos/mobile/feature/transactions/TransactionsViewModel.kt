package com.kapos.mobile.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.domain.model.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TransactionsViewModel(
    private val repository: PosRepository,
    private val settingsRepository: com.kapos.mobile.core.data.repository.SettingsRepository,
    private val bluetoothPrinterService: com.kapos.mobile.core.printer.BluetoothPrinterService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeOrders().collect { list ->
                _uiState.update { it.copy(orders = list) }
            }
        }
        viewModelScope.launch {
            repository.isSyncing.collect { syncing ->
                _uiState.update { it.copy(isSyncing = syncing) }
            }
        }
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        storeName = settings.storeName,
                        storeAddress = settings.storeAddress
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onFilterChange(filter: TransactionFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun onDatePresetChange(preset: com.kapos.mobile.domain.model.DateFilterPreset) {
        _uiState.update { it.copy(dateFilter = it.dateFilter.copy(preset = preset)) }
    }

    fun onCustomDateRangeSelected(startMillis: Long, endMillis: Long) {
        _uiState.update {
            it.copy(
                dateFilter = it.dateFilter.copy(
                    preset = com.kapos.mobile.domain.model.DateFilterPreset.CUSTOM,
                    customStartMillis = startMillis,
                    customEndMillis = endMillis
                ),
                showDatePickerDialog = false
            )
        }
    }

    fun onShowDatePickerDialog(show: Boolean) {
        _uiState.update { it.copy(showDatePickerDialog = show) }
    }

    fun onSelectOrder(order: Order) {
        _uiState.update { it.copy(selectedOrder = order, isLoadingDetail = true) }
        viewModelScope.launch {
            val items = repository.getOrderItems(order.id)
            _uiState.update { it.copy(selectedOrderItems = items, isLoadingDetail = false) }
        }
    }

    fun onDismissDetail() {
        _uiState.update { it.copy(selectedOrder = null, selectedOrderItems = emptyList()) }
    }

    // --- VOID with PIN & Reason ---
    fun onShowVoidPinDialog(order: Order) {
        _uiState.update {
            it.copy(
                orderToVoid = order,
                showVoidPinDialog = true,
                voidPinInput = "",
                voidPinError = null,
                selectedVoidReason = "Salah Input Menu",
                voidReasonNotes = "",
                voidReasonError = null
            )
        }
    }

    fun onDismissVoidPinDialog() {
        _uiState.update {
            it.copy(
                showVoidPinDialog = false,
                orderToVoid = null,
                voidPinInput = "",
                voidPinError = null,
                selectedVoidReason = "Salah Input Menu",
                voidReasonNotes = "",
                voidReasonError = null
            )
        }
    }

    fun onSelectVoidReason(reason: String) {
        _uiState.update { it.copy(selectedVoidReason = reason, voidReasonError = null) }
    }

    fun onVoidReasonNotesChange(notes: String) {
        _uiState.update { it.copy(voidReasonNotes = notes, voidReasonError = null) }
    }

    fun onVoidPinInputChange(pin: String) {
        if (pin.length <= 6 && pin.all { it.isDigit() }) {
            _uiState.update { it.copy(voidPinInput = pin, voidPinError = null) }
        }
    }

    fun confirmVoid() {
        val state = _uiState.value
        val order = state.orderToVoid ?: return
        val correctPin = settingsRepository.settingsFlow.value.ownerPin

        if (state.selectedVoidReason.isBlank()) {
            _uiState.update { it.copy(voidReasonError = "Silakan pilih alasan pembatalan.") }
            return
        }

        if (state.selectedVoidReason == "Lainnya" && state.voidReasonNotes.isBlank()) {
            _uiState.update { it.copy(voidReasonError = "Tuliskan keterangan untuk alasan 'Lainnya'.") }
            return
        }

        if (state.voidPinInput != correctPin) {
            _uiState.update { it.copy(voidPinError = "PIN Owner salah. Coba lagi.") }
            return
        }

        val fullReason = if (state.voidReasonNotes.isNotBlank()) {
            "${state.selectedVoidReason} (${state.voidReasonNotes.trim()})"
        } else {
            state.selectedVoidReason
        }

        _uiState.update { it.copy(isVoiding = true) }
        viewModelScope.launch {
            repository.voidOrder(order.id, fullReason)
            _uiState.update {
                it.copy(
                    isVoiding = false,
                    showVoidPinDialog = false,
                    orderToVoid = null,
                    voidPinInput = "",
                    voidPinError = null,
                    selectedVoidReason = "Salah Input Menu",
                    voidReasonNotes = "",
                    voidReasonError = null,
                    selectedOrder = null,
                    selectedOrderItems = emptyList()
                )
            }
        }
    }

    fun printOrder(order: Order, items: List<com.kapos.mobile.domain.model.OrderItem>) {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.value
            if (settings.printerMacAddress.isBlank()) {
                _uiState.update { it.copy(printFeedbackMessage = "Printer Bluetooth belum dipilih di Pengaturan.") }
                return@launch
            }

            try {
                val builder = com.kapos.mobile.core.printer.EscPosBuilder(settings.paperWidth)
                val escPosItems = items.map {
                    com.kapos.mobile.core.printer.EscPosItem(
                        name = it.productName,
                        price = it.price,
                        quantity = it.quantity,
                        spiceLevel = it.spiceLevel,
                        notes = it.notes,
                        subtotal = it.subtotal
                    )
                }
                val dateStr = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("id", "ID")).format(java.util.Date(order.createdAt))
                val isVoid = (order.paymentStatus == com.kapos.mobile.domain.model.PaymentStatus.VOID)
                val receiptBytes = builder.buildReceipt(
                    storeName = settings.storeName,
                    address = settings.storeAddress,
                    phone = settings.storePhone,
                    receiptFooter = settings.receiptFooter,
                    orderNo = order.orderNo,
                    dateStr = dateStr,
                    orderType = order.orderType.displayName,
                    tableNo = order.tableNo,
                    cashierName = order.cashierName,
                    customerName = order.customerName,
                    items = escPosItems,
                    subtotal = order.subtotal,
                    discount = order.discount,
                    total = order.total,
                    paymentMethod = order.paymentMethod.displayName,
                    cashGiven = order.total,
                    change = 0.0,
                    isVoid = isVoid
                )
                val result = bluetoothPrinterService.printBytes(settings.printerMacAddress, receiptBytes)
                if (result.isSuccess) {
                    _uiState.update { it.copy(printFeedbackMessage = "Struk berhasil dicetak ke printer!") }
                } else {
                    _uiState.update { it.copy(printFeedbackMessage = "Gagal mencetak: ${result.exceptionOrNull()?.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(printFeedbackMessage = "Gagal mencetak struk: ${e.message}") }
            }
        }
    }

    fun clearPrintFeedback() {
        _uiState.update { it.copy(printFeedbackMessage = null) }
    }

    fun syncPending() {
        viewModelScope.launch {
            repository.syncPendingOrders()
        }
    }
}

