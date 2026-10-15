package com.kapos.mobile.feature.settings

data class SettingsUiState(
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val storeName: String = "Taichan & Chicken KA",
    val storeAddress: String = "Bandar Lampung",
    val storePhone: String = "081234567890",
    val cashierName: String = "Kasir Utama",
    val receiptFooter: String = "Terima kasih atas kunjungannya! 🍢",
    val printerMacAddress: String = "",
    val printerName: String = "",
    val paperWidth: Int = 58,
    val autoPrintCheckout: Boolean = true,
    val pairedDevices: List<com.kapos.mobile.core.printer.BluetoothDeviceInfo> = emptyList(),
    val isTestingConnection: Boolean = false,
    val connectionStatusMessage: String? = null,
    val isConnectionSuccess: Boolean? = null,
    val isTestingPrint: Boolean = false,
    val printTestStatusMessage: String? = null,
    val isPrintTestSuccess: Boolean? = null,
    val isSyncing: Boolean = false,
    val pendingSyncCount: Int = 0
)
