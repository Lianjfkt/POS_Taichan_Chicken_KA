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
    val cashierName: String = "Kasir Utama",
    val receiptFooter: String = "Terima kasih atas kunjungannya! 🍢"
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
            cashierName = prefs.getString("cashier_name", "Kasir Utama") ?: "Kasir Utama",
            receiptFooter = prefs.getString("receipt_footer", "Terima kasih atas kunjungannya! 🍢") ?: "Terima kasih atas kunjungannya! 🍢"
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString("supabase_url", settings.supabaseUrl)
            .putString("supabase_anon_key", settings.supabaseAnonKey)
            .putString("store_name", settings.storeName)
            .putString("store_address", settings.storeAddress)
            .putString("cashier_name", settings.cashierName)
            .putString("receipt_footer", settings.receiptFooter)
            .apply()
        _settingsFlow.value = settings
    }
}
