package com.kapos.mobile.core.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object ShiftShareFormatter {

    private val df = DecimalFormat("#,###", DecimalFormatSymbols(Locale("id", "ID")))
    private fun formatRp(amount: Double): String = "Rp ${df.format(amount)}"

    fun formatWhatsAppSummary(
        storeName: String,
        cashierName: String,
        dateStr: String,
        startCash: Double,
        cashSales: Double,
        nonCashSales: Double,
        cashIn: Double,
        cashOut: Double,
        expectedCash: Double,
        actualCash: Double,
        selisih: Double
    ): String {
        val selisihText = when {
            selisih > 0 -> "+${formatRp(selisih)} (LEBIH ⚠️)"
            selisih < 0 -> "-${formatRp(-selisih)} (MINUS ❌)"
            else -> "Rp 0 (PAS ✅)"
        }

        return buildString {
            appendLine("📋 *REKAP SHIFT KASIR — $storeName*")
            appendLine("📅 Tanggal: $dateStr")
            appendLine("👤 Kasir: $cashierName")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("💰 Modal Awal (Laci): ${formatRp(startCash)}")
            appendLine("💵 Penjualan Tunai: ${formatRp(cashSales)}")
            appendLine("📱 Non-Tunai (QRIS/Trf): ${formatRp(nonCashSales)}")
            if (cashIn > 0) {
                appendLine("➕ Kas Masuk Operasional: ${formatRp(cashIn)}")
            }
            if (cashOut > 0) {
                appendLine("➖ Kas Keluar Operasional: ${formatRp(cashOut)}")
            }
            appendLine("------------------------------------")
            appendLine("🎯 Kas Sistem (Diharapkan): ${formatRp(expectedCash)}")
            appendLine("💵 Kas Fisik (Dihitung): ${formatRp(actualCash)}")
            appendLine("⚖️ Status Selisih: $selisihText")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("Total Omzet Shift: ${formatRp(cashSales + nonCashSales)}")
        }
    }
}
