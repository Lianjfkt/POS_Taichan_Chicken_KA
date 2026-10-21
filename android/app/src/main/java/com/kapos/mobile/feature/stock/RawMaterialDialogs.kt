package com.kapos.mobile.feature.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Remove
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
import com.kapos.mobile.core.theme.SurfaceTint
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.core.theme.Warning
import com.kapos.mobile.core.utils.CurrencyFormatter
import com.kapos.mobile.domain.model.RawMaterial
import com.kapos.mobile.domain.model.RawMaterialCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMaterialDialog(
    material: RawMaterial?,
    onSave: (name: String, category: RawMaterialCategory, unit: String, currentStock: Double, minStock: Double, costPerUnit: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(material?.name ?: "") }
    var selectedCategory by remember { mutableStateOf(material?.category ?: RawMaterialCategory.BUMBU_SAMBAL) }
    var unit by remember { mutableStateOf(material?.unit ?: "gram") }
    var currentStockStr by remember { mutableStateOf(if (material != null) material.currentStock.toString() else "0") }
    var minStockStr by remember { mutableStateOf(if (material != null) material.minStock.toString() else "100") }
    var costPerUnitStr by remember { mutableStateOf(if (material != null) material.costPerUnit.toString() else "0") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val commonUnits = listOf("gram", "kg", "pcs", "buah", "ml", "liter", "potong", "botol", "porsi")

    BasicAlertDialog(onDismissRequest = onDismiss) {
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.Lg)
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, AppShapes.Lg)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (material == null) "Tambah Bahan Baku Baru" else "Edit Bahan Baku",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Nama Bahan Baku *") },
                    placeholder = { Text("Contoh: Cabe Rawit Merah") },
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Kategori Bahan Baku
                Text(text = "Kategori Bahan", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RawMaterialCategory.values().forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) PrimarySoft else SurfaceTint)
                                .border(1.dp, if (isSelected) Primary else OutlineSoft, CircleShape)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Primary else TextPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Satuan Unit
                Text(text = "Satuan Ukur", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonUnits.forEach { u ->
                        val isSelected = unit.equals(u, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) PrimarySoft else SurfaceTint)
                                .border(1.dp, if (isSelected) Primary else OutlineSoft, CircleShape)
                                .clickable { unit = u }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = u,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Primary else TextPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stok Saat Ini & Min Stok
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentStockStr,
                        onValueChange = { currentStockStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Stok Sekarang ($unit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = AppShapes.Sm,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minStockStr,
                        onValueChange = { minStockStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Batas Minimum ($unit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = AppShapes.Sm,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = costPerUnitStr,
                    onValueChange = { costPerUnitStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Harga Modal per $unit (HPP)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage!!, style = MaterialTheme.typography.bodySmall.copy(color = Danger))
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(text = "Batal", onClick = onDismiss, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "Simpan",
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Nama bahan baku wajib diisi!"
                            } else {
                                val current = currentStockStr.toDoubleOrNull() ?: 0.0
                                val min = minStockStr.toDoubleOrNull() ?: 0.0
                                val cost = costPerUnitStr.toDoubleOrNull() ?: 0.0
                                onSave(name, selectedCategory, unit, current, min, cost)
                            }
                        },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockMaterialDialog(
    material: RawMaterial,
    onConfirm: (quantity: Double, cost: Double, expiryDate: Long?, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var quantityStr by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val now = remember { System.currentTimeMillis() }
    val oneDay = 24L * 60 * 60 * 1000
    // Expiry chips: null for non-perishable, or timestamp
    var selectedExpiryDays by remember { mutableStateOf<Int?>(7) } // default 7 days

    val qty = quantityStr.toDoubleOrNull() ?: 0.0
    val cost = costStr.toDoubleOrNull() ?: 0.0
    val unitCostPreview = if (qty > 0 && cost > 0) cost / qty else null

    BasicAlertDialog(onDismissRequest = onDismiss) {
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.Lg)
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, AppShapes.Lg)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimarySoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Catat Stok Masuk (Restock)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(text = "${material.name} (Stok: ${material.currentStock} ${material.unit})", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it.filter { c -> c.isDigit() || c == '.' }; errorMessage = null },
                    label = { Text("Jumlah Masuk (${material.unit}) *") },
                    placeholder = { Text("Contoh: 1000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Total Biaya Pembelian (Rp)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                )

                if (unitCostPreview != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "≈ ${CurrencyFormatter.format(unitCostPreview)} / ${material.unit}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Primary, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Expiry selector
                Text(
                    text = "Masa Simpan / Kadaluarsa",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val options = listOf(
                        Pair("3 Hari", 3),
                        Pair("7 Hari", 7),
                        Pair("14 Hari", 14),
                        Pair("30 Hari", 30),
                        Pair("Non-perishable", null)
                    )

                    options.forEach { (label, days) ->
                        val isSelected = selectedExpiryDays == days
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimarySoft else SurfaceTint)
                                .border(1.dp, if (isSelected) Primary else OutlineSoft, RoundedCornerShape(8.dp))
                                .clickable { selectedExpiryDays = days }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) Primary else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan / Supplier (Opsional)") },
                    placeholder = { Text("Contoh: Pasar Induk / Agen Cabe") },
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage!!, style = MaterialTheme.typography.bodySmall.copy(color = Danger))
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(text = "Batal", onClick = onDismiss, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "Tambah Stok",
                        onClick = {
                            if (qty <= 0) {
                                errorMessage = "Jumlah masuk harus lebih dari 0!"
                            } else {
                                val expiryTimestamp = selectedExpiryDays?.let { now + (it * oneDay) }
                                onConfirm(qty, cost, expiryTimestamp, notes)
                            }
                        },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteMaterialDialog(
    material: RawMaterial,
    onConfirm: (quantity: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var quantityStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                            .background(Color(0xFFFDE4E5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = Danger, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Catat Stok Rusak / Waste", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(text = "${material.name} (Tersedia: ${material.currentStock} ${material.unit})", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it.filter { c -> c.isDigit() || c == '.' }; errorMessage = null },
                    label = { Text("Jumlah Rusak / Terbuang (${material.unit}) *") },
                    placeholder = { Text("Contoh: 100") },
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
                    label = { Text("Alasan Kerusakan (Opsional)") },
                    placeholder = { Text("Contoh: Basi / Busuk / Tumpah saat masak") },
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage!!, style = MaterialTheme.typography.bodySmall.copy(color = Danger))
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(text = "Batal", onClick = onDismiss, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .clip(AppShapes.Sm)
                            .background(Danger)
                            .clickable {
                                val qty = quantityStr.toDoubleOrNull() ?: 0.0
                                if (qty <= 0) {
                                    errorMessage = "Jumlah rusak harus lebih dari 0!"
                                } else {
                                    onConfirm(qty, notes)
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Potong Stok Rusak", style = MaterialTheme.typography.labelMedium.copy(color = SurfaceWhite, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpnameMaterialDialog(
    material: RawMaterial,
    onConfirm: (newStock: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newStockStr by remember { mutableStateOf(material.currentStock.toString()) }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val newStock = newStockStr.toDoubleOrNull() ?: 0.0
    val diff = newStock - material.currentStock

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
                Text(text = "Stok Opname (Penyesuaian Fisik)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(text = "${material.name} • Sistem saat ini: ${material.currentStock} ${material.unit}", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = newStockStr,
                    onValueChange = { newStockStr = it.filter { c -> c.isDigit() || c == '.' }; errorMessage = null },
                    label = { Text("Hasil Hitung Fisik Aktual (${material.unit}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Selisih Stok Opname:", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    Text(
                        text = if (diff >= 0) "+$diff ${material.unit}" else "$diff ${material.unit}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (diff >= 0) Primary else Danger
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Keterangan Opname (Opsional)") },
                    placeholder = { Text("Contoh: Opname tutup toko harian") },
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage!!, style = MaterialTheme.typography.bodySmall.copy(color = Danger))
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(text = "Batal", onClick = onDismiss, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "Simpan Penyesuaian",
                        onClick = {
                            if (newStock < 0) {
                                errorMessage = "Stok fisik tidak boleh negatif!"
                            } else {
                                onConfirm(newStock, notes)
                            }
                        },
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }
        }
    }
}
