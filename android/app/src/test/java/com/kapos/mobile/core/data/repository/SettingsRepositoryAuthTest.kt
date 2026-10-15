package com.kapos.mobile.core.data.repository

import com.kapos.mobile.domain.model.AuthSession
import com.kapos.mobile.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositoryAuthTest {

    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        // In-memory instance for testing
        repository = SettingsRepository(context = null)
    }

    @Test
    fun testDefaultPinsAndAuthValidation() {
        // Default PIN Kasir = 1111, Owner = 1234
        assertTrue(repository.verifyPin(UserRole.CASHIER, "1111"))
        assertFalse(repository.verifyPin(UserRole.CASHIER, "0000"))

        assertTrue(repository.verifyPin(UserRole.OWNER, "1234"))
        assertFalse(repository.verifyPin(UserRole.OWNER, "9999"))
    }

    @Test
    fun testSessionUpdateAndLogout() {
        assertFalse(repository.settingsFlow.value.currentSession.isLoggedIn)

        val activeSession = AuthSession(
            isLoggedIn = true,
            role = UserRole.CASHIER,
            cashierName = "Budi Kasir"
        )
        repository.updateSession(activeSession)

        assertTrue(repository.settingsFlow.value.currentSession.isLoggedIn)
        assertEquals(UserRole.CASHIER, repository.settingsFlow.value.currentSession.role)
        assertEquals("Budi Kasir", repository.settingsFlow.value.currentSession.cashierName)

        repository.logout()
        assertFalse(repository.settingsFlow.value.currentSession.isLoggedIn)
    }

    @Test
    fun testDailyBepAndFixedCostSettings() {
        val settings = repository.settingsFlow.value
        assertEquals(350000.0, settings.dailyFixedCost, 0.01)
        assertEquals(1500000.0, settings.dailyRevenueTarget, 0.01)

        val updated = settings.copy(
            dailyFixedCost = 400000.0,
            dailyRevenueTarget = 2000000.0
        )
        repository.saveSettings(updated)

        assertEquals(400000.0, repository.settingsFlow.value.dailyFixedCost, 0.01)
        assertEquals(2000000.0, repository.settingsFlow.value.dailyRevenueTarget, 0.01)
    }
}
