package com.kapos.mobile.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapos.mobile.core.components.BadgeType
import com.kapos.mobile.core.components.DateFilterBar
import com.kapos.mobile.core.components.DateRangePickerDialog
import com.kapos.mobile.core.components.SearchField
import com.kapos.mobile.core.components.StatusBadge
import com.kapos.mobile.domain.model.DateFilterPreset
import com.kapos.mobile.core.theme.AppShapes
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
import com.kapos.mobile.domain.model.Order
import com.kapos.mobile.domain.model.PaymentStatus
import com.kapos.mobile.domain.model.SyncStatus
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.kapos.mobile.core.components.PrimaryButton
import com.kapos.mobile.core.components.SecondaryButton
import com.kapos.mobile.core.theme.Danger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionsRoute(
    viewModel: TransactionsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    TransactionsScreen(
        uiState = uiState,
        onSearchChange = viewModel::onSearchQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onDatePresetChange = viewModel::onDatePresetChange,
        onCustomDateRangeSelected = viewModel::onCustomDateRangeSelected,
        onShowDatePickerDialog = viewModel::onShowDatePickerDialog,
        onSelectOrder = viewModel::onSelectOrder,
        onDismissDetail = viewModel::onDismissDetail,
        onPrintOrder = viewModel::printOrder,
        onSyncClick = viewModel::syncPending,
        onVoidClick = viewModel::onShowVoidPinDialog,
        onDismissVoidPin = viewModel::onDismissVoidPinDialog,
        onSelectVoidReason = viewModel::onSelectVoidReason,
        onVoidReasonNotesChange = viewModel::onVoidReasonNotesChange,
        onVoidPinChange = viewModel::onVoidPinInputChange,
        onConfirmVoid = viewModel::confirmVoid,
        modifier = modifier
    )
}

@Composable
fun TransactionsScreen(
    uiState: TransactionsUiState,
    onSearchChange: (String) -> Unit,
    onFilterChange: (TransactionFilter) -> Unit,
    onDatePresetChange: (com.kapos.mobile.domain.model.DateFilterPreset) -> Unit,
    onCustomDateRangeSelected: (Long, Long) -> Unit,
    onShowDatePickerDialog: (Boolean) -> Unit,
    onSelectOrder: (Order) -> Unit,
    onDismissDetail: () -> Unit,
    onPrintOrder: (Order, List<com.kapos.mobile.domain.model.OrderItem>) -> Unit,
    onSyncClick: () -> Unit,
    onVoidClick: (Order) -> Unit,
    onDismissVoidPin: () -> Unit,
    onSelectVoidReason: (String) -> Unit,
    onVoidReasonNotesChange: (String) -> Unit,
    onVoidPinChange: (String) -> Unit,
    onConfirmVoid: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Riwayat Transaksi",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "${uiState.orders.size} Total Transaksi",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                IconButton(
                    onClick = onSyncClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimarySoft)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Search
            SearchField(
                query = uiState.searchQuery,
                onQueryChange = onSearchChange,
                placeholder = "Cari nomor nota atau pelanggan...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Date Filter Bar
            DateFilterBar(
                currentFilter = uiState.dateFilter,
                onPresetSelected = onDatePresetChange,
                onOpenDatePicker = { onShowDatePickerDialog(true) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipItem(
                    label = "Semua",
                    isSelected = uiState.filter == TransactionFilter.ALL,
                    onClick = { onFilterChange(TransactionFilter.ALL) }
                )
                FilterChipItem(
                    label = "Lunas",
                    isSelected = uiState.filter == TransactionFilter.PAID,
                    onClick = { onFilterChange(TransactionFilter.PAID) }
                )
                FilterChipItem(
                    label = "Pending",
                    isSelected = uiState.filter == TransactionFilter.UNPAID,
                    onClick = { onFilterChange(TransactionFilter.UNPAID) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transactions List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.filteredOrders, key = { it.id }) { order ->
                    TransactionCard(
                        order = order,
                        onClick = { onSelectOrder(order) }
                    )
                }
            }
        }

        // Receipt Modal Dialog
        if (uiState.selectedOrder != null) {
            OrderDetailDialog(
                order = uiState.selectedOrder,
                items = uiState.selectedOrderItems,
                isLoading = uiState.isLoadingDetail,
                storeName = uiState.storeName,
                storeAddress = uiState.storeAddress,
                onPrint = { onPrintOrder(uiState.selectedOrder, uiState.selectedOrderItems) },
                onVoid = { onVoidClick(uiState.selectedOrder) },
                onDismiss = onDismissDetail
            )
        }

        // VOID PIN Dialog
        if (uiState.showVoidPinDialog && uiState.orderToVoid != null) {
            VoidPinDialog(
                orderNo = uiState.orderToVoid.orderNo,
                selectedReason = uiState.selectedVoidReason,
                reasonNotes = uiState.voidReasonNotes,
                reasonError = uiState.voidReasonError,
                pin = uiState.voidPinInput,
                errorMessage = uiState.voidPinError,
                isLoading = uiState.isVoiding,
                onSelectReason = onSelectVoidReason,
                onReasonNotesChange = onVoidReasonNotesChange,
                onPinChange = onVoidPinChange,
                onConfirm = onConfirmVoid,
                onDismiss = onDismissVoidPin
            )
        }

        // Custom Date Range Picker Dialog
        if (uiState.showDatePickerDialog) {
            DateRangePickerDialog(
                initialStartDate = uiState.dateFilter.customStartMillis,
                initialEndDate = uiState.dateFilter.customEndMillis,
                onConfirm = { start, end ->
                    onCustomDateRangeSelected(start, end)
                },
                onDismiss = { onShowDatePickerDialog(false) }
            )
        }
    }
}

