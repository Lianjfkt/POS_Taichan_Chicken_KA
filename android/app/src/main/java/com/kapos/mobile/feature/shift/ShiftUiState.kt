package com.kapos.mobile.feature.shift

import com.kapos.mobile.domain.model.CashMovement
import com.kapos.mobile.domain.model.CashMovementType
import com.kapos.mobile.domain.model.Shift
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class ShiftClosedSummary(
    val cashierName: String,
    val openedAtStr: String,
    val closedAtStr: String,
    val startCash: Double,
    val cashSales: Double,
    val nonCashSales: Double,
    val cashIn: Double,
    val cashOut: Double,
    val expectedCash: Double,
    val actualCash: Double,
    val selisih: Double
)

data class ShiftUiState(
    // Active shift state
    val activeShift: Shift? = null,
    val isLoading: Boolean = false,

    // Realtime shift financial metrics
    val cashSales: Double = 0.0,
    val nonCashSales: Double = 0.0,
    val cashMovements: List<CashMovement> = emptyList(),

    // Open Shift Dialog
    val showOpenShiftDialog: Boolean = false,
    val startCashInput: String = "",
    val cashierNameInput: String = "",

    // Petty Cash Dialog
    val showPettyCashDialog: Boolean = false,
    val pettyCashType: CashMovementType = CashMovementType.CASH_OUT,
    val pettyCashCategory: String = "Operasional",
    val pettyCashAmountInput: String = "",
    val pettyCashNotesInput: String = "",
    // SECURITY: PIN otorisasi Owner untuk Kas Keluar
    val pettyCashPinInput: String = "",
    val pettyCashPinError: String? = null,

    // Blind Cash Count Dialog
    val showBlindCountDialog: Boolean = false,
    val count100k: Int = 0,
    val count50k: Int = 0,
    val count20k: Int = 0,
    val count10k: Int = 0,
    val count5k: Int = 0,
    val count2k: Int = 0,
    val count1k: Int = 0,
    val countCoin: Double = 0.0,

    // Shift Closed Report Dialog
    val showShiftReportDialog: Boolean = false,
    val lastClosedSummary: ShiftClosedSummary? = null,
    val isPrinting: Boolean = false,

    // Feedback
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Shift history (last 10)
    val recentShifts: List<Shift> = emptyList()
) {
    val hasActiveShift: Boolean get() = activeShift != null

    val cashInTotal: Double
        get() = cashMovements.filter { it.type == CashMovementType.CASH_IN }.sumOf { it.amount }

    val cashOutTotal: Double
        get() = cashMovements.filter { it.type == CashMovementType.CASH_OUT }.sumOf { it.amount }

    val expectedCash: Double
        get() = (activeShift?.startCash ?: 0.0) + cashSales + cashInTotal - cashOutTotal

    val actualPhysicalCash: Double
        get() = (count100k * 100_000.0) +
                (count50k * 50_000.0) +
                (count20k * 20_000.0) +
                (count10k * 10_000.0) +
                (count5k * 5_000.0) +
                (count2k * 2_000.0) +
                (count1k * 1_000.0) +
                countCoin

    val selisih: Double
        get() = actualPhysicalCash - expectedCash

    val formattedStartCash: String get() = activeShift?.let { formatRp(it.startCash) } ?: "-"
    val formattedCashSales: String get() = formatRp(cashSales)
    val formattedNonCashSales: String get() = formatRp(nonCashSales)
    val formattedCashIn: String get() = formatRp(cashInTotal)
    val formattedCashOut: String get() = formatRp(cashOutTotal)
    val formattedExpectedCash: String get() = formatRp(expectedCash)
    val formattedActualCash: String get() = formatRp(actualPhysicalCash)
    val formattedSelisih: String get() = formatRp(selisih)

    val startCashDouble: Double get() = startCashInput.toDoubleOrNull() ?: 0.0

    private val df = DecimalFormat("#,###", DecimalFormatSymbols(Locale("id", "ID")))
    fun formatRp(amount: Double): String = "Rp ${df.format(amount)}"
}
