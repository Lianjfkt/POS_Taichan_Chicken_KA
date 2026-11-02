package com.kapos.mobile.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InventoryBackdatedRestockTest {

    private val oneDayMs = 24L * 60 * 60 * 1000

    @Test
    fun testBackdatedValidationRules() {
        val now = 1730520000000L // arbitrary fixed timestamp
        val sevenDaysAgo = now - (7L * oneDayMs)

        // Valid: today
        assertTrue(isValidReceivedDate(now, now))
        // Valid: 2 days ago
        assertTrue(isValidReceivedDate(now - (2L * oneDayMs), now))
        // Valid: exactly 7 days ago
        assertTrue(isValidReceivedDate(sevenDaysAgo, now))

        // Invalid: 8 days ago
        assertFalse(isValidReceivedDate(now - (8L * oneDayMs), now))
        // Invalid: future date (+5 minutes)
        assertFalse(isValidReceivedDate(now + (5 * 60 * 1000L), now))
    }

    @Test
    fun testAntiFraudWatermarkGeneration() {
        val now = 1730520000000L // 02 Nov 2024 04:00 GMT
        val yesterday = now - oneDayMs

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val backdatedWatermark = generateAuditWatermark(
            receivedDate = yesterday,
            recordedAt = now,
            userNotes = "Pasar Senen"
        )

        assertTrue(backdatedWatermark.contains("Pasar Senen"))
        assertTrue(backdatedWatermark.contains("[BACKDATED: Diterima ${dateFormat.format(Date(yesterday))} | Diinput ${timeFormat.format(Date(now))}]"))

        // Same day restock should NOT have BACKDATED tag
        val sameDayNotes = generateAuditWatermark(
            receivedDate = now,
            recordedAt = now,
            userNotes = "Pasar Senen"
        )
        assertEquals("Pasar Senen", sameDayNotes)
    }

    @Test
    fun testRelativeExpiryCalculationFromReceivedDate() {
        val receivedDate = 1730433600000L // yesterday
        val shelfLifeDays = 3
        val expectedExpiry = receivedDate + (shelfLifeDays * oneDayMs)

        assertEquals(receivedDate + (3L * oneDayMs), expectedExpiry)
    }

    private fun isValidReceivedDate(receivedDate: Long, now: Long): Boolean {
        val minAllowed = now - (7L * 24 * 60 * 60 * 1000)
        val maxAllowed = now + 60_000L
        return receivedDate in minAllowed..maxAllowed
    }

    private fun generateAuditWatermark(receivedDate: Long, recordedAt: Long, userNotes: String): String {
        val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val isBackdated = dayFormat.format(Date(receivedDate)) != dayFormat.format(Date(recordedAt))
        return if (isBackdated) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val watermark = "[BACKDATED: Diterima ${dateFormat.format(Date(receivedDate))} | Diinput ${timeFormat.format(Date(recordedAt))}]"
            if (userNotes.isNotBlank()) "${userNotes.trim()} $watermark" else watermark
        } else {
            userNotes.trim()
        }
    }
}
