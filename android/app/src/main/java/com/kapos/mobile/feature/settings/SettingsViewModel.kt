package com.kapos.mobile.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapos.mobile.core.data.local.AppDatabase
import com.kapos.mobile.core.data.remote.SupabaseClient
import com.kapos.mobile.core.data.repository.AppSettings
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val supabaseClient: SupabaseClient,
    private val posRepository: PosRepository,
    private val bluetoothPrinterService: com.kapos.mobile.core.printer.BluetoothPrinterService,
    database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        supabaseUrl = settings.supabaseUrl,
                        supabaseAnonKey = settings.supabaseAnonKey,
                        storeName = settings.storeName,
                        storeAddress = settings.storeAddress,
                        storePhone = settings.storePhone,
                        cashierName = settings.cashierName,
                        receiptFooter = settings.receiptFooter,
                        printerMacAddress = settings.printerMacAddress,
                        printerName = settings.printerName,
                        paperWidth = settings.paperWidth,
                        autoPrintCheckout = settings.autoPrintCheckout,
                        cashierPin = settings.cashierPin,
                        ownerPin = settings.ownerPin,
                        dailyFixedCost = settings.dailyFixedCost,
                        dailyRevenueTarget = settings.dailyRevenueTarget,
                        staffList = settingsRepository.getStaffNames()
                    )
                }
            }
        }
        viewModelScope.launch {
            database.orderDao().observePendingCount().collect { count ->
                _uiState.update { it.copy(pendingSyncCount = count) }
            }
        }
        viewModelScope.launch {
            posRepository.isSyncing.collect { syncing ->
                _uiState.update { it.copy(isSyncing = syncing) }
            }
        }
        loadPairedPrinters()
    }

    fun loadPairedPrinters() {
        val paired = bluetoothPrinterService.getPairedDevices()
        _uiState.update { it.copy(pairedDevices = paired) }
    }

    fun onSelectPrinter(name: String, address: String) {
        _uiState.update { it.copy(printerName = name, printerMacAddress = address) }
        saveSettings()
    }

    fun onPaperWidthChange(width: Int) {
        _uiState.update { it.copy(paperWidth = width) }
        saveSettings()
    }

    fun onAutoPrintChange(enabled: Boolean) {
        _uiState.update { it.copy(autoPrintCheckout = enabled) }
        saveSettings()
    }

    fun onUrlChange(url: String) {
        _uiState.update { it.copy(supabaseUrl = url) }
    }

    fun onAnonKeyChange(key: String) {
        _uiState.update { it.copy(supabaseAnonKey = key) }
    }

    fun onStoreNameChange(name: String) {
        _uiState.update { it.copy(storeName = name) }
    }

    fun onStoreAddressChange(address: String) {
        _uiState.update { it.copy(storeAddress = address) }
    }

    fun onStorePhoneChange(phone: String) {
        _uiState.update { it.copy(storePhone = phone) }
    }

    fun onCashierNameChange(name: String) {
        _uiState.update { it.copy(cashierName = name) }
    }

    fun onReceiptFooterChange(footer: String) {
        _uiState.update { it.copy(receiptFooter = footer) }
    }

    fun onCashierPinChange(pin: String) {
        _uiState.update { it.copy(cashierPin = pin) }
    }

    fun onOwnerPinChange(pin: String) {
        _uiState.update { it.copy(ownerPin = pin) }
    }

    fun onDailyFixedCostChange(cost: Double) {
        _uiState.update { it.copy(dailyFixedCost = cost) }
    }

    fun onDailyRevenueTargetChange(target: Double) {
        _uiState.update { it.copy(dailyRevenueTarget = target) }
    }

    fun saveSettings() {
        val state = _uiState.value
        settingsRepository.saveSettings(
            AppSettings(
                supabaseUrl = state.supabaseUrl,
                supabaseAnonKey = state.supabaseAnonKey,
                storeName = state.storeName,
                storeAddress = state.storeAddress,
                storePhone = state.storePhone,
                cashierName = state.cashierName,
                receiptFooter = state.receiptFooter,
                printerMacAddress = state.printerMacAddress,
                printerName = state.printerName,
                paperWidth = state.paperWidth,
                autoPrintCheckout = state.autoPrintCheckout,
                cashierPin = state.cashierPin,
                ownerPin = state.ownerPin,
                dailyFixedCost = state.dailyFixedCost,
                dailyRevenueTarget = state.dailyRevenueTarget
            )
        )
    }

    fun testPrintReceipt() {
        val state = _uiState.value
        if (state.printerMacAddress.isBlank()) {
            _uiState.update {
                it.copy(
                    isTestingPrint = false,
                    isPrintTestSuccess = false,
                    printTestStatusMessage = "Silakan pilih printer Bluetooth terlebih dahulu."
                )
            }
            return
        }

        _uiState.update { it.copy(isTestingPrint = true, printTestStatusMessage = null) }
        viewModelScope.launch {
            val builder = com.kapos.mobile.core.printer.EscPosBuilder(state.paperWidth)
            val sampleItems = listOf(
                com.kapos.mobile.core.printer.EscPosItem(
                    name = "Sate Taichan Paha (10 tsk)",
                    price = 28000.0,
                    quantity = 1,
                    spiceLevel = "Lv. 3",
                    notes = "Sambal dipisah",
                    subtotal = 28000.0
                ),
                com.kapos.mobile.core.printer.EscPosItem(
                    name = "Es Jeruk Peras",
                    price = 10000.0,
                    quantity = 1,
                    spiceLevel = "",
                    notes = "Sedikit gula",
                    subtotal = 10000.0
                )
            )

            val receiptBytes = builder.buildReceipt(
                storeName = state.storeName,
                address = state.storeAddress,
                phone = state.storePhone,
                receiptFooter = state.receiptFooter,
                orderNo = "KA-TEST-PRINT",
                dateStr = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("id", "ID")).format(java.util.Date()),
                orderType = "Dine-In",
                tableNo = "01",
                cashierName = state.cashierName,
                customerName = "Tes Printer",
                items = sampleItems,
                subtotal = 38000.0,
                discount = 0.0,
                total = 38000.0,
                paymentMethod = "Tunai",
                cashGiven = 50000.0,
                change = 12000.0
            )

            val result = bluetoothPrinterService.printBytes(state.printerMacAddress, receiptBytes)
            _uiState.update {
                it.copy(
                    isTestingPrint = false,
                    isPrintTestSuccess = result.isSuccess,
                    printTestStatusMessage = if (result.isSuccess) "Struk tes berhasil dikirim ke printer!" else (result.exceptionOrNull()?.message ?: "Gagal mencetak.")
                )
            }
        }
    }

    fun testConnection() {
        saveSettings()
        _uiState.update { it.copy(isTestingConnection = true, connectionStatusMessage = null) }
        viewModelScope.launch {
            val success = supabaseClient.testConnection()
            _uiState.update {
                it.copy(
                    isTestingConnection = false,
                    isConnectionSuccess = success,
                    connectionStatusMessage = if (success) "Berhasil terhubung ke Supabase Cloud!" else "Gagal terhubung. Cek URL & Anon Key."
                )
            }
        }
    }

    fun syncNow() {
        saveSettings()
        viewModelScope.launch {
            posRepository.refreshFromSupabase()
        }
    }

    fun onSelectActiveStaff(staffName: String) {
        settingsRepository.switchActiveCashier(staffName)
        _uiState.update { it.copy(cashierName = staffName) }
    }

    fun onAddStaff(staffName: String) {
        settingsRepository.addStaffName(staffName)
        _uiState.update { it.copy(staffList = settingsRepository.getStaffNames()) }
    }
}
