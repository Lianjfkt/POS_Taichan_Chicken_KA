package com.kapos.mobile.feature.shift

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kapos.mobile.core.components.PrimaryButton
import com.kapos.mobile.core.components.SecondaryButton
import com.kapos.mobile.core.theme.AppShapes
import com.kapos.mobile.core.theme.Danger
import com.kapos.mobile.core.theme.OutlineSoft
import com.kapos.mobile.core.theme.Primary
import com.kapos.mobile.core.theme.PrimarySoft
import com.kapos.mobile.core.theme.ScreenBackground
import com.kapos.mobile.core.theme.Success
import com.kapos.mobile.core.theme.SurfaceTint
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.core.utils.CurrencyFormatter
import com.kapos.mobile.domain.model.CashMovement
import com.kapos.mobile.domain.model.CashMovementType
import com.kapos.mobile.domain.model.Shift
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun ShiftRoute(viewModel: ShiftViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()

    // Auto-dismiss success message after 3 seconds
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            delay(3000)
            viewModel.clearMessages()
        }
    }

    ShiftScreen(
        uiState = uiState,
        onOpenShiftClick = viewModel::onShowOpenShiftDialog,
        onCloseShiftClick = viewModel::onShowBlindCountDialog,
        onDismissOpenShift = viewModel::onDismissOpenShiftDialog,
        onStartCashChange = viewModel::onStartCashInputChange,
        onCashierNameChange = viewModel::onCashierNameInputChange,
        onConfirmOpenShift = viewModel::openShift,
        onShowPettyCash = viewModel::onShowPettyCashDialog,
        onDismissPettyCash = viewModel::onDismissPettyCashDialog,
        onPettyCashCategoryChange = viewModel::onPettyCashCategoryChange,
        onPettyCashAmountChange = viewModel::onPettyCashAmountChange,
        onPettyCashNotesChange = viewModel::onPettyCashNotesChange,
        onPettyCashPinChange = viewModel::onPettyCashPinChange,
        onSubmitPettyCash = viewModel::submitPettyCash,
        onDismissBlindCount = viewModel::onDismissBlindCountDialog,
        onDenominationCountChange = viewModel::onDenominationCountChange,
        onCoinChange = viewModel::onCoinAmountChange,
        onConfirmCloseShift = viewModel::confirmCloseShift,
        onPrintShiftReceipt = viewModel::printShiftReceipt,
        getWhatsAppShareText = viewModel::getWhatsAppShareText,
        onDismissShiftReport = viewModel::onDismissShiftReportDialog,
        modifier = modifier
    )
}

