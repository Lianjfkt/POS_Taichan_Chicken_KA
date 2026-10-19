package com.kapos.mobile.feature.report

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.core.data.repository.SettingsRepository
import com.kapos.mobile.core.data.repository.ShiftRepository
import com.kapos.mobile.core.printer.BluetoothPrinterService
import com.kapos.mobile.core.printer.EscPosBuilder
import com.kapos.mobile.core.utils.CsvExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportViewModel(
    private val repository: PosRepository,
    private val settingsRepository: SettingsRepository,
    private val shiftRepository: ShiftRepository? = null,
    private val bluetoothPrinterService: BluetoothPrinterService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        // Collect orders
        viewModelScope.launch {
            repository.observeOrders().collect { list ->
                _uiState.update { it.copy(orders = list) }
            }
        }
        // Collect order items
        viewModelScope.launch {
            repository.observeAllOrderItems().collect { items ->
                _uiState.update { it.copy(orderItems = items) }
            }
        }
        // Collect products (for costPrice COGS calculation)
        viewModelScope.launch {
            repository.observeAllProducts(includeInactive = true).collect { prods ->
                _uiState.update { it.copy(products = prods) }
            }
        }
        // Collect cash movements (for operational expense calculation)
        shiftRepository?.let { shiftRepo ->
            viewModelScope.launch {
                shiftRepo.observeAllCashMovements().collect { movements ->
                    _uiState.update { it.copy(cashMovements = movements) }
                }
            }
        }
        // Collect cashier name from settings
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(cashierShiftName = settings.cashierName) }
            }
        }
    }

    fun onPeriodChange(period: ReportPeriod) {
        val preset = when (period) {
            ReportPeriod.TODAY -> com.kapos.mobile.domain.model.DateFilterPreset.TODAY
            ReportPeriod.WEEK -> com.kapos.mobile.domain.model.DateFilterPreset.LAST_7_DAYS
            ReportPeriod.MONTH -> com.kapos.mobile.domain.model.DateFilterPreset.THIS_MONTH
            ReportPeriod.ALL -> com.kapos.mobile.domain.model.DateFilterPreset.ALL
        }
        onDatePresetChange(preset)
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

    fun exportTransactionsCsv(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isExporting = true) }
                val state = _uiState.value
                val label = state.dateFilter.getDisplayLabel()
                val file = CsvExporter.exportOrdersToCsv(
                    context = context,
                    periodName = label,
                    orders = state.periodOrders,
                    orderItems = state.periodOrderItems
                )
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isExporting = false, snackbarMessage = "CSV Transaksi berhasil dibuat") }
                    CsvExporter.shareCsvFile(context, file, "Bagikan Laporan Transaksi ($label)")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isExporting = false, snackbarMessage = "Gagal ekspor CSV: ${e.localizedMessage}") }
                }
            }
        }
    }

    fun exportFinancialSummaryCsv(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isExporting = true) }
                val state = _uiState.value
                val label = state.dateFilter.getDisplayLabel()
                val file = CsvExporter.exportFinancialSummaryToCsv(
                    context = context,
                    periodName = label,
                    cashierName = state.cashierShiftName,
                    grossRevenue = state.grossRevenue,
                    discount = state.totalDiscount,
                    netRevenue = state.netRevenue,
                    cogs = state.totalCogs,
                    grossProfit = state.grossProfit,
                    expenses = state.operationalExpenses,
                    netProfit = state.netProfit,
                    totalTransactions = state.totalTransactionsCount,
                    cashSales = state.cashTotal,
                    nonCashSales = state.nonCashTotal
                )
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isExporting = false, snackbarMessage = "CSV Laba Rugi berhasil dibuat") }
                    CsvExporter.shareCsvFile(context, file, "Bagikan Ringkasan Laba Rugi ($label)")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isExporting = false, snackbarMessage = "Gagal ekspor CSV: ${e.localizedMessage}") }
                }
            }
        }
    }

    fun printFinancialSummaryReceipt() {
        viewModelScope.launch(Dispatchers.IO) {
            val printerService = bluetoothPrinterService
            if (printerService == null) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(snackbarMessage = "Layanan printer Bluetooth tidak tersedia") }
                }
                return@launch
            }

            val settings = settingsRepository.settingsFlow.value
            if (settings.printerMacAddress.isBlank()) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(snackbarMessage = "Printer belum dipilih di Pengaturan") }
                }
                return@launch
            }

            try {
                val state = _uiState.value
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                val builder = EscPosBuilder(paperWidth = settings.paperWidth)
                val bytes = builder.buildFinancialReport(
                    storeName = settings.storeName,
                    periodName = state.dateFilter.getDisplayLabel(),
                    cashierName = state.cashierShiftName,
                    dateStr = dateStr,
                    grossRevenue = state.grossRevenue,
                    discount = state.totalDiscount,
                    netRevenue = state.netRevenue,
                    cogs = state.totalCogs,
                    grossProfit = state.grossProfit,
                    expenses = state.operationalExpenses,
                    netProfit = state.netProfit,
                    totalTransactions = state.totalTransactionsCount,
                    cashSales = state.cashTotal,
                    nonCashSales = state.nonCashTotal
                )

                val result = printerService.printBytes(settings.printerMacAddress, bytes)
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = {
                            _uiState.update { it.copy(snackbarMessage = "Rekap keuangan berhasil dicetak!") }
                        },
                        onFailure = { error ->
                            _uiState.update { it.copy(snackbarMessage = "Gagal mencetak: ${error.localizedMessage}") }
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(snackbarMessage = "Error cetak rekap: ${e.localizedMessage}") }
                }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
