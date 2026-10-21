package com.kapos.mobile.core.components

import com.kapos.mobile.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavDestinationsTest {

    @Test
    fun testCashierDestinationsIncludePosTransactionsShift() {
        val cashierDestinations = getBottomNavDestinations(UserRole.CASHIER)
        val routes = cashierDestinations.map { it.route }
        assertEquals(listOf("pos", "transactions", "shift"), routes)
    }

    @Test
    fun testOwnerDestinationsIncludeShiftAndOtherTabs() {
        val ownerDestinations = getBottomNavDestinations(UserRole.OWNER)
        val routes = ownerDestinations.map { it.route }
        assertTrue("Owner destinations must include 'shift'", routes.contains("shift"))
        assertEquals(
            listOf("pos", "transactions", "shift", "stock", "analytics", "settings"),
            routes
        )
    }

    @Test
    fun testReportDestinationExists() {
        assertEquals("report", NavDestination.Report.route)
        assertEquals("Laporan", NavDestination.Report.title)
    }
}