@Composable
fun ShiftScreen(
    uiState: ShiftUiState,
    onOpenShiftClick: () -> Unit,
    onCloseShiftClick: () -> Unit,
    onDismissOpenShift: () -> Unit,
    onStartCashChange: (String) -> Unit,
    onCashierNameChange: (String) -> Unit,
    onConfirmOpenShift: () -> Unit,
    onShowPettyCash: (CashMovementType) -> Unit,
    onDismissPettyCash: () -> Unit,
    onPettyCashCategoryChange: (String) -> Unit,
    onPettyCashAmountChange: (String) -> Unit,
    onPettyCashNotesChange: (String) -> Unit,
    onPettyCashPinChange: (String) -> Unit,
    onSubmitPettyCash: () -> Unit,
    onDismissBlindCount: () -> Unit,
    onDenominationCountChange: (denomination: Int, count: Int) -> Unit,
    onCoinChange: (String) -> Unit,
    onConfirmCloseShift: () -> Unit,
    onPrintShiftReceipt: () -> Unit,
    getWhatsAppShareText: () -> String,
    onDismissShiftReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "⏱️ Manajemen Shift & Laci Kas",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Buka & tutup shift, petty cash, rekonsiliasi kasir",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }

            // Success/Error Banner
            if (uiState.successMessage != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(AppShapes.Md)
                            .background(Success.copy(alpha = 0.12f))
                            .border(1.dp, Success.copy(alpha = 0.4f), AppShapes.Md)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.successMessage,
                            style = MaterialTheme.typography.bodySmall.copy(color = Success, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            // Active Shift Card
            item {
                ActiveShiftCard(
                    uiState = uiState,
                    onOpenShiftClick = onOpenShiftClick,
                    onCloseShiftClick = onCloseShiftClick,
                    onShowPettyCash = onShowPettyCash
                )
            }

            // Petty Cash Movements List (Active Shift)
            if (uiState.hasActiveShift && uiState.cashMovements.isNotEmpty()) {
                item {
                    Text(
                        text = "Arus Kas Laci (Shift Ini)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
                items(uiState.cashMovements) { movement ->
                    CashMovementItemCard(movement = movement)
                }
            }

            // History header
            if (uiState.recentShifts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Riwayat Shift Terakhir",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
                items(uiState.recentShifts.filter { !it.isOpen }) { shift ->
                    ShiftHistoryCard(shift = shift)
                }
            }
        }

        // Open Shift Dialog
        if (uiState.showOpenShiftDialog) {
            OpenShiftDialog(
                uiState = uiState,
                onDismiss = onDismissOpenShift,
                onStartCashChange = onStartCashChange,
                onCashierNameChange = onCashierNameChange,
                onConfirm = onConfirmOpenShift
            )
        }

        // Petty Cash Dialog (Kas Masuk / Kas Keluar)
        if (uiState.showPettyCashDialog) {
            PettyCashDialog(
                type = uiState.pettyCashType,
                selectedCategory = uiState.pettyCashCategory,
                amountInput = uiState.pettyCashAmountInput,
                notesInput = uiState.pettyCashNotesInput,
                pinInput = uiState.pettyCashPinInput,
                pinError = uiState.pettyCashPinError,
                onCategoryChange = onPettyCashCategoryChange,
                onAmountChange = onPettyCashAmountChange,
                onNotesChange = onPettyCashNotesChange,
                onPinChange = onPettyCashPinChange,
                onDismiss = onDismissPettyCash,
                onSubmit = onSubmitPettyCash
            )
        }


        // Blind Cash Count Dialog
        if (uiState.showBlindCountDialog) {
            BlindCountDialog(
                uiState = uiState,
                onCountChange = onDenominationCountChange,
                onCoinChange = onCoinChange,
                onDismiss = onDismissBlindCount,
                onConfirmClose = onConfirmCloseShift
            )
        }

        // Shift Closed Summary Dialog
        if (uiState.showShiftReportDialog && uiState.lastClosedSummary != null) {
            ShiftReportSummaryDialog(
                summary = uiState.lastClosedSummary,
                isPrinting = uiState.isPrinting,
                onPrintReceipt = onPrintShiftReceipt,
                whatsAppShareText = getWhatsAppShareText(),
                onDismiss = onDismissShiftReport
            )
        }
    }
}

@Composable
private fun ActiveShiftCard(
    uiState: ShiftUiState,
    onOpenShiftClick: () -> Unit,
    onCloseShiftClick: () -> Unit,
    onShowPettyCash: (CashMovementType) -> Unit
) {
    val shift = uiState.activeShift

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(AppShapes.Lg)
            .background(SurfaceWhite)
            .border(1.dp, OutlineSoft, AppShapes.Lg)
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(AppShapes.Sm)
                        .background(if (uiState.hasActiveShift) Success.copy(alpha = 0.12f) else SurfaceTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.hasActiveShift) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (uiState.hasActiveShift) Success else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (uiState.hasActiveShift) "Shift Sedang Berjalan" else "Tidak Ada Shift Aktif",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.hasActiveShift) Success else TextSecondary
                        )
                    )
                    if (shift != null) {
                        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
                        Text(
                            text = "Kasir: ${shift.cashierName} • Buka: ${sdf.format(Date(shift.openedAt))}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                }
            }

            if (shift != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = OutlineSoft)
                Spacer(modifier = Modifier.height(12.dp))

                ShiftStatRow("Modal Awal (Kas Laci)", uiState.formattedStartCash)
                ShiftStatRow("Penjualan Tunai", uiState.formattedCashSales, color = Primary)
                ShiftStatRow("Penjualan Non-Tunai", uiState.formattedNonCashSales)
                if (uiState.cashInTotal > 0) {
                    ShiftStatRow("Kas Masuk (+)", uiState.formattedCashIn, color = Color(0xFF10B981))
                }
                if (uiState.cashOutTotal > 0) {
                    ShiftStatRow("Kas Keluar (-)", "- ${uiState.formattedCashOut}", color = Danger)
                }

                Divider(color = OutlineSoft, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                // SECURITY: Estimasi Kas Akhir DISEMBUNYIKAN dari kasir untuk menjaga integritas
                // Blind Cash Count. Kasir harus menghitung uang fisik secara mandiri tanpa referensi.
                // Angka ini hanya tampil di laporan akhir setelah shift resmi ditutup.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estimasi Kas Akhir",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Success.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tersembunyi (Blind Count)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Success.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Petty Cash Action Buttons (+ Kas Masuk, - Kas Keluar)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(AppShapes.Sm)
                            .background(Color(0xFF10B981).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), AppShapes.Sm)
                            .clickable { onShowPettyCash(CashMovementType.CASH_IN) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Kas Masuk", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(AppShapes.Sm)
                            .background(Danger.copy(alpha = 0.12f))
                            .border(1.dp, Danger.copy(alpha = 0.4f), AppShapes.Sm)
                            .clickable { onShowPettyCash(CashMovementType.CASH_OUT) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = null, tint = Danger, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Kas Keluar", color = Danger, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                SecondaryButton(
                    text = "Tutup Shift & Blind Count",
                    icon = Icons.Default.Lock,
                    onClick = onCloseShiftClick,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(
                    text = "Buka Shift Baru",
                    icon = Icons.Default.LockOpen,
                    onClick = onOpenShiftClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CashMovementItemCard(movement: CashMovement) {
    val isCashIn = movement.type == CashMovementType.CASH_IN
    val accentColor = if (isCashIn) Color(0xFF10B981) else Danger
    val sdf = SimpleDateFormat("HH:mm", Locale("id", "ID"))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(AppShapes.Md)
            .background(SurfaceWhite)
            .border(1.dp, OutlineSoft, AppShapes.Md)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(AppShapes.Xs)
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = movement.type.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = movement.category,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    )
                }
                if (movement.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = movement.notes,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isCashIn) "+ " else "- ") + CurrencyFormatter.formatRupiah(movement.amount),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                )
                Text(
                    text = sdf.format(Date(movement.createdAt)),
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                )
            }
        }
    }
}

