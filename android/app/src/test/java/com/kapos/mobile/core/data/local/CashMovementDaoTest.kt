package com.kapos.mobile.core.data.local

import com.kapos.mobile.domain.model.CashMovementType
import org.junit.Assert.assertEquals
import org.junit.Test

class CashMovementDaoTest {
    @Test
    fun testCashMovementTypeDisplayName() {
        assertEquals("Kas Masuk", CashMovementType.CASH_IN.displayName)
        assertEquals("Kas Keluar", CashMovementType.CASH_OUT.displayName)
    }
}
