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
                        cashierName = settings.cashierName,
                        receiptFooter = settings.receiptFooter
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
            cashierName = state.cashierName,
            receiptFooter = state.receiptFooter
        )
        settingsRepository.saveSettings(settings)
        supabaseClient.updateConfig(settings.supabaseUrl, settings.supabaseAnonKey)
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
