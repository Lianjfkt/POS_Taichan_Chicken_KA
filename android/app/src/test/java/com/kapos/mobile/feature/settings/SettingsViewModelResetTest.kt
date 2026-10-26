package com.kapos.mobile.feature.settings

import com.kapos.mobile.core.data.repository.SettingsRepository
import com.kapos.mobile.domain.model.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelResetTest {
    @Test
    fun ownerPinVerification_validatesCorrectly() {
        val settingsRepo = SettingsRepository(context = null)
        // Default owner pin adalah 1234
        assertTrue(settingsRepo.verifyPin(UserRole.OWNER, "1234"))
        assertFalse(settingsRepo.verifyPin(UserRole.OWNER, "9999"))
        assertFalse(settingsRepo.verifyPin(UserRole.OWNER, "1111")) // PIN kasir bukan PIN owner
    }
}
