package com.kapos.mobile.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kapos.mobile.core.theme.Primary
import com.kapos.mobile.core.theme.ScreenBackground
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    initialStartDate: Long? = null,
    initialEndDate: Long? = null,
    onConfirm: (startMillis: Long, endMillis: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDate,
        initialSelectedEndDateMillis = initialEndDate ?: initialStartDate
    )

    val hasSelection = dateRangePickerState.selectedStartDateMillis != null

    val handleConfirm = {
        val start = dateRangePickerState.selectedStartDateMillis
        val end = dateRangePickerState.selectedEndDateMillis ?: start
        if (start != null) {
            onConfirm(start, end ?: start)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            containerColor = SurfaceWhite,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Pilih Rentang Tanggal",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = TextPrimary
                            )
                        }
                    },
                    actions = {
                        Button(
                            onClick = handleConfirm,
                            enabled = hasSelection,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                disabledContainerColor = Primary.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "OKE",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
                )
            },
            bottomBar = {
                Surface(
                    color = SurfaceWhite,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Batal", color = TextSecondary)
                        }
                        Button(
                            onClick = handleConfirm,
                            enabled = hasSelection,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                disabledContainerColor = Primary.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "OKE / Terapkan",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            DateRangePicker(
                state = dateRangePickerState,
                title = null,
                headline = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
                    val headlineText = when {
                        start != null && end != null -> "${sdf.format(Date(start))} - ${sdf.format(Date(end))}"
                        start != null -> "${sdf.format(Date(start))} - (Pilih tanggal selesai)"
                        else -> "Pilih tanggal mulai dan tanggal selesai"
                    }
                    Text(
                        text = headlineText,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Primary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                },
                showModeToggle = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = Primary,
                    dayInSelectionRangeContainerColor = Primary.copy(alpha = 0.2f)
                )
            )
        }
    }
}
