package com.kapos.mobile.feature.shift

import com.kapos.mobile.domain.model.Shift
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class ShiftUiState(
    // Active shift state
    val activeShift: Shift? = null,
    val isLoading: Boolean = false,

    // Open Shift Dialog
    val showOpenShiftDialog: Boolean = false,
    val startCashInput: String = "",
    val cashierNameInput: String = "",

    // Close Shift Dialog
    val showCloseShiftDialog: Boolean = false,
    val endCashInput: String = "",

    // Error / feedback
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Shift history (last 10)
    val recentShifts: List<Shift> = emptyList()
) {
    val hasActiveShift: Boolean get() = activeShift != null

    val formattedStartCash: String get() = activeShift?.let { formatRp(it.startCash) } ?: "-"
    val formattedSystemSales: String get() = activeShift?.let { formatRp(it.systemSales) } ?: "-"
    val formattedExpectedCash: String get() = activeShift?.let { formatRp(it.startCash + it.systemSales) } ?: "-"

    val endCashDouble: Double get() = endCashInput.toDoubleOrNull() ?: 0.0
    val startCashDouble: Double get() = startCashInput.toDoubleOrNull() ?: 0.0
    val selisih: Double get() = endCashDouble - (( activeShift?.startCash ?: 0.0) + (activeShift?.systemSales ?: 0.0))

    private val df = DecimalFormat("#,###", DecimalFormatSymbols(Locale("id", "ID")))
    private fun formatRp(amount: Double): String = "Rp ${df.format(amount)}"
}
