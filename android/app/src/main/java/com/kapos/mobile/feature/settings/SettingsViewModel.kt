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
                        autoPrintCheckout = settings.autoPrintCheckout
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

    fun saveSettings() {
        val state = _uiState.value
        val settings = AppSettings(
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
            autoPrintCheckout = state.autoPrintCheckout
        )
        settingsRepository.saveSettings(settings)
        supabaseClient.updateConfig(settings.supabaseUrl, settings.supabaseAnonKey)
    }

    fun testPrintReceipt() {
        saveSettings()
        val state = _uiState.value
        if (state.printerMacAddress.isBlank()) {
            _uiState.update {
                it.copy(
                    isPrintTestSuccess = false,
                    printTestStatusMessage = "Pilih printer Bluetooth terlebih dahulu dari daftar."
                )
            }
            return
        }

        _uiState.update { it.copy(isTestingPrint = true, printTestStatusMessage = null) }
        viewModelScope.launch {
            val builder = com.kapos.mobile.core.printer.EscPosBuilder(state.paperWidth)
            val sampleItems = listOf(
                com.kapos.mobile.core.printer.EscPosItem(
                    name = "Taichan Pedas Lv.3",
                    price = 18000.0,
                    quantity = 2,
                    spiceLevel = "Lv.3",
                    notes = "sambal dipisah",
                    subtotal = 36000.0
                ),
                com.kapos.mobile.core.printer.EscPosItem(
                    name = "Es Teh Manis",
                    price = 5000.0,
                    quantity = 2,
                    spiceLevel = "",
                    notes = "",
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
                subtotal = 46000.0,
                discount = 0.0,
                total = 46000.0,
                paymentMethod = "Tunai",
                cashGiven = 50000.0,
                change = 4000.0
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
}
