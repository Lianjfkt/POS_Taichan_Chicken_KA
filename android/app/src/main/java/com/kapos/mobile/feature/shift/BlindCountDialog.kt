package com.kapos.mobile.feature.shift

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.kapos.mobile.core.theme.AppShapes
import com.kapos.mobile.core.theme.OutlineSoft
import com.kapos.mobile.core.theme.Primary
import com.kapos.mobile.core.theme.PrimarySoft
import com.kapos.mobile.core.theme.SurfaceTint
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.core.utils.CurrencyFormatter

@Composable
fun BlindCountDialog(
    uiState: ShiftUiState,
    onCountChange: (denomination: Int, count: Int) -> Unit,
    onCoinChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirmClose: () -> Unit
) {
    val scrollState = rememberScrollState()

    val denominations = listOf(
        Pair(100_000, uiState.count100k),
        Pair(50_000, uiState.count50k),
        Pair(20_000, uiState.count20k),
        Pair(10_000, uiState.count10k),
        Pair(5_000, uiState.count5k),
        Pair(2_000, uiState.count2k),
        Pair(1_000, uiState.count1k)
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.Lg)
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, AppShapes.Lg)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tutup Shift (Blind Count)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Hitung uang fisik di laci kasir",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Realtime Total Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.Md)
                        .background(PrimarySoft)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Uang Fisik:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(uiState.actualPhysicalCash),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Denominations List
                denominations.forEach { (denom, count) ->
                    DenominationRow(
                        denomination = denom,
                        count = count,
                        onCountChange = { newCount -> onCountChange(denom, newCount) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Divider(color = OutlineSoft, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                // Koin / Receh Manual
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Koin / Receh (Total)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Nominal koin keseluruhan",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }

                    OutlinedTextField(
                        value = if (uiState.countCoin > 0) uiState.countCoin.toInt().toString() else "",
                        onValueChange = onCoinChange,
                        placeholder = { Text("0", fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(110.dp),
                        singleLine = true,
                        shape = AppShapes.Sm,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = OutlineSoft
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                PrimaryButton(
                    text = "Konfirmasi Tutup Shift",
                    onClick = onConfirmClose,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DenominationRow(
    denomination: Int,
    count: Int,
    onCountChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.Sm)
            .background(SurfaceTint)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = CurrencyFormatter.formatRupiah(denomination.toDouble()),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            )
            Text(
                text = "= ${CurrencyFormatter.formatRupiah(denomination.toDouble() * count)}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (count > 0) Primary else TextSecondary,
                    fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { if (count > 0) onCountChange(count - 1) },
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SurfaceWhite)
                    .border(1.dp, OutlineSoft, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Minus",
                    tint = TextPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = "$count",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            )

            IconButton(
                onClick = { onCountChange(count + 1) },
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Plus",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
