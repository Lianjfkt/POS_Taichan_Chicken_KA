package com.kapos.mobile.core.printer

import org.junit.Assert.assertTrue
import org.junit.Test

class EscPosBuilderTest {

    @Test
    fun testBuildReceiptReturnsValidEscPosBytes() {
        val builder = EscPosBuilder(paperWidth = 58)
        val items = listOf(
            EscPosItem(
                name = "Taichan Pedas Lv.3",
                price = 18000.0,
                quantity = 2,
                spiceLevel = "Lv.3",
                notes = "sambal dipisah",
                subtotal = 36000.0
            )
        )

        val bytes = builder.buildReceipt(
            storeName = "Taichan & Chicken KA",
            address = "Bandar Lampung",
            phone = "081234567890",
            receiptFooter = "Terima kasih!",
            orderNo = "KA-20261015-001",
            dateStr = "15 Okt 2026, 14:30",
            orderType = "Dine-In",
            tableNo = "05",
            cashierName = "Kasir 1",
            customerName = "Budi",
            items = items,
            subtotal = 36000.0,
            discount = 5000.0,
            total = 31000.0,
            paymentMethod = "Tunai",
            cashGiven = 50000.0,
            change = 19000.0
        )

        assertTrue(bytes.isNotEmpty())
        // Inisialisasi ESC @ (0x1B, 0x40)
        assertTrue(bytes[0] == 0x1B.toByte() && bytes[1] == 0x40.toByte())
        // Mengandung teks nama toko
        val content = String(bytes, Charsets.ISO_8859_1)
        assertTrue(content.contains("Taichan & Chicken KA"))
        assertTrue(content.contains("KA-20261015-001"))
        assertTrue(content.contains("31.000") || content.contains("31000"))
    }

    @Test
    fun testBuildShiftReceiptProducesBytes() {
        val builder = EscPosBuilder(paperWidth = 58)
        val bytes = builder.buildShiftReceipt(
            storeName = "KA Taichan & Chicken",
            cashierName = "Kasir Budi",
            openedAtStr = "16 Okt 2026, 10:00",
            closedAtStr = "16 Okt 2026, 22:00",
            startCash = 200000.0,
            cashSales = 1000000.0,
            nonCashSales = 500000.0,
            cashIn = 50000.0,
            cashOut = 20000.0,
            expectedCash = 1230000.0,
            actualCash = 1230000.0,
            selisih = 0.0
        )
        assertTrue(bytes.isNotEmpty())
        val content = String(bytes, Charsets.ISO_8859_1)
        assertTrue(content.contains("REKAP SHIFT"))
        assertTrue(content.contains("Kasir Budi"))
        assertTrue(content.contains("1.230.000") || content.contains("1230000"))
    }
}
