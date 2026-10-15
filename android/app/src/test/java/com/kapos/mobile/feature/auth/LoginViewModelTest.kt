package com.kapos.mobile.feature.auth

import com.kapos.mobile.core.data.repository.SettingsRepository
import com.kapos.mobile.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginViewModelTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        settingsRepository = SettingsRepository(context = null)
        viewModel = LoginViewModel(settingsRepository)
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertEquals(UserRole.CASHIER, state.selectedRole)
        assertEquals("", state.pinBuffer)
        assertFalse(state.isLoginSuccess)
        assertNull(state.errorMessage)
    }

    @Test
    fun testCashierLoginSuccess() {
        // Default PIN Kasir: 1111
        viewModel.onDigitClick("1")
        viewModel.onDigitClick("1")
        viewModel.onDigitClick("1")
        viewModel.onDigitClick("1")

        val state = viewModel.uiState.value
        assertTrue(state.isLoginSuccess)
        assertNotNull(state.loggedInSession)
        assertEquals(UserRole.CASHIER, state.loggedInSession?.role)
        assertTrue(settingsRepository.settingsFlow.value.currentSession.isLoggedIn)
    }

    @Test
    fun testOwnerLoginSuccess() {
        // Switch to Owner
        viewModel.onSelectRole(UserRole.OWNER)
        assertEquals(UserRole.OWNER, viewModel.uiState.value.selectedRole)

        // Default PIN Owner: 1234
        viewModel.onDigitClick("1")
        viewModel.onDigitClick("2")
        viewModel.onDigitClick("3")
        viewModel.onDigitClick("4")

        val state = viewModel.uiState.value
        assertTrue(state.isLoginSuccess)
        assertEquals(UserRole.OWNER, state.loggedInSession?.role)
        assertTrue(settingsRepository.settingsFlow.value.currentSession.isLoggedIn)
    }

    @Test
    fun testWrongPinShowsErrorAndClearsBuffer() {
        viewModel.onDigitClick("9")
        viewModel.onDigitClick("9")
        viewModel.onDigitClick("9")
        viewModel.onDigitClick("9")

        val state = viewModel.uiState.value
        assertFalse(state.isLoginSuccess)
        assertEquals("", state.pinBuffer)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun testClearAndDeleteOperations() {
        viewModel.onDigitClick("1")
        viewModel.onDigitClick("2")
        assertEquals("12", viewModel.uiState.value.pinBuffer)

        viewModel.onDeleteClick()
        assertEquals("1", viewModel.uiState.value.pinBuffer)

        viewModel.onClearClick()
        assertEquals("", viewModel.uiState.value.pinBuffer)
    }
}
