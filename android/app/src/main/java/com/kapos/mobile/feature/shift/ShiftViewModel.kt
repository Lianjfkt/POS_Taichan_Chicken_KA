package com.kapos.mobile.feature.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapos.mobile.core.data.repository.ShiftRepository
import com.kapos.mobile.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShiftViewModel(
    private val shiftRepository: ShiftRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShiftUiState())
    val uiState: StateFlow<ShiftUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            shiftRepository.observeActiveShift().collect { shift ->
                _uiState.update { it.copy(activeShift = shift) }
            }
        }
        viewModelScope.launch {
            shiftRepository.observeAllShifts().collect { shifts ->
                _uiState.update { it.copy(recentShifts = shifts.take(10)) }
            }
        }
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(cashierNameInput = settings.cashierName) }
            }
        }
    }

    // --- Open Shift ---
    fun onShowOpenShiftDialog() {
        _uiState.update {
            it.copy(
                showOpenShiftDialog = true,
                startCashInput = "",
                errorMessage = null
            )
        }
    }

    fun onDismissOpenShiftDialog() {
        _uiState.update { it.copy(showOpenShiftDialog = false) }
    }

    fun onStartCashInputChange(value: String) {
        if (value.all { c -> c.isDigit() || c == '.' }) {
            _uiState.update { it.copy(startCashInput = value) }
        }
    }

    fun onCashierNameInputChange(value: String) {
        _uiState.update { it.copy(cashierNameInput = value) }
    }

    fun openShift() {
        val state = _uiState.value
        val startCash = state.startCashDouble
        val cashierName = state.cashierNameInput.ifBlank {
            settingsRepository.settingsFlow.value.cashierName
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                shiftRepository.openShift(cashierName, startCash)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showOpenShiftDialog = false,
                        successMessage = "Shift berhasil dibuka! Selamat bekerja 💪"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Gagal membuka shift: ${e.message}") }
            }
        }
    }

    // --- Close Shift ---
    fun onShowCloseShiftDialog() {
        _uiState.update {
            it.copy(
                showCloseShiftDialog = true,
                endCashInput = "",
                errorMessage = null
            )
        }
    }

    fun onDismissCloseShiftDialog() {
        _uiState.update { it.copy(showCloseShiftDialog = false) }
    }

    fun onEndCashInputChange(value: String) {
        if (value.all { c -> c.isDigit() || c == '.' }) {
            _uiState.update { it.copy(endCashInput = value) }
        }
    }

    fun closeShift() {
        val state = _uiState.value
        val shift = state.activeShift ?: return
        val endCash = state.endCashDouble

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                shiftRepository.closeShift(shift, endCash)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showCloseShiftDialog = false,
                        successMessage = "Shift berhasil ditutup. Terima kasih atas kerja kerasnya! 🙏"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Gagal menutup shift: ${e.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
