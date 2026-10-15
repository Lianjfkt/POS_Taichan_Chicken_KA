package com.kapos.mobile.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
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
    val autoPrintCheckout: Boolean = true
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ka_pos_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            supabaseUrl = prefs.getString("supabase_url", "") ?: "",
            supabaseAnonKey = prefs.getString("supabase_anon_key", "") ?: "",
            storeName = prefs.getString("store_name", "Taichan & Chicken KA") ?: "Taichan & Chicken KA",
            storeAddress = prefs.getString("store_address", "Bandar Lampung") ?: "Bandar Lampung",
            storePhone = prefs.getString("store_phone", "081234567890") ?: "081234567890",
            cashierName = prefs.getString("cashier_name", "Kasir Utama") ?: "Kasir Utama",
            receiptFooter = prefs.getString("receipt_footer", "Terima kasih atas kunjungannya! 🍢") ?: "Terima kasih atas kunjungannya! 🍢",
            printerMacAddress = prefs.getString("printer_mac_address", "") ?: "",
            printerName = prefs.getString("printer_name", "") ?: "",
            paperWidth = prefs.getInt("paper_width", 58),
            autoPrintCheckout = prefs.getBoolean("auto_print_checkout", true)
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString("supabase_url", settings.supabaseUrl)
            .putString("supabase_anon_key", settings.supabaseAnonKey)
            .putString("store_name", settings.storeName)
            .putString("store_address", settings.storeAddress)
            .putString("store_phone", settings.storePhone)
            .putString("cashier_name", settings.cashierName)
            .putString("receipt_footer", settings.receiptFooter)
            .putString("printer_mac_address", settings.printerMacAddress)
            .putString("printer_name", settings.printerName)
            .putInt("paper_width", settings.paperWidth)
            .putBoolean("auto_print_checkout", settings.autoPrintCheckout)
            .apply()
        _settingsFlow.value = settings
    }
}
