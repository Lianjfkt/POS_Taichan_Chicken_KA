package com.kapos.mobile.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.kapos.mobile.core.config.AppConfig
import com.kapos.mobile.domain.model.AuthSession
import com.kapos.mobile.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val supabaseUrl: String = AppConfig.DEFAULT_SUPABASE_URL,
    val supabaseAnonKey: String = AppConfig.DEFAULT_SUPABASE_ANON_KEY,
    val storeName: String = "Taichan & Chicken KA",
    val storeAddress: String = "Bandar Lampung",
    val storePhone: String = "081234567890",
    val cashierName: String = "Kasir Utama",
    val receiptFooter: String = "Terima kasih atas kunjungannya!",
    val printerMacAddress: String = "",
    val printerName: String = "",
    val paperWidth: Int = 58,
    val autoPrintCheckout: Boolean = true,
    val cashierPin: String = "1111",
    val ownerPin: String = "1234",
    val dailyFixedCost: Double = 350000.0,
    val dailyRevenueTarget: Double = 1500000.0,
    val staffList: String = "Kasir Utama,Kasir Pagi,Kasir Malam,Manager",
    val autoLockMinutes: Int = 2,
    val autoSyncIntervalMinutes: Int = 5,
    val currentSession: AuthSession = AuthSession(
        isLoggedIn = false,
        role = UserRole.CASHIER,
        cashierName = "Kasir Utama"
    )
)

class SettingsRepository(context: Context? = null) {
    private val prefs: SharedPreferences? = context?.getSharedPreferences("ka_pos_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): AppSettings {
        if (prefs == null) {
            return AppSettings()
        }
        val isSessionActive = prefs.getBoolean("session_is_logged_in", false)
        val sessionRoleStr = prefs.getString("session_role", UserRole.CASHIER.name) ?: UserRole.CASHIER.name
        val sessionRole = try { UserRole.valueOf(sessionRoleStr) } catch (e: Exception) { UserRole.CASHIER }
        val sessionCashierName = prefs.getString("session_cashier_name", "Kasir Utama") ?: "Kasir Utama"

        val storedUrl = prefs.getString("supabase_url", null)
        val storedKey = prefs.getString("supabase_anon_key", null)
        val finalUrl = if (storedUrl.isNullOrBlank()) AppConfig.DEFAULT_SUPABASE_URL else storedUrl
        val finalKey = if (storedKey.isNullOrBlank()) AppConfig.DEFAULT_SUPABASE_ANON_KEY else storedKey

        return AppSettings(
            supabaseUrl = finalUrl,
            supabaseAnonKey = finalKey,
            storeName = prefs.getString("store_name", "Taichan & Chicken KA") ?: "Taichan & Chicken KA",
            storeAddress = prefs.getString("store_address", "Bandar Lampung") ?: "Bandar Lampung",
            storePhone = prefs.getString("store_phone", "081234567890") ?: "081234567890",
            cashierName = prefs.getString("cashier_name", "Kasir Utama") ?: "Kasir Utama",
            receiptFooter = prefs.getString("receipt_footer", "Terima kasih atas kunjungannya!") ?: "Terima kasih atas kunjungannya!",
            printerMacAddress = prefs.getString("printer_mac_address", "") ?: "",
            printerName = prefs.getString("printer_name", "") ?: "",
            paperWidth = prefs.getInt("paper_width", 58),
            autoPrintCheckout = prefs.getBoolean("auto_print_checkout", true),
            cashierPin = prefs.getString("cashier_pin", "1111") ?: "1111",
            ownerPin = prefs.getString("owner_pin", "1234") ?: "1234",
            dailyFixedCost = prefs.getFloat("daily_fixed_cost", 350000f).toDouble(),
            dailyRevenueTarget = prefs.getFloat("daily_revenue_target", 1500000f).toDouble(),
            staffList = prefs.getString("staff_list", "Kasir Utama,Kasir Pagi,Kasir Malam,Manager") ?: "Kasir Utama,Kasir Pagi,Kasir Malam,Manager",
            autoLockMinutes = prefs.getInt("auto_lock_minutes", 2),
            autoSyncIntervalMinutes = prefs.getInt("auto_sync_interval_minutes", 5),
            currentSession = AuthSession(
                isLoggedIn = isSessionActive,
                role = sessionRole,
                cashierName = sessionCashierName
            )
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs?.edit()
            ?.putString("supabase_url", settings.supabaseUrl)
            ?.putString("supabase_anon_key", settings.supabaseAnonKey)
            ?.putString("store_name", settings.storeName)
            ?.putString("store_address", settings.storeAddress)
            ?.putString("store_phone", settings.storePhone)
            ?.putString("cashier_name", settings.cashierName)
            ?.putString("receipt_footer", settings.receiptFooter)
            ?.putString("printer_mac_address", settings.printerMacAddress)
            ?.putString("printer_name", settings.printerName)
            ?.putInt("paper_width", settings.paperWidth)
            ?.putBoolean("auto_print_checkout", settings.autoPrintCheckout)
            ?.putString("cashier_pin", settings.cashierPin)
            ?.putString("owner_pin", settings.ownerPin)
            ?.putFloat("daily_fixed_cost", settings.dailyFixedCost.toFloat())
            ?.putFloat("daily_revenue_target", settings.dailyRevenueTarget.toFloat())
            ?.putString("staff_list", settings.staffList)
            ?.putInt("auto_lock_minutes", settings.autoLockMinutes)
            ?.putInt("auto_sync_interval_minutes", settings.autoSyncIntervalMinutes)
            ?.apply()
        _settingsFlow.value = settings
    }

    fun getStaffNames(): List<String> {
        return _settingsFlow.value.staffList.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    fun switchActiveCashier(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val current = _settingsFlow.value
        val updated = current.copy(
            cashierName = trimmed,
            currentSession = current.currentSession.copy(cashierName = trimmed)
        )
        saveSettings(updated)
        prefs?.edit()?.putString("session_cashier_name", trimmed)?.apply()
    }

    fun addStaffName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val currentList = getStaffNames().toMutableList()
        if (!currentList.contains(trimmed)) {
            currentList.add(trimmed)
            val updatedString = currentList.joinToString(",")
            val updated = _settingsFlow.value.copy(staffList = updatedString)
            saveSettings(updated)
        }
    }

    fun updateSession(session: AuthSession) {
        prefs?.edit()
            ?.putBoolean("session_is_logged_in", session.isLoggedIn)
            ?.putString("session_role", session.role.name)
            ?.putString("session_cashier_name", session.cashierName)
            ?.apply()
        _settingsFlow.value = _settingsFlow.value.copy(currentSession = session)
    }

    fun logout() {
        updateSession(
            AuthSession(
                isLoggedIn = false,
                role = UserRole.CASHIER,
                cashierName = _settingsFlow.value.cashierName
            )
        )
    }

    fun verifyPin(role: UserRole, pin: String): Boolean {
        val current = _settingsFlow.value
        return when (role) {
            UserRole.CASHIER -> pin == current.cashierPin
            UserRole.OWNER -> pin == current.ownerPin
        }
    }
}
