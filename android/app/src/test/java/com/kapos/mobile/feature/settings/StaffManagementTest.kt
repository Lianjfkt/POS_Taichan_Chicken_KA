package com.kapos.mobile.feature.settings

import com.kapos.mobile.core.data.repository.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StaffManagementTest {

    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        settingsRepository = SettingsRepository(context = null)
    }

    @Test
    fun testDefaultStaffList() {
        val staffList = settingsRepository.getStaffNames()
        assertTrue(staffList.contains("Kasir Utama"))
        assertTrue(staffList.contains("Kasir Pagi"))
        assertTrue(staffList.contains("Kasir Malam"))
        assertTrue(staffList.contains("Manager"))
    }

    @Test
    fun testSwitchActiveCashier() {
        settingsRepository.switchActiveCashier("Kasir Pagi")
        val currentSettings = settingsRepository.settingsFlow.value
        assertEquals("Kasir Pagi", currentSettings.cashierName)
        assertEquals("Kasir Pagi", currentSettings.currentSession.cashierName)
    }

    @Test
    fun testAddNewStaff() {
        settingsRepository.addStaffName("Kasir Weekend - Budi")
        val updatedList = settingsRepository.getStaffNames()
        assertTrue(updatedList.contains("Kasir Weekend - Budi"))

        // Re-adding duplicate shouldn't create duplicate entries
        val countBefore = updatedList.size
        settingsRepository.addStaffName("Kasir Weekend - Budi")
        val countAfter = settingsRepository.getStaffNames().size
        assertEquals(countBefore, countAfter)
    }
}
