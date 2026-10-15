package com.kapos.mobile.core.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {
    private val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val decimalFormat = DecimalFormat("#,###", symbols)

    fun formatRupiah(amount: Double): String {
        return "Rp ${decimalFormat.format(amount)}"
    }

    fun formatRupiah(amount: Long): String {
        return "Rp ${decimalFormat.format(amount)}"
    }

    fun formatRupiah(amount: Int): String {
        return "Rp ${decimalFormat.format(amount)}"
    }
}
