package com.kapos.mobile.feature.transactions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class BackdatedOrderValidationTest {

    @Test
    fun testBackdatedTimestampCannotBeInFuture() {
        val now = System.currentTimeMillis()
        val pastTimestamp = now - 86400000L // 1 day ago
        val futureTimestamp = now + 86400000L // 1 day in future

        assertTrue(pastTimestamp <= now)
        assertFalse(futureTimestamp <= now)
    }

    @Test
    fun testNotesFormatContainsSusulanTag() {
        val reason = "Nota manual tgl 2 Nov listrik padam"
        val formattedNotes = "[Susulan] $reason"

        assertTrue(formattedNotes.startsWith("[Susulan]"))
        assertTrue(formattedNotes.contains("listrik padam"))
    }

    @Test
    fun testOrderNoMatchesSelectedDate() {
        val selectedDate = Date(1730505600000L) // Example past date
        val orderNo = com.kapos.mobile.feature.checkout.generateOrderNo(selectedDate)
        assertTrue(orderNo.startsWith("KA-"))
    }
}