@Composable
private fun ShiftStatRow(label: String, value: String, color: Color = TextPrimary, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = color,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun ShiftHistoryCard(shift: Shift) {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID"))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(AppShapes.Md)
            .background(SurfaceWhite)
            .border(1.dp, OutlineSoft, AppShapes.Md)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = shift.cashierName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "${sdf.format(Date(shift.openedAt))} → ${shift.closedAt?.let { sdf.format(Date(it)) } ?: "-"}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatRupiah(shift.systemSales),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    )
                    Text(
                        text = "Total Penjualan",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = OutlineSoft)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Kas Awal: ${CurrencyFormatter.formatRupiah(shift.startCash)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
                Text(
                    text = "Kas Akhir: ${CurrencyFormatter.formatRupiah(shift.endCash)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun OpenShiftDialog(
    uiState: ShiftUiState,
    onDismiss: () -> Unit,
    onStartCashChange: (String) -> Unit,
    onCashierNameChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.Lg)
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, AppShapes.Lg)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Buka Shift Baru",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "Nama Kasir", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = uiState.cashierNameInput,
                    onValueChange = onCashierNameChange,
                    placeholder = { Text("Nama kasir...", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = OutlineSoft
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Modal Awal Laci (Rp)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = uiState.startCashInput,
                    onValueChange = onStartCashChange,
                    placeholder = { Text("Contoh: 200000", fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = OutlineSoft
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                PrimaryButton(
                    text = "Buka Shift",
                    onClick = onConfirm,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
