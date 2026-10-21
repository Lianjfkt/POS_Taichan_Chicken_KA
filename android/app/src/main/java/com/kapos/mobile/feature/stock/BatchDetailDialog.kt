package com.kapos.mobile.feature.stock

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.kapos.mobile.core.theme.Success
import com.kapos.mobile.core.theme.SurfaceTint
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.core.theme.Warning
import com.kapos.mobile.core.utils.CurrencyFormatter
import com.kapos.mobile.domain.model.BatchFreshness
import com.kapos.mobile.domain.model.MaterialBatch
import com.kapos.mobile.domain.model.RawMaterial
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchDetailDialog(
    material: RawMaterial,
    batches: List<MaterialBatch>,
    onToggleUsable: (batchId: String, isUsable: Boolean) -> Unit,
    onOpenWaste: (batch: MaterialBatch) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
    val now = remember { System.currentTimeMillis() }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.Lg)
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, AppShapes.Lg)
                .padding(20.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PrimarySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Inventory, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Rincian Batch: ${material.name}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Total Stok: ${material.currentStock} ${material.unit} • Biaya: ${CurrencyFormatter.formatRupiah(material.costPerUnit)}/${material.unit}",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Daftar Batch Pengiriman (${batches.size})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Primary)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (batches.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppShapes.Md)
                            .background(SurfaceTint)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada riwayat batch untuk bahan ini.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(batches, key = { it.id }) { batch ->
                            val freshness = batch.freshnessStatus(now)
                            val badgeColor = when (freshness) {
                                BatchFreshness.FRESH -> Success
                                BatchFreshness.EXPIRING_SOON -> Warning
                                BatchFreshness.EXPIRED -> Danger
                                BatchFreshness.DAMAGED -> Danger
                            }
                            val badgeBg = when (freshness) {
                                BatchFreshness.FRESH -> Color(0xFFD1FAE5)
                                BatchFreshness.EXPIRING_SOON -> Color(0xFFFEF3C7)
                                BatchFreshness.EXPIRED -> Color(0xFFFEE2E2)
                                BatchFreshness.DAMAGED -> Color(0xFFFEE2E2)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceTint)
                                    .border(1.dp, OutlineSoft, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = batch.batchNumber,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                        )
                                        // Freshness Badge
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(badgeBg)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = freshness.displayName,
                                                style = MaterialTheme.typography.labelSmall.copy(color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Sisa: ${batch.remainingQuantity} / ${batch.initialQuantity} ${material.unit}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            Text(
                                                text = "Harga Beli: ${CurrencyFormatter.formatRupiah(batch.costPerUnit)}/${material.unit}",
                                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Masuk: ${dateFormat.format(Date(batch.receivedDate))}",
                                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                            )
                                            Text(
                                                text = if (batch.expiryDate != null) "Exp: ${dateFormat.format(Date(batch.expiryDate))}" else "Non-perishable",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (batch.expiryDate != null && batch.expiryDate <= now) Danger else TextSecondary,
                                                    fontWeight = if (batch.expiryDate != null && batch.expiryDate <= now) FontWeight.Bold else FontWeight.Normal
                                                )
                                            )
                                        }
                                    }

                                    if (batch.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Ket: ${batch.notes}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Action buttons for batch
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Usable toggle
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (batch.isUsable) SurfaceWhite else Color(0xFFFEE2E2))
                                                .border(1.dp, if (batch.isUsable) OutlineSoft else Danger, RoundedCornerShape(6.dp))
                                                .clickable { onToggleUsable(batch.id, !batch.isUsable) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (batch.isUsable) "Tandai Rusak" else "Tandai Layak",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (batch.isUsable) TextSecondary else Danger,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Waste button
                                        if (batch.remainingQuantity > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFFEE2E2))
                                                    .clickable { onOpenWaste(batch) }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Danger, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Catat Waste",
                                                        style = MaterialTheme.typography.labelSmall.copy(color = Danger, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SecondaryButton(
                    text = "Tutup",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteBatchDialog(
    material: RawMaterial,
    batch: MaterialBatch,
    onConfirm: (batchId: String, quantity: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var quantityStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val qty = quantityStr.toDoubleOrNull() ?: 0.0
    val estimatedLoss = qty * batch.costPerUnit

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.Lg)
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, AppShapes.Lg)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Danger, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Pemusnahan / Waste Batch", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(text = "${material.name} • ${batch.batchNumber}", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Sisa stok di batch ini: ${batch.remainingQuantity} ${material.unit} (Harga: ${CurrencyFormatter.formatRupiah(batch.costPerUnit)}/${material.unit})",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = {
                        quantityStr = it.filter { c -> c.isDigit() || c == '.' }
                        errorMessage = null
                    },
                    label = { Text("Jumlah Dibuang (${material.unit}) *") },
                    placeholder = { Text("Maks: ${batch.remainingQuantity}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Danger, unfocusedBorderColor = OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Alasan Pemusnahan *") },
                    placeholder = { Text("Contoh: Kadaluarsa / Basi / Tumpah") },
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Danger, unfocusedBorderColor = OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                )

                if (estimatedLoss > 0.0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppShapes.Sm)
                            .background(Color(0xFFFEE2E2))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Estimasi Nilai Kerugian: ${CurrencyFormatter.formatRupiah(estimatedLoss)}",
                            style = MaterialTheme.typography.labelMedium.copy(color = Danger, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage!!, style = MaterialTheme.typography.bodySmall.copy(color = Danger))
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(text = "Batal", onClick = onDismiss, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "Catat Waste",
                        onClick = {
                            if (qty <= 0) {
                                errorMessage = "Jumlah harus lebih dari 0!"
                            } else if (qty > batch.remainingQuantity) {
                                errorMessage = "Jumlah melebihi sisa stok batch (${batch.remainingQuantity})!"
                            } else if (notes.isBlank()) {
                                errorMessage = "Masukkan alasan pemusnahan!"
                            } else {
                                onConfirm(batch.id, qty, notes.trim())
                            }
                        },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}
