package com.kapos.mobile.feature.settings

data class SettingsUiState(
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val storeName: String = "Taichan & Chicken KA",
    val storeAddress: String = "Bandar Lampung",
    val cashierName: String = "Kasir Utama",
    val receiptFooter: String = "Terima kasih atas kunjungannya! 🍢",
    val isTestingConnection: Boolean = false,
    val connectionStatusMessage: String? = null,
    val isConnectionSuccess: Boolean? = null,
    val isSyncing: Boolean = false,
    val pendingSyncCount: Int = 0
)
