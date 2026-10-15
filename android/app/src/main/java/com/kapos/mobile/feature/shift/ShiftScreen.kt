package com.kapos.mobile.feature.shift

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.kapos.mobile.core.theme.Warning
import com.kapos.mobile.core.utils.CurrencyFormatter
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
        onCloseShiftClick = viewModel::onShowCloseShiftDialog,
        onDismissOpenShift = viewModel::onDismissOpenShiftDialog,
        onDismissCloseShift = viewModel::onDismissCloseShiftDialog,
        onStartCashChange = viewModel::onStartCashInputChange,
        onEndCashChange = viewModel::onEndCashInputChange,
        onCashierNameChange = viewModel::onCashierNameInputChange,
        onConfirmOpenShift = viewModel::openShift,
        onConfirmCloseShift = viewModel::closeShift,
        modifier = modifier
    )
}

@Composable
fun ShiftScreen(
    uiState: ShiftUiState,
    onOpenShiftClick: () -> Unit,
    onCloseShiftClick: () -> Unit,
    onDismissOpenShift: () -> Unit,
    onDismissCloseShift: () -> Unit,
    onStartCashChange: (String) -> Unit,
    onEndCashChange: (String) -> Unit,
    onCashierNameChange: (String) -> Unit,
    onConfirmOpenShift: () -> Unit,
    onConfirmCloseShift: () -> Unit,
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
                        text = "⏱️ Manajemen Shift",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Buka & tutup shift, pantau kas harian",
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
                    onCloseShiftClick = onCloseShiftClick
                )
            }

            // History header
            if (uiState.recentShifts.isNotEmpty()) {
                item {
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

        // Close Shift Dialog
        if (uiState.showCloseShiftDialog) {
            CloseShiftDialog(
                uiState = uiState,
                onDismiss = onDismissCloseShift,
                onEndCashChange = onEndCashChange,
                onConfirm = onConfirmCloseShift
            )
        }
    }
}

@Composable
private fun ActiveShiftCard(
    uiState: ShiftUiState,
    onOpenShiftClick: () -> Unit,
    onCloseShiftClick: () -> Unit
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
                            text = "Dibuka: ${sdf.format(Date(shift.openedAt))}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                }
            }

            if (shift != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = OutlineSoft)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = shift.cashierName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                ShiftStatRow("Kas Awal", uiState.formattedStartCash)
                ShiftStatRow("Penjualan (Sistem)", uiState.formattedSystemSales, color = Primary)
                ShiftStatRow("Estimasi Kas Akhir", uiState.formattedExpectedCash, color = Success, bold = true)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.hasActiveShift) {
                SecondaryButton(
                    text = "Tutup Shift & Hitung Kas",
                    icon = Icons.Default.Lock,
                    onClick = onCloseShiftClick,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
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
private fun ShiftStatRow(label: String, value: String, color: Color = TextPrimary, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
                        text = "Penjualan",
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
                InfoChip("Kas Awal", CurrencyFormatter.formatRupiah(shift.startCash))
                InfoChip("Kas Akhir", CurrencyFormatter.formatRupiah(shift.endCash))
                val diff = shift.endCash - (shift.startCash + shift.systemSales)
                InfoChip(
                    label = if (diff >= 0) "Lebih" else "Kurang",
                    value = CurrencyFormatter.formatRupiah(Math.abs(diff)),
                    valueColor = if (diff >= 0) Success else Danger
                )
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, valueColor: Color = TextPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        )
    }
}

// --- Dialogs ---

@Composable
fun OpenShiftDialog(
    uiState: ShiftUiState,
    onDismiss: () -> Unit,
    onStartCashChange: (String) -> Unit,
    onCashierNameChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = {
            Text(
                text = "🔓 Buka Shift Baru",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Masukkan nama kasir dan jumlah kas awal di laci kas.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                OutlinedTextField(
                    value = uiState.cashierNameInput,
                    onValueChange = onCashierNameChange,
                    label = { Text("Nama Kasir") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )
                OutlinedTextField(
                    value = uiState.startCashInput,
                    onValueChange = onStartCashChange,
                    label = { Text("Kas Awal (Rp)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )
                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.labelSmall.copy(color = Danger)
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Buka Shift",
                icon = Icons.Default.Check,
                onClick = onConfirm,
                isLoading = uiState.isLoading
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary)
            }
        }
    )
}

@Composable
fun CloseShiftDialog(
    uiState: ShiftUiState,
    onDismiss: () -> Unit,
    onEndCashChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = {
            Text(
                text = "🔒 Tutup Shift",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Summary info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.Sm)
                        .background(SurfaceTint)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ShiftStatRow("Kas Awal", uiState.formattedStartCash)
                        ShiftStatRow("Penjualan (Sistem)", uiState.formattedSystemSales, color = Primary)
                        ShiftStatRow("Estimasi Kas Akhir", uiState.formattedExpectedCash, color = Success, bold = true)
                    }
                }
                OutlinedTextField(
                    value = uiState.endCashInput,
                    onValueChange = onEndCashChange,
                    label = { Text("Kas Akhir Aktual (Rp)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )
                // Selisih indicator
                if (uiState.endCashInput.isNotBlank()) {
                    val selisih = uiState.selisih
                    val selisihColor = if (selisih >= 0) Success else Danger
                    val selisihLabel = if (selisih >= 0) "Lebih" else "Kurang"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppShapes.Sm)
                            .background(selisihColor.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Selisih Kas ($selisihLabel)",
                            style = MaterialTheme.typography.bodySmall.copy(color = selisihColor)
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(Math.abs(selisih)),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = selisihColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.labelSmall.copy(color = Danger)
                    )
                }
            }
        },
        confirmButton = {
            SecondaryButton(
                text = "Tutup & Simpan",
                icon = Icons.Default.Lock,
                onClick = onConfirm,
                isLoading = uiState.isLoading
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary)
            }
        }
    )
}
