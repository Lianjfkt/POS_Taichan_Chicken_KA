package com.kapos.mobile.feature.menu

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.kapos.mobile.core.utils.CurrencyFormatter
import com.kapos.mobile.domain.model.Product
import com.kapos.mobile.domain.model.RawMaterial
import com.kapos.mobile.domain.model.RecipeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeBomDialog(
    product: Product,
    recipes: List<RecipeItem>,
    allMaterials: List<RawMaterial>,
    onAddRecipeItem: (materialId: String, quantity: Double) -> Unit,
    onDeleteRecipeItem: (recipeId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMaterial by remember(allMaterials) {
        val existingIds = recipes.map { it.materialId }.toSet()
        mutableStateOf(allMaterials.firstOrNull { it.id !in existingIds } ?: allMaterials.firstOrNull())
    }
    var quantityText by remember { mutableStateOf("") }
    var isMaterialDropdownExpanded by remember { mutableStateOf(false) }
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
                            Text(text = product.emoji, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Resep BOM: ${product.name}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Bahan otomatis terpotong saat menu terjual",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // List of Recipe Items
                Text(
                    text = "Daftar Bahan (${recipes.size})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Primary)
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (recipes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppShapes.Md)
                            .background(SurfaceTint)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada resep bahan baku untuk produk ini.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                } else {
                    val totalBop = recipes.sumOf { r ->
                        val mat = allMaterials.find { it.id == r.materialId }
                        r.quantityRequired * (mat?.costPerUnit ?: 0.0)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(recipes, key = { it.id }) { item ->
                            val mat = allMaterials.find { it.id == item.materialId }
                            val lineCost = item.quantityRequired * (mat?.costPerUnit ?: 0.0)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceTint)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Inventory,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = item.materialName,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        if (lineCost > 0) {
                                            Text(
                                                text = "≈ ${CurrencyFormatter.formatRupiah(lineCost)}",
                                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                                            )
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val formattedQty = if (item.quantityRequired % 1.0 == 0.0) {
                                        item.quantityRequired.toInt().toString()
                                    } else {
                                        String.format("%.2f", item.quantityRequired)
                                    }
                                    Text(
                                        text = "$formattedQty ${item.unit} / porsi",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Primary, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { onDeleteRecipeItem(item.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Danger, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    if (totalBop > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppShapes.Sm)
                                .background(Color(0xFFDCFCE7))
                                .border(1.dp, Color(0xFF86EFAC), AppShapes.Sm)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Estimasi BOP / Porsi:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(totalBop),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Form to add ingredient
                Text(
                    text = "Tambah Bahan Baru",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (allMaterials.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = isMaterialDropdownExpanded,
                        onExpandedChange = { isMaterialDropdownExpanded = !isMaterialDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedMaterial?.let { "${it.name} (${it.unit})" } ?: "Pilih Bahan Baku",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMaterialDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OutlineSoft
                            ),
                            shape = AppShapes.Sm
                        )
                        ExposedDropdownMenu(
                            expanded = isMaterialDropdownExpanded,
                            onDismissRequest = { isMaterialDropdownExpanded = false }
                        ) {
                            allMaterials.forEach { mat ->
                                DropdownMenuItem(
                                    text = { Text("${mat.name} (${mat.unit})") },
                                    onClick = {
                                        selectedMaterial = mat
                                        isMaterialDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = {
                                quantityText = it
                                errorMessage = null
                            },
                            label = { Text("Kebutuhan per porsi (${selectedMaterial?.unit ?: ""})") },
                            placeholder = { Text("Contoh: 150") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = AppShapes.Sm,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OutlineSoft
                            )
                        )

                        Box(
                            modifier = Modifier
                                .clip(AppShapes.Sm)
                                .background(Primary)
                                .clickable {
                                    val mat = selectedMaterial
                                    val qty = quantityText.toDoubleOrNull()
                                    if (mat == null) {
                                        errorMessage = "Pilih bahan baku terlebih dahulu"
                                    } else if (qty == null || qty <= 0) {
                                        errorMessage = "Masukkan jumlah yang valid (> 0)"
                                    } else {
                                        onAddRecipeItem(mat.id, qty)
                                        quantityText = ""
                                        errorMessage = null
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = SurfaceWhite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tambah",
                                    style = MaterialTheme.typography.labelMedium.copy(color = SurfaceWhite, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(color = Danger)
                        )
                    }
                } else {
                    Text(
                        text = "Belum ada master bahan baku. Silakan buat di Tab Bahan Baku terlebih dahulu.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SecondaryButton(
                    text = "Selesai",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
