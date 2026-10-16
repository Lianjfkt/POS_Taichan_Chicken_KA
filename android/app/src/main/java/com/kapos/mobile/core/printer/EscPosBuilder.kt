package com.kapos.mobile.core.printer

import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class EscPosItem(
    val name: String,
    val price: Double,
    val quantity: Int,
    val spiceLevel: String = "",
    val notes: String = "",
    val subtotal: Double
)

class EscPosBuilder(private val paperWidth: Int = 58) {

    private val maxCols: Int = if (paperWidth >= 80) 48 else 32

    private val df: DecimalFormat = DecimalFormat("#,###", DecimalFormatSymbols(Locale("id", "ID")))

    private fun formatRp(amount: Double): String = "Rp ${df.format(amount)}"

    fun buildReceipt(
        storeName: String,
        address: String,
        phone: String,
        receiptFooter: String,
        orderNo: String,
        dateStr: String,
        orderType: String,
        tableNo: String?,
        cashierName: String,
        customerName: String?,
        items: List<EscPosItem>,
        subtotal: Double,
        discount: Double,
        total: Double,
        paymentMethod: String,
        cashGiven: Double,
        change: Double
    ): ByteArray {
        val out = ByteArrayOutputStream()

        fun write(vararg b: Int) = b.forEach { out.write(it) }
        fun writeBytes(bytes: ByteArray) = out.write(bytes)
        fun writeText(text: String) = out.write(text.toByteArray(Charsets.ISO_8859_1))
        fun line(text: String = "") {
            writeText(text)
            write(0x0A)
        }
        fun divider() = line("-".repeat(maxCols))

        fun twoCols(left: String, right: String): String {
            val space = maxCols - left.length - right.length
            return if (space > 0) left + " ".repeat(space) + right else "$left $right"
        }

        // ESC @ -> Initialize printer
        write(0x1B, 0x40)

        // Center Align & Double Size for Store Name
        write(0x1B, 0x61, 0x01) // Center
        write(0x1B, 0x45, 0x01) // Bold ON
        write(0x1D, 0x21, 0x11) // Double width & height
        line(storeName)

        // Normal Size
        write(0x1D, 0x21, 0x00)
        write(0x1B, 0x45, 0x00) // Bold OFF
        if (address.isNotBlank()) line(address)
        if (phone.isNotBlank()) line(phone)
        divider()

        // Metadata (Left Align)
        write(0x1B, 0x61, 0x00)
        line(twoCols("No. Nota", orderNo))
        line(twoCols("Waktu", dateStr))
        line(twoCols("Tipe", orderType + if (!tableNo.isNullOrBlank()) " (Meja $tableNo)" else ""))
        if (!customerName.isNullOrBlank()) line(twoCols("Pelanggan", customerName))
        line(twoCols("Kasir", cashierName))
        divider()

        // Items List
        items.forEach { item ->
            line(item.name)
            val subDetails = mutableListOf<String>()
            if (item.spiceLevel.isNotBlank()) subDetails.add(item.spiceLevel)
            if (item.notes.isNotBlank()) subDetails.add(item.notes)
            if (subDetails.isNotEmpty()) {
                line("  • " + subDetails.joinToString(" - "))
            }
            val qtyPrice = "${item.quantity}x @ ${formatRp(item.price)}"
            val subtotalStr = formatRp(item.subtotal)
            line(twoCols("  $qtyPrice", subtotalStr))
        }
        divider()

        // Totals
        line(twoCols("Subtotal", formatRp(subtotal)))
        if (discount > 0) {
            line(twoCols("Diskon", "- ${formatRp(discount)}"))
        }
        write(0x1B, 0x45, 0x01) // Bold
        line(twoCols("TOTAL", formatRp(total)))
        write(0x1B, 0x45, 0x00)
        line(twoCols("Metode Bayar", paymentMethod))
        if (paymentMethod.equals("Tunai", ignoreCase = true) || paymentMethod.equals("CASH", ignoreCase = true)) {
            line(twoCols("Bayar Tunai", formatRp(cashGiven)))
            line(twoCols("Kembalian", formatRp(change)))
        }
        divider()

        // Footer (Center)
        write(0x1B, 0x61, 0x01)
        line(receiptFooter)
        line("")
        line("www.taichanchickenka.com")
        line("")
        line("")

        // Paper Feed & Cut
        write(0x1D, 0x56, 0x42, 0x00)

        return out.toByteArray()
    }

    fun buildShiftReceipt(
        storeName: String,
        cashierName: String,
        openedAtStr: String,
        closedAtStr: String,
        startCash: Double,
        cashSales: Double,
        nonCashSales: Double,
        cashIn: Double,
        cashOut: Double,
        expectedCash: Double,
        actualCash: Double,
        selisih: Double
    ): ByteArray {
        val out = ByteArrayOutputStream()

        fun write(vararg b: Int) = b.forEach { out.write(it) }
        fun writeText(text: String) = out.write(text.toByteArray(Charsets.ISO_8859_1))
        fun line(text: String = "") {
            writeText(text)
            write(0x0A)
        }
        fun divider() = line("-".repeat(maxCols))
        fun doubleDivider() = line("=".repeat(maxCols))

        fun twoCols(left: String, right: String): String {
            val space = maxCols - left.length - right.length
            return if (space > 0) left + " ".repeat(space) + right else "$left $right"
        }

        // Initialize printer
        write(0x1B, 0x40)

        // Store Header (Center, Bold)
        write(0x1B, 0x61, 0x01)
        write(0x1B, 0x45, 0x01)
        write(0x1D, 0x21, 0x11)
        line(storeName)

        // Title
        write(0x1D, 0x21, 0x00)
        write(0x1B, 0x45, 0x00)
        line("LAPORAN REKAP SHIFT")
        doubleDivider()

        // Shift Metadata (Left)
        write(0x1B, 0x61, 0x00)
        line(twoCols("Kasir", cashierName))
        line(twoCols("Buka", openedAtStr))
        line(twoCols("Tutup", closedAtStr))
        divider()

        // Cash Flow Details
        line(twoCols("Modal Awal (Laci)", formatRp(startCash)))
        line(twoCols("Penjualan Tunai", formatRp(cashSales)))
        if (cashIn > 0) {
            line(twoCols("Kas Masuk (+)", formatRp(cashIn)))
        }
        if (cashOut > 0) {
            line(twoCols("Kas Keluar (-)", "- ${formatRp(cashOut)}"))
        }
        divider()

        // Expected vs Actual Cash
        write(0x1B, 0x45, 0x01)
        line(twoCols("Kas Sistem (Diharapkan)", formatRp(expectedCash)))
        line(twoCols("Kas Fisik (Dihitung)", formatRp(actualCash)))
        write(0x1B, 0x45, 0x00)

        val selisihStatus = when {
            selisih > 0 -> "Lebih: +${formatRp(selisih)}"
            selisih < 0 -> "Kurang: -${formatRp(-selisih)}"
            else -> "Seimbang (Pas)"
        }
        line(twoCols("Status Selisih", selisihStatus))
        divider()

        // Non Cash Sales
        line(twoCols("Penjualan Non-Tunai", formatRp(nonCashSales)))
        line(twoCols("Total Omzet Shift", formatRp(cashSales + nonCashSales)))
        doubleDivider()

        // Signatures (Center)
        write(0x1B, 0x61, 0x01)
        line("")
        line("Tanda Tangan Kasir")
        line("")
        line("")
        line("( $cashierName )")
        line("")
        line("")

        // Paper Feed & Cut
        write(0x1D, 0x56, 0x42, 0x00)

        return out.toByteArray()
    }
}
