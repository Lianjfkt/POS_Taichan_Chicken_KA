package com.kapos.mobile.feature.transactions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kapos.mobile.core.components.PrimaryButton
import com.kapos.mobile.core.components.SecondaryButton
import com.kapos.mobile.core.theme.AppShapes
import com.kapos.mobile.core.theme.OutlineSoft
import com.kapos.mobile.core.theme.Primary
import com.kapos.mobile.core.theme.PrimarySoft
import com.kapos.mobile.core.theme.ScreenBackground
import com.kapos.mobile.core.theme.SurfaceTint
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.core.utils.CurrencyFormatter
import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.Product
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun BackdatedOrderDialog(
    availableProducts: List<Product>,
    isSubmitting: Boolean,
    onSubmit: (timestampMillis: Long, items: List<CartItem>, paymentMethod: PaymentMethod, reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()

    // Default timestamp: 1 hari yang lalu pada jam yang sama
    var selectedTimestamp by remember {
        mutableLongStateOf(now - 86400000L)
    }

    val selectedQuantities = remember { mutableStateMapOf<String, Int>() }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var reasonText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val dateFormat = remember {
        SimpleDateFormat("EEE, dd MMM yyyy, HH:mm", Locale("id", "ID"))
    }

    // Filter produk berdasarkan query pencarian
    val filteredProducts = remember(availableProducts, searchQuery) {
        if (searchQuery.isBlank()) {
            availableProducts
        } else {
            val q = searchQuery.trim().lowercase()
            availableProducts.filter { it.name.lowercase().contains(q) }
        }
    }

    // Hitung item yang dipilih
    val selectedCartItems = remember(availableProducts, selectedQuantities.toMap()) {
        availableProducts.mapNotNull { prod ->
            val qty = selectedQuantities[prod.id] ?: 0
            if (qty > 0) {
                CartItem(product = prod, quantity = qty)
            } else null
        }
    }

    val totalAmount = selectedCartItems.sumOf { it.subtotal }
    val totalItemCount = selectedCartItems.sumOf { it.quantity }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(AppShapes.Lg)
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, AppShapes.Lg)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HistoryEdu,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Transaksi Susulan (Owner)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Catat transaksi masa lampau yang terlewat",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = OutlineSoft)
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Pemilihan Tanggal & Waktu Lampau
                    item {
                        Column {
                            Text(
                                text = "Tanggal & Waktu Transaksi (Masa Lampau):",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ScreenBackground)
                                    .border(1.dp, OutlineSoft, RoundedCornerShape(10.dp))
                                    .clickable {
                                        val cal = Calendar.getInstance().apply {
                                            timeInMillis = selectedTimestamp
                                        }
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                cal.set(Calendar.YEAR, year)
                                                cal.set(Calendar.MONTH, month)
                                                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                                                TimePickerDialog(
                                                    context,
                                                    { _, hourOfDay, minute ->
                                                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                        cal.set(Calendar.MINUTE, minute)
                                                        cal.set(Calendar.SECOND, 0)
                                                        val chosen = cal.timeInMillis
                                                        // Cegah memilih waktu masa depan
                                                        selectedTimestamp = if (chosen > System.currentTimeMillis()) {
                                                            System.currentTimeMillis()
                                                        } else {
                                                            chosen
                                                        }
                                                    },
                                                    cal.get(Calendar.HOUR_OF_DAY),
                                                    cal.get(Calendar.MINUTE),
                                                    true
                                                ).show()
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).apply {
                                            datePicker.maxDate = System.currentTimeMillis()
                                        }.show()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = dateFormat.format(Date(selectedTimestamp)),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = TextPrimary
                                            )
                                        )
                                    }

                                    Text(
                                        text = "Ubah",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Primary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 2. Pilih Menu Produk
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pilih Produk yang Terjual:",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                                if (totalItemCount > 0) {
                                    Text(
                                        text = "$totalItemCount item dipilih",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Primary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text("Cari nama produk...", style = MaterialTheme.typography.bodySmall)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = OutlineSoft
                                )
                            )
                        }
                    }

                    // Daftar Produk
                    items(filteredProducts) { product ->
                        val qty = selectedQuantities[product.id] ?: 0
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                                containerColor = if (qty > 0) SurfaceTint.copy(alpha = 0.5f) else ScreenBackground
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${product.emoji} ${product.name}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(product.price),
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                    )
                                }

                                // Stepper Kuantitas
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (qty > 0) {
                                                selectedQuantities[product.id] = qty - 1
                                            }
                                        },
                                        enabled = qty > 0,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (qty > 0) SurfaceWhite else Color.Transparent)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Kurang",
                                            tint = if (qty > 0) TextPrimary else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Text(
                                        text = "$qty",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (qty > 0) Primary else TextSecondary
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    )

                                    IconButton(
                                        onClick = {
                                            selectedQuantities[product.id] = qty + 1
                                        },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(PrimarySoft)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Tambah",
                                            tint = Primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Metode Pembayaran
                    item {
                        Column {
                            Text(
                                text = "Metode Pembayaran:",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(PaymentMethod.CASH, PaymentMethod.QRIS, PaymentMethod.TRANSFER).forEach { method ->
                                    val isSelected = selectedPaymentMethod == method
                                    FilledTonalButton(
                                        onClick = { selectedPaymentMethod = method },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (isSelected) Primary else ScreenBackground,
                                            contentColor = if (isSelected) Color.White else TextPrimary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = method.displayName,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Catatan / Alasan Koreksi (Wajib)
                    item {
                        Column {
                            Text(
                                text = "Alasan / Catatan Koreksi (Wajib):",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = reasonText,
                                onValueChange = { reasonText = it },
                                placeholder = {
                                    Text("Contoh: Struk manual tgl 2 Nov, kasir lupa input saat listrik padam", style = MaterialTheme.typography.bodySmall)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = OutlineSoft
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = OutlineSoft)
                Spacer(modifier = Modifier.height(10.dp))

                // Total & Tombol Aksi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Transaksi:",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(totalAmount),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton(
                            text = "Batal",
                            onClick = onDismiss
                        )
                        PrimaryButton(
                            text = "Simpan Nota",
                            onClick = {
                                onSubmit(
                                    selectedTimestamp,
                                    selectedCartItems,
                                    selectedPaymentMethod,
                                    reasonText
                                )
                            },
                            isLoading = isSubmitting,
                            enabled = selectedCartItems.isNotEmpty() && reasonText.isNotBlank() && !isSubmitting
                        )
                    }
                }
            }
        }
    }
}