@Composable
fun TransactionCard(
    order: Order,
    onClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm • dd MMM", Locale("id", "ID"))
    val formattedTime = timeFormat.format(Date(order.createdAt))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.Md)
            .background(SurfaceWhite)
            .border(1.dp, OutlineSoft, AppShapes.Md)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.orderNo,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (order.syncStatus == SyncStatus.PENDING_SYNC) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Pending Sync",
                            tint = Warning,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Synced",
                            tint = Success,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${order.orderType.displayName} • $formattedTime",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
                if (order.customerName != null) {
                    Text(
                        text = "Pelanggan: ${order.customerName}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Primary)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatRupiah(order.total),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        fontSize = 15.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(
                    text = order.paymentStatus.displayName,
                    type = if (order.paymentStatus == PaymentStatus.PAID) BadgeType.SUCCESS else BadgeType.DANGER
                )
            }
        }
    }
}

@Composable
fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) Primary else SurfaceWhite)
            .border(1.dp, if (isSelected) Primary else OutlineSoft, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) SurfaceWhite else TextSecondary
            )
        )
    }
}

@Composable
fun VoidPinDialog(
    orderNo: String,
    selectedReason: String,
    reasonNotes: String,
    reasonError: String?,
    pin: String,
    errorMessage: String?,
    isLoading: Boolean,
    onSelectReason: (String) -> Unit,
    onReasonNotesChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val predefinedReasons = listOf(
        "Salah Input Menu",
        "Pelanggan Membatalkan",
        "Meja Pindah",
        "Komplain Makanan",
        "Lainnya"
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Danger.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security",
                        tint = Danger,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Otorisasi VOID Transaksi",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Batalkan nota $orderNo",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                // Alasan Pembatalan
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Alasan Pembatalan (Wajib):",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    predefinedReasons.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            pair.forEach { reason ->
                                val isSelected = selectedReason == reason
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(AppShapes.Sm)
                                        .background(if (isSelected) Danger.copy(alpha = 0.12f) else SurfaceTint)
                                        .border(1.dp, if (isSelected) Danger else OutlineSoft, AppShapes.Sm)
                                        .clickable { onSelectReason(reason) }
                                        .padding(vertical = 6.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = reason,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) Danger else TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (selectedReason == "Lainnya") {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = reasonNotes,
                            onValueChange = onReasonNotesChange,
                            placeholder = { Text("Tuliskan alasan spesifik...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppShapes.Sm,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Danger,
                                unfocusedBorderColor = OutlineSoft
                            )
                        )
                    }

                    if (reasonError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = reasonError,
                            style = MaterialTheme.typography.labelSmall.copy(color = Danger)
                        )
                    }
                }

                // PIN Input
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "PIN Owner (Default: 1234):",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = onPinChange,
                        singleLine = true,
                        placeholder = { Text("••••", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 8.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (errorMessage != null) Danger else Primary,
                            unfocusedBorderColor = if (errorMessage != null) Danger else OutlineSoft
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Danger,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryButton(
                        text = "Batal",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = "VOID Nota",
                        onClick = onConfirm,
                        isLoading = isLoading,
                        enabled = pin.length >= 4 && !isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

