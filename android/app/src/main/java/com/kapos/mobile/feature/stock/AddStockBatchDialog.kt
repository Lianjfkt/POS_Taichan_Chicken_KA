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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kapos.mobile.core.components.PrimaryButton
import com.kapos.mobile.core.theme.OutlineSoft
import com.kapos.mobile.core.theme.Primary
import com.kapos.mobile.core.theme.PrimarySoft
import com.kapos.mobile.core.theme.SurfaceTint
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.domain.model.ChickenPart
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockBatchDialog(
    activeTab: StockTab,
    selectedChickenPart: ChickenPart,
    onDismiss: () -> Unit,
    onAddTaichan: (raw: Int, notes: String, receivedDate: Long) -> Unit,
    onAddChicken: (part: ChickenPart, raw: Int, notes: String, receivedDate: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var rawText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var currentPart by remember { mutableStateOf(selectedChickenPart) }

    val now = remember { System.currentTimeMillis() }
    var selectedReceivedDate by remember { mutableStateOf(now) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dayFormat = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")) }
    val isBackdated = remember(selectedReceivedDate) {
        dayFormat.format(Date(selectedReceivedDate)) != dayFormat.format(Date(now))
    }

    if (showDatePicker) {
        val utcToday = remember(now) {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }
        val utcMin = remember(now) {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -7)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedReceivedDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis in utcMin..utcToday
                }
                override fun isSelectableYear(year: Int): Boolean = true
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedUtc ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = selectedUtc
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                            }
                            selectedReceivedDate = localCal.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih", color = Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, RoundedCornerShape(16.dp))
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
                            text = if (activeTab == StockTab.TAICHAN) "Restock Sate Taichan" else "Restock Ayam Potong",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Catat batch mentah masuk",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tanggal Masuk Batch
                Text(
                    text = "Tanggal Masuk Batch",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceTint)
                        .border(
                            1.dp,
                            if (isBackdated) Primary else OutlineSoft,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = if (isBackdated) Primary else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateFormat.format(Date(selectedReceivedDate)),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        )
                    }
                    if (isBackdated) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimarySoft)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Lampau",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    } else {
                        Text(
                            text = "Hari Ini",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // If Chicken: Part Selector
                if (activeTab == StockTab.CHICKEN) {
                    Text(
                        text = "Bagian Ayam",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChickenPart.values().forEach { part ->
                            val isSelected = currentPart == part
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Primary else SurfaceTint)
                                    .border(1.dp, if (isSelected) Primary else OutlineSoft, RoundedCornerShape(8.dp))
                                    .clickable { currentPart = part }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = part.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) SurfaceWhite else TextPrimary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Input Jumlah Mentah
                Text(
                    text = if (activeTab == StockTab.TAICHAN) "Jumlah Tusuk Mentah" else "Jumlah Potong Mentah",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it.filter { ch -> ch.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Contoh: 100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Input Catatan
                Text(
                    text = "Catatan Batch (Opsional)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Misal: Batch pagi supplier A") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Tombol Simpan
                PrimaryButton(
                    text = "Simpan Batch Masuk",
                    onClick = {
                        val qty = rawText.toIntOrNull() ?: 0
                        if (qty > 0) {
                            if (activeTab == StockTab.TAICHAN) {
                                onAddTaichan(qty, notesText, selectedReceivedDate)
                            } else {
                                onAddChicken(currentPart, qty, notesText, selectedReceivedDate)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = (rawText.toIntOrNull() ?: 0) > 0
                )
            }
        }
    }
}
