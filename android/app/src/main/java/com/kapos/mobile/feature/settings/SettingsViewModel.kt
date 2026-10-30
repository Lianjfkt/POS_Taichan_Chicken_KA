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
    private val database: AppDatabase,
    private val stockRepository: com.kapos.mobile.core.data.repository.StockRepository? = null,
    private val savedOrderRepository: com.kapos.mobile.core.data.repository.SavedOrderRepository? = null
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
                        dailyFixedCost = settings.dailyFixedCost,
                        dailyRevenueTarget = settings.dailyRevenueTarget,
                        staffList = settingsRepository.getStaffNames(),
                        autoLockMinutes = settings.autoLockMinutes,
                        autoSyncIntervalMinutes = settings.autoSyncIntervalMinutes
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
        viewModelScope.launch {
            posRepository.lastSyncTime.collect { ts ->
                _uiState.update { it.copy(lastSyncTime = ts) }
            }
        }
        loadPairedPrinters()
    }

    fun loadPairedPrinters() {
        val hasPerm = bluetoothPrinterService.hasBluetoothPermission()
        val paired = if (hasPerm) bluetoothPrinterService.getPairedDevices() else emptyList()
        _uiState.update { it.copy(pairedDevices = paired, hasBluetoothPermission = hasPerm) }
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
        if (pin.length <= 6 && pin.all { it.isDigit() }) {
            _uiState.update { it.copy(cashierPin = pin) }
        }
    }

    fun onOwnerPinChange(pin: String) {
        if (pin.length <= 6 && pin.all { it.isDigit() }) {
            _uiState.update { it.copy(ownerPin = pin) }
        }
    }

    fun onDailyFixedCostChange(cost: Double) {
        _uiState.update { it.copy(dailyFixedCost = cost) }
    }

    fun onDailyRevenueTargetChange(target: Double) {
        _uiState.update { it.copy(dailyRevenueTarget = target) }
    }

    fun saveSettings() {
        val state = _uiState.value
        val current = settingsRepository.settingsFlow.value
        val finalCashierPin = if (state.cashierPin.isNotBlank() && state.cashierPin.length >= 4) {
            state.cashierPin
        } else {
            current.cashierPin
        }
        val finalOwnerPin = if (state.ownerPin.isNotBlank() && state.ownerPin.length >= 4) {
            state.ownerPin
        } else {
            current.ownerPin
        }

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
                cashierPin = finalCashierPin,
                ownerPin = finalOwnerPin,
                dailyFixedCost = state.dailyFixedCost,
                dailyRevenueTarget = state.dailyRevenueTarget,
                autoLockMinutes = state.autoLockMinutes,
                autoSyncIntervalMinutes = state.autoSyncIntervalMinutes
            )
        )
        // Reset local pin input buffers after save
        _uiState.update { it.copy(cashierPin = "", ownerPin = "") }
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
        _uiState.update { it.copy(syncResultMessage = null, isSyncSuccess = null) }
        viewModelScope.launch {
            val success = posRepository.syncAll()
            _uiState.update {
                it.copy(
                    isSyncSuccess = success,
                    syncResultMessage = if (success)
                        "Sinkronisasi berhasil! Data lokal & cloud sudah sinkron."
                    else
                        "Sinkronisasi gagal. Cek koneksi internet atau konfigurasi Supabase."
                )
            }
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

    private val backupManager = com.kapos.mobile.core.data.backup.DatabaseBackupManager(database)

    fun onAutoLockMinutesChange(minutes: Int) {
        _uiState.update { it.copy(autoLockMinutes = minutes) }
        saveSettings()
    }

    fun onAutoSyncIntervalChange(minutes: Int) {
        _uiState.update { it.copy(autoSyncIntervalMinutes = minutes) }
        saveSettings()
    }

    fun exportDatabaseBackup(context: android.content.Context) {
        _uiState.update { it.copy(isExportingBackup = true, backupStatusMessage = null) }
        viewModelScope.launch {
            try {
                val file = backupManager.exportToJson(context)
                _uiState.update {
                    it.copy(
                        isExportingBackup = false,
                        isBackupSuccess = true,
                        backupExportPath = file.absolutePath,
                        backupStatusMessage = "Cadangan tersimpan: ${file.name} (${file.length() / 1024} KB)"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExportingBackup = false,
                        isBackupSuccess = false,
                        backupStatusMessage = "Gagal ekspor: ${e.message}"
                    )
                }
            }
        }
    }

    fun importDatabaseBackup(jsonString: String) {
        _uiState.update { it.copy(isExportingBackup = true, backupStatusMessage = null) }
        viewModelScope.launch {
            val result = backupManager.importFromJson(jsonString)
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _uiState.update {
                    it.copy(
                        isExportingBackup = false,
                        isBackupSuccess = true,
                        backupStatusMessage = "Berhasil memulihkan $count data dari cadangan!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isExportingBackup = false,
                        isBackupSuccess = false,
                        backupStatusMessage = "Gagal impor: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun resetDatabase(ownerPin: String, onComplete: (Boolean) -> Unit = {}) {
        if (!settingsRepository.verifyPin(com.kapos.mobile.domain.model.UserRole.OWNER, ownerPin)) {
            _uiState.update {
                it.copy(
                    isResettingDatabase = false,
                    isResetSuccess = false,
                    resetDatabaseStatusMessage = "PIN Owner salah! Reset database dibatalkan."
                )
            }
            onComplete(false)
            return
        }

        _uiState.update { it.copy(isResettingDatabase = true, resetDatabaseStatusMessage = null) }
        viewModelScope.launch {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    database.clearAllTables()
                    stockRepository?.clearAllBatches()
                    savedOrderRepository?.clearAll()
                }
                _uiState.update {
                    it.copy(
                        isResettingDatabase = false,
                        isResetSuccess = true,
                        resetDatabaseStatusMessage = "Database berhasil dikosongkan total! Seluruh data transaksi, menu & bahan telah bersih."
                    )
                }
                onComplete(true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isResettingDatabase = false,
                        isResetSuccess = false,
                        resetDatabaseStatusMessage = "Gagal mengosongkan database: ${e.message}"
                    )
                }
                onComplete(false)
            }
        }
    }
}
