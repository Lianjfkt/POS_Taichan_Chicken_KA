package com.kapos.mobile.feature.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
import com.kapos.mobile.core.utils.CurrencyFormatter
import com.kapos.mobile.domain.model.ModifierCategory
import com.kapos.mobile.domain.model.ModifierOption
import com.kapos.mobile.domain.model.PresetModifiers
import com.kapos.mobile.domain.model.Product

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModifierDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirmAddToCart: (
        product: Product,
        quantity: Int,
        spiceLevel: String,
        selectedModifiers: List<ModifierOption>,
        notes: String
    ) -> Unit
) {
    val spiceOptions = listOf("Original", "Lv.1", "Lv.2", "Lv.3", "Lv.4", "Lv.5")
    var selectedSpice by remember {
        mutableStateOf(if (product.spiceLevel.isNotBlank()) product.spiceLevel else "Original")
    }

    val defaultSambal = PresetModifiers.allOptions.firstOrNull { it.category == ModifierCategory.SAMBAL && it.isDefault }
    var selectedSambal by remember { mutableStateOf(defaultSambal) }

    // Multi-select for extras and complimentaries
    var selectedExtras by remember { mutableStateOf(setOf<ModifierOption>()) }
    var notes by remember { mutableStateOf("") }
    var quantity by remember { mutableIntStateOf(1) }

    val activeModifiers: List<ModifierOption> = buildList {
        selectedSambal?.let { add(it) }
        addAll(selectedExtras)
    }

    val unitPrice = product.price + activeModifiers.sumOf { it.price }
    val totalPrice = unitPrice * quantity

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
                    .heightIn(max = 580.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.formatRupiah(product.price),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = OutlineSoft)

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                // Level Pedas / Varian — hanya tampil jika produk mengizinkan
                    if (product.allowSpiceLevel) {
                        Text(
                            text = "Level Pedas / Varian",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            spiceOptions.forEach { option ->
                                SelectableTag(
                                    text = option,
                                    isSelected = selectedSpice == option,
                                    onClick = { selectedSpice = option }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Pilihan Sambal — hanya tampil jika produk mengizinkan
                    if (product.allowSambal) {
                        Text(
                            text = "Pilihan Sambal",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PresetModifiers.allOptions
                                .filter { it.category == ModifierCategory.SAMBAL }
                                .forEach { option ->
                                    SelectableTag(
                                        text = option.name,
                                        isSelected = selectedSambal?.id == option.id,
                                        onClick = {
                                            selectedSambal = if (selectedSambal?.id == option.id) null else option
                                        }
                                    )
                                }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Pelengkap & Alat Makan — hanya tampil jika produk mengizinkan
                    if (product.allowComplimentary) {
                        Text(
                            text = "Pelengkap & Alat Makan (Gratis)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PresetModifiers.allOptions
                                .filter { it.category == ModifierCategory.COMPLIMENTARY }
                                .forEach { option ->
                                    val isSelected = selectedExtras.contains(option)
                                    SelectableTag(
                                        text = option.name,
                                        isSelected = isSelected,
                                        onClick = {
                                            selectedExtras = if (isSelected) {
                                                selectedExtras - option
                                            } else {
                                                selectedExtras + option
                                            }
                                        }
                                    )
                                }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Ekstra Topping & Kemasan — hanya tampil jika produk mengizinkan
                    if (product.allowToppings) {
                        Text(
                            text = "Ekstra Topping & Kemasan",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PresetModifiers.allOptions
                                .filter { it.category == ModifierCategory.EXTRA || it.category == ModifierCategory.PACKAGING }
                                .forEach { option ->
                                    val isSelected = selectedExtras.contains(option)
                                    SelectableTag(
                                        text = option.name,
                                        isSelected = isSelected,
                                        onClick = {
                                            selectedExtras = if (isSelected) {
                                                selectedExtras - option
                                            } else {
                                                selectedExtras + option
                                            }
                                        }
                                    )
                                }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Catatan Khusus
                    Text(
                        text = "Catatan Khusus",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Contoh: sambal dipisah, ekstra gurih...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = AppShapes.Sm,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = OutlineSoft
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = OutlineSoft)
                Spacer(modifier = Modifier.height(12.dp))

                // Quantity & Submit Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceTint)
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Minus", tint = TextPrimary)
                        }
                        Text(text = "$quantity", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(
                            onClick = { quantity++ },
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(Primary)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Plus", tint = SurfaceWhite)
                        }
                    }

                    val btnLabel: String = "+ Keranjang (${CurrencyFormatter.formatRupiah(totalPrice)})"
                    PrimaryButton(
                        text = btnLabel,
                        onClick = {
                            onConfirmAddToCart(product, quantity, selectedSpice, activeModifiers, notes)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableTag(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(AppShapes.Sm)
            .background(if (isSelected) Primary else SurfaceTint)
            .border(1.dp, if (isSelected) Primary else OutlineSoft, AppShapes.Sm)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SurfaceWhite,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) SurfaceWhite else TextPrimary,
                    fontSize = 11.sp
                )
            )
        }
    }
}
