package com.kapos.mobile.feature.shift

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.kapos.mobile.core.theme.SurfaceTint
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.domain.model.CashMovementType

@Composable
fun PettyCashDialog(
    type: CashMovementType,
    selectedCategory: String,
    amountInput: String,
    notesInput: String,
    onCategoryChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val categories = if (type == CashMovementType.CASH_OUT) {
        listOf("Bahan Baku", "Operasional", "Galon/Es", "Bensin Kurir", "Lainnya")
    } else {
        listOf("Setoran Modal", "Pengembalian Kasbon", "Lainnya")
    }

    val title = if (type == CashMovementType.CASH_OUT) "Catat Kas Keluar (-)" else "Catat Kas Masuk (+)"
    val accentColor = if (type == CashMovementType.CASH_OUT) Color(0xFFEF4444) else Color(0xFF10B981)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.Lg)
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, AppShapes.Lg)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Laci kasir operasional",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Chips
                Text(
                    text = "Kategori",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(AppShapes.Sm)
                                .background(if (isSelected) accentColor.copy(alpha = 0.15f) else SurfaceTint)
                                .border(1.dp, if (isSelected) accentColor else OutlineSoft, AppShapes.Sm)
                                .clickable { onCategoryChange(cat) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) accentColor else TextPrimary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                if (categories.size > 3) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.drop(3).forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(AppShapes.Sm)
                                    .background(if (isSelected) accentColor.copy(alpha = 0.15f) else SurfaceTint)
                                    .border(1.dp, if (isSelected) accentColor else OutlineSoft, AppShapes.Sm)
                                    .clickable { onCategoryChange(cat) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) accentColor else TextPrimary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Nominal Input
                Text(
                    text = "Nominal Uang (Rp)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = onAmountChange,
                    placeholder = { Text("Contoh: 25000", fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = OutlineSoft
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Keterangan / Catatan
                Text(
                    text = "Catatan / Keterangan",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = onNotesChange,
                    placeholder = { Text("Contoh: Beli es kristal 2 kantong", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = OutlineSoft
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                PrimaryButton(
                    text = if (type == CashMovementType.CASH_OUT) "Simpan Kas Keluar" else "Simpan Kas Masuk",
                    onClick = onSubmit,
                    enabled = amountInput.isNotBlank() && (amountInput.toDoubleOrNull() ?: 0.0) > 0,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
