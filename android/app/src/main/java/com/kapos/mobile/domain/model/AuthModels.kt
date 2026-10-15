package com.kapos.mobile.domain.model

enum class UserRole(val displayName: String) {
    CASHIER("Kasir"),
    OWNER("Owner")
}

data class AuthSession(
    val isLoggedIn: Boolean = false,
    val role: UserRole = UserRole.CASHIER,
    val cashierName: String = "Kasir Utama"
)
