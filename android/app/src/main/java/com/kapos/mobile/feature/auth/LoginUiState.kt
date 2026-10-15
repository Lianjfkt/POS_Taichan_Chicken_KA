package com.kapos.mobile.feature.auth

import com.kapos.mobile.domain.model.AuthSession
import com.kapos.mobile.domain.model.UserRole

data class LoginUiState(
    val selectedRole: UserRole = UserRole.CASHIER,
    val pinBuffer: String = "",
    val errorMessage: String? = null,
    val isLoginSuccess: Boolean = false,
    val loggedInSession: AuthSession? = null
)
