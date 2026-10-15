package com.kapos.mobile.feature.auth

import androidx.lifecycle.ViewModel
import com.kapos.mobile.core.data.repository.SettingsRepository
import com.kapos.mobile.domain.model.AuthSession
import com.kapos.mobile.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LoginViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onSelectRole(role: UserRole) {
        _uiState.update {
            it.copy(
                selectedRole = role,
                pinBuffer = "",
                errorMessage = null,
                isLoginSuccess = false
            )
        }
    }

    fun onDigitClick(digit: String) {
        val currentPin = _uiState.value.pinBuffer
        if (currentPin.length >= 4) return

        val newPin = currentPin + digit
        _uiState.update { it.copy(pinBuffer = newPin, errorMessage = null) }

        if (newPin.length == 4) {
            verifyPin(newPin)
        }
    }

    fun onDeleteClick() {
        _uiState.update {
            if (it.pinBuffer.isNotEmpty()) {
                it.copy(pinBuffer = it.pinBuffer.dropLast(1), errorMessage = null)
            } else {
                it
            }
        }
    }

    fun onClearClick() {
        _uiState.update { it.copy(pinBuffer = "", errorMessage = null) }
    }

    fun resetLoginSuccess() {
        _uiState.update { it.copy(isLoginSuccess = false) }
    }

    private fun verifyPin(pin: String) {
        val role = _uiState.value.selectedRole
        val isValid = settingsRepository.verifyPin(role, pin)
        if (isValid) {
            val session = AuthSession(
                isLoggedIn = true,
                role = role,
                cashierName = if (role == UserRole.OWNER) "Owner Gerai" else settingsRepository.settingsFlow.value.cashierName
            )
            settingsRepository.updateSession(session)
            _uiState.update {
                it.copy(
                    isLoginSuccess = true,
                    loggedInSession = session,
                    errorMessage = null
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    pinBuffer = "",
                    errorMessage = "PIN ${role.displayName} salah. Silakan coba lagi."
                )
            }
        }
    }
}
