package com.kapos.mobile.feature.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.core.data.repository.SettingsRepository
import com.kapos.mobile.core.data.repository.ShiftRepository
import com.kapos.mobile.core.printer.BluetoothPrinterService
import com.kapos.mobile.core.printer.EscPosBuilder
import com.kapos.mobile.core.utils.ShiftShareFormatter
import com.kapos.mobile.domain.model.CashMovementType
import com.kapos.mobile.domain.model.Order
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.PaymentStatus
import com.kapos.mobile.domain.model.calculatePaymentSplit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShiftViewModel(
    private val shiftRepository: ShiftRepository,
    private val settingsRepository: SettingsRepository,
    private val posRepository: PosRepository? = null,
    private val bluetoothPrinterService: BluetoothPrinterService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShiftUiState())
    val uiState: StateFlow<ShiftUiState> = _uiState.asStateFlow()

    private var allOrders: List<Order> = emptyList()

    init {
        // Observe active shift
        viewModelScope.launch {
            shiftRepository.observeActiveShift().collect { shift ->
                _uiState.update { it.copy(activeShift = shift) }
                recalculateShiftSales()
            }
        }

        // Observe cash movements when active shift changes
        viewModelScope.launch {
            shiftRepository.observeActiveShift().collect { shift ->
                if (shift != null) {
                    shiftRepository.observeCashMovements(shift.id).collect { movements ->
                        _uiState.update { it.copy(cashMovements = movements) }
                    }
                } else {
                    _uiState.update { it.copy(cashMovements = emptyList()) }
                }
            }
        }

        // Observe orders for sales calculation
        posRepository?.let { repo ->
            viewModelScope.launch {
                repo.observeOrders().collect { orders ->
                    allOrders = orders
                    recalculateShiftSales()
                }
            }
        }

        // Observe recent shifts history
        viewModelScope.launch {
            shiftRepository.observeAllShifts().collect { shifts ->
                _uiState.update { it.copy(recentShifts = shifts.take(10)) }
            }
        }

        // Observe default cashier name from settings
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(cashierNameInput = settings.cashierName) }
            }
        }
    }

    private fun recalculateShiftSales() {
        val shift = _uiState.value.activeShift
        if (shift == null) {
            _uiState.update { it.copy(cashSales = 0.0, nonCashSales = 0.0) }
            return
        }

        val shiftOrders = allOrders.filter {
            it.createdAt >= shift.openedAt && it.paymentStatus == PaymentStatus.PAID
        }
        val split = shiftOrders.calculatePaymentSplit()

        _uiState.update {
            it.copy(
                cashSales = split.cashTotal,
                nonCashSales = split.nonCashTotal
            )
        }
    }

    // --- Open Shift ---
    fun onShowOpenShiftDialog() {
        _uiState.update {
            it.copy(
                showOpenShiftDialog = true,
                startCashInput = "",
                errorMessage = null
            )
        }
    }

    fun onDismissOpenShiftDialog() {
        _uiState.update { it.copy(showOpenShiftDialog = false) }
    }

    fun onStartCashInputChange(value: String) {
        if (value.all { c -> c.isDigit() || c == '.' }) {
            _uiState.update { it.copy(startCashInput = value) }
        }
    }

    fun onCashierNameInputChange(value: String) {
        _uiState.update { it.copy(cashierNameInput = value) }
    }

    fun openShift() {
        val state = _uiState.value
        val startCash = state.startCashDouble
        val cashierName = state.cashierNameInput.ifBlank {
            settingsRepository.settingsFlow.value.cashierName
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                shiftRepository.openShift(cashierName, startCash)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showOpenShiftDialog = false,
                        successMessage = "Shift berhasil dibuka! Selamat bekerja 💪"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Gagal membuka shift: ${e.message}") }
            }
        }
    }

    // --- Petty Cash Dialog ---
    fun onShowPettyCashDialog(type: CashMovementType) {
        _uiState.update {
            it.copy(
                showPettyCashDialog = true,
                pettyCashType = type,
                pettyCashCategory = if (type == CashMovementType.CASH_OUT) "Operasional" else "Setoran Kas",
                pettyCashAmountInput = "",
                pettyCashNotesInput = "",
                errorMessage = null
            )
        }
    }

    fun onDismissPettyCashDialog() {
        _uiState.update { it.copy(showPettyCashDialog = false) }
    }

    fun onPettyCashCategoryChange(category: String) {
        _uiState.update { it.copy(pettyCashCategory = category) }
    }

    fun onPettyCashAmountChange(amount: String) {
        if (amount.all { it.isDigit() }) {
            _uiState.update { it.copy(pettyCashAmountInput = amount) }
        }
    }

    fun onPettyCashNotesChange(notes: String) {
        _uiState.update { it.copy(pettyCashNotesInput = notes) }
    }

    fun submitPettyCash() {
        val state = _uiState.value
        val shift = state.activeShift ?: return
        val amount = state.pettyCashAmountInput.toDoubleOrNull() ?: 0.0

        if (amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Nominal kas harus lebih dari 0") }
            return
        }

        viewModelScope.launch {
            try {
                shiftRepository.addCashMovement(
                    shiftId = shift.id,
                    type = state.pettyCashType,
                    category = state.pettyCashCategory,
                    amount = amount,
                    notes = state.pettyCashNotesInput
                )
                _uiState.update {
                    it.copy(
                        showPettyCashDialog = false,
                        successMessage = "${state.pettyCashType.displayName} sebesar Rp ${amount.toInt()} berhasil dicatat"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Gagal mencatat kas: ${e.message}") }
            }
        }
    }

    // --- Blind Cash Count & Close Shift ---
    fun onShowBlindCountDialog() {
        _uiState.update {
            it.copy(
                showBlindCountDialog = true,
                count100k = 0,
                count50k = 0,
                count20k = 0,
                count10k = 0,
                count5k = 0,
                count2k = 0,
                count1k = 0,
                countCoin = 0.0,
                errorMessage = null
            )
        }
    }

    fun onDismissBlindCountDialog() {
        _uiState.update { it.copy(showBlindCountDialog = false) }
    }

    fun onDenominationCountChange(denomination: Int, count: Int) {
        val safeCount = count.coerceAtLeast(0)
        _uiState.update {
            when (denomination) {
                100_000 -> it.copy(count100k = safeCount)
                50_000 -> it.copy(count50k = safeCount)
                20_000 -> it.copy(count20k = safeCount)
                10_000 -> it.copy(count10k = safeCount)
                5_000 -> it.copy(count5k = safeCount)
                2_000 -> it.copy(count2k = safeCount)
                1_000 -> it.copy(count1k = safeCount)
                else -> it
            }
        }
    }

    fun onCoinAmountChange(coinStr: String) {
        val coinDouble = coinStr.toDoubleOrNull() ?: 0.0
        _uiState.update { it.copy(countCoin = coinDouble) }
    }

    fun confirmCloseShift() {
        val state = _uiState.value
        val shift = state.activeShift ?: return
        val actualCash = state.actualPhysicalCash
        val now = System.currentTimeMillis()

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val closed = shiftRepository.closeShift(shift, actualCash)
                val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
                val openedStr = dateFormat.format(Date(closed.openedAt))
                val closedStr = dateFormat.format(Date(now))

                val summary = ShiftClosedSummary(
                    cashierName = closed.cashierName,
                    openedAtStr = openedStr,
                    closedAtStr = closedStr,
                    startCash = closed.startCash,
                    cashSales = state.cashSales,
                    nonCashSales = state.nonCashSales,
                    cashIn = state.cashInTotal,
                    cashOut = state.cashOutTotal,
                    expectedCash = state.expectedCash,
                    actualCash = actualCash,
                    selisih = state.selisih
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showBlindCountDialog = false,
                        showShiftReportDialog = true,
                        lastClosedSummary = summary,
                        successMessage = "Shift kasir berhasil ditutup! 🎉"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Gagal menutup shift: ${e.message}") }
            }
        }
    }

    fun onDismissShiftReportDialog() {
        _uiState.update { it.copy(showShiftReportDialog = false, lastClosedSummary = null) }
    }

    fun printShiftReceipt() {
        val summary = _uiState.value.lastClosedSummary ?: return
        val printer = bluetoothPrinterService ?: return
        val settings = settingsRepository.settingsFlow.value
        if (settings.printerMacAddress.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Printer Bluetooth belum dipilih di Pengaturan.") }
            return
        }

        _uiState.update { it.copy(isPrinting = true) }
        viewModelScope.launch {
            try {
                val builder = EscPosBuilder(settings.paperWidth)
                val bytes = builder.buildShiftReceipt(
                    storeName = settings.storeName,
                    cashierName = summary.cashierName,
                    openedAtStr = summary.openedAtStr,
                    closedAtStr = summary.closedAtStr,
                    startCash = summary.startCash,
                    cashSales = summary.cashSales,
                    nonCashSales = summary.nonCashSales,
                    cashIn = summary.cashIn,
                    cashOut = summary.cashOut,
                    expectedCash = summary.expectedCash,
                    actualCash = summary.actualCash,
                    selisih = summary.selisih
                )
                printer.printBytes(settings.printerMacAddress, bytes)
                _uiState.update { it.copy(isPrinting = false, successMessage = "Struk rekap shift berhasil dicetak!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isPrinting = false, errorMessage = "Gagal mencetak struk shift: ${e.message}") }
            }
        }
    }

    fun getWhatsAppShareText(): String {
        val summary = _uiState.value.lastClosedSummary ?: return ""
        val settings = settingsRepository.settingsFlow.value
        return ShiftShareFormatter.formatWhatsAppSummary(
            storeName = settings.storeName,
            cashierName = summary.cashierName,
            dateStr = summary.closedAtStr,
            startCash = summary.startCash,
            cashSales = summary.cashSales,
            nonCashSales = summary.nonCashSales,
            cashIn = summary.cashIn,
            cashOut = summary.cashOut,
            expectedCash = summary.expectedCash,
            actualCash = summary.actualCash,
            selisih = summary.selisih
        )
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
