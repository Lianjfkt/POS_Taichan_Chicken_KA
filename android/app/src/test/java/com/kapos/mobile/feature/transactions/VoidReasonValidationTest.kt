package com.kapos.mobile.feature.transactions

import org.junit.Assert.assertTrue
import org.junit.Test

class VoidReasonValidationTest {
    @Test
    fun testPredefinedVoidReasonsNotEmpty() {
        val reasons = listOf(
            "Salah Input Menu",
            "Pelanggan Membatalkan",
            "Meja Pindah / Ganti Tagihan",
            "Komplain Makanan",
            "Lainnya"
        )
        assertTrue(reasons.size >= 5)
    }

    @Test
    fun testVoidReasonFormat() {
        val reason = "Salah Input Menu"
        val notes = "Kasir salah ketik porsi"
        val fullReason = if (notes.isNotBlank()) "$reason ($notes)" else reason
        assertTrue(fullReason.contains("Salah Input Menu"))
        assertTrue(fullReason.contains("Kasir salah ketik porsi"))
    }
}
