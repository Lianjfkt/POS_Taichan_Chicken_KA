package com.kapos.mobile.feature.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.kapos.mobile.core.theme.ScreenBackground
import com.kapos.mobile.core.theme.Success
import com.kapos.mobile.core.theme.SurfaceTint
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.core.utils.CurrencyFormatter
import com.kapos.mobile.domain.model.OrderType
import com.kapos.mobile.domain.model.PaymentMethod

@Composable
fun ConfirmSaleRoute(
    viewModel: CheckoutViewModel,
    onBack: () -> Unit,
    onSuccessFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    ConfirmSaleScreen(
        uiState = uiState,
        onBack = onBack,
        onOrderTypeChange = viewModel::onOrderTypeChange,
        onTableNoChange = viewModel::onTableNoChange,
        onCustomerNameChange = viewModel::onCustomerNameChange,
        onDiscountTypeChange = viewModel::onDiscountTypeChange,
        onDiscountInputChange = viewModel::onDiscountInputChange,
        onPaymentMethodChange = viewModel::onPaymentMethodChange,
        onCashGivenChange = viewModel::onCashGivenChange,
        onNotesChange = viewModel::onNotesChange,
        onSubmitOrder = {
            viewModel.submitOrder {
                onSuccessFinish()
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmSaleScreen(
    uiState: CheckoutUiState,
    onBack: () -> Unit,
    onOrderTypeChange: (OrderType) -> Unit,
    onTableNoChange: (String) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onDiscountTypeChange: (com.kapos.mobile.domain.model.DiscountType) -> Unit,
    onDiscountInputChange: (Double) -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onCashGivenChange: (Double) -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmitOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Konfirmasi Penjualan",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = uiState.orderNo,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Order Type Selector
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.Md)
                        .background(SurfaceWhite)
                        .border(1.dp, OutlineSoft, AppShapes.Md)
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Tipe Pesanan",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OrderTypeChip(
                                label = "Dine-In (Makan di Tempat)",
                                isSelected = uiState.orderType == OrderType.DINE_IN,
                                onClick = { onOrderTypeChange(OrderType.DINE_IN) },
                                modifier = Modifier.weight(1f)
                            )
                            OrderTypeChip(
                                label = "Take-Away (Bungkus)",
                                isSelected = uiState.orderType == OrderType.TAKE_AWAY,
                                onClick = { onOrderTypeChange(OrderType.TAKE_AWAY) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (uiState.orderType == OrderType.DINE_IN) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = uiState.tableNo,
                                onValueChange = onTableNoChange,
                                label = { Text("Nomor Meja") },
                                singleLine = true,
                                shape = AppShapes.Sm,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = OutlineSoft
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.customerName,
                            onValueChange = onCustomerNameChange,
                            label = { Text("Nama Pelanggan (Opsional)") },
                            singleLine = true,
                            shape = AppShapes.Sm,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OutlineSoft
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Section 2: Items Summary Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.Md)
                        .background(SurfaceWhite)
                        .border(1.dp, OutlineSoft, AppShapes.Md)
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Item Pesanan (${uiState.cartItems.sumOf { it.quantity }})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.cartItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.quantity}x ${item.product.name}",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(item.subtotal),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                            }
                        }

                        Divider(color = OutlineSoft, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                        // Subtotal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Subtotal", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                            Text(
                                text = CurrencyFormatter.formatRupiah(uiState.subtotal),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Discount Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Diskon Pesanan",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                com.kapos.mobile.domain.model.DiscountType.values().forEach { dType ->
                                    val isSel = uiState.discountType == dType
                                    Box(
                                        modifier = Modifier
                                            .clip(AppShapes.Sm)
                                            .background(if (isSel) Primary else SurfaceTint)
                                            .clickable { onDiscountTypeChange(dType) }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = dType.displayName,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSel) SurfaceWhite else TextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = if (uiState.discountInput == 0.0) "" else if (uiState.discountType == com.kapos.mobile.domain.model.DiscountType.PERCENT) uiState.discountInput.toInt().toString() else uiState.discountInput.toLong().toString(),
                            onValueChange = { str ->
                                val cleaned = str.filter { it.isDigit() }
                                val amount = cleaned.toDoubleOrNull() ?: 0.0
                                onDiscountInputChange(amount)
                            },
                            placeholder = { Text(if (uiState.discountType == com.kapos.mobile.domain.model.DiscountType.PERCENT) "Contoh: 10 (%)" else "Contoh: 5000 (Rp)") },
                            singleLine = true,
                            shape = AppShapes.Sm,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (uiState.discount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Potongan Diskon", style = MaterialTheme.typography.bodySmall.copy(color = Danger))
                                Text(
                                    text = "- ${CurrencyFormatter.formatRupiah(uiState.discount)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Danger, fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Divider(color = OutlineSoft, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Pembayaran",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = CurrencyFormatter.formatRupiah(uiState.total),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Primary,
                                    fontSize = 20.sp
                                )
                            )
                        }
                    }
                }

                // Section 3: Payment Method
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.Md)
                        .background(SurfaceWhite)
                        .border(1.dp, OutlineSoft, AppShapes.Md)
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Metode Pembayaran",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PaymentMethodTile(
                                label = "Tunai",
                                icon = Icons.Default.Payments,
                                isSelected = uiState.paymentMethod == PaymentMethod.CASH,
                                onClick = {
                                    onPaymentMethodChange(PaymentMethod.CASH)
                                    onCashGivenChange(uiState.total)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            PaymentMethodTile(
                                label = "QRIS",
                                icon = Icons.Default.QrCode,
                                isSelected = uiState.paymentMethod == PaymentMethod.QRIS,
                                onClick = { onPaymentMethodChange(PaymentMethod.QRIS) },
                                modifier = Modifier.weight(1f)
                            )
                            PaymentMethodTile(
                                label = "Transfer",
                                icon = Icons.Default.CreditCard,
                                isSelected = uiState.paymentMethod == PaymentMethod.TRANSFER,
                                onClick = { onPaymentMethodChange(PaymentMethod.TRANSFER) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Cash Change Calculator
                        if (uiState.paymentMethod == PaymentMethod.CASH) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Uang Diterima",
                                style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Quick cash chip buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QuickCashChip("Uang Pas", onClick = { onCashGivenChange(uiState.total) })
                                QuickCashChip("50.000", onClick = { onCashGivenChange(50000.0) })
                                QuickCashChip("100.000", onClick = { onCashGivenChange(100000.0) })
                                QuickCashChip("200.000", onClick = { onCashGivenChange(200000.0) })
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = if (uiState.cashGiven > 0) uiState.cashGiven.toLong().toString() else "",
                                onValueChange = { str ->
                                    val num = str.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                                    onCashGivenChange(num)
                                },
                                label = { Text("Nominal Uang (Rp)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = AppShapes.Sm,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = OutlineSoft
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Change due display
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppShapes.Sm)
                                    .background(if (uiState.isCashSufficient) Color(0xFFE8F7EE) else Color(0xFFFDE4E5))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (uiState.isCashSufficient) "Kembalian" else "Uang Kurang",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (uiState.isCashSufficient) Success else Danger,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = if (uiState.isCashSufficient) {
                                            CurrencyFormatter.formatRupiah(uiState.changeDue)
                                        } else {
                                            CurrencyFormatter.formatRupiah(uiState.total - uiState.cashGiven)
                                        },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = if (uiState.isCashSufficient) Success else Danger,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 4: Notes
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = onNotesChange,
                    label = { Text("Catatan Pesanan / Meja (Opsional)") },
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = OutlineSoft
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryButton(
                        text = "Batal",
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = "Selesaikan",
                        onClick = onSubmitOrder,
                        enabled = uiState.isCashSufficient && !uiState.isSubmitting,
                        isLoading = uiState.isSubmitting,
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun OrderTypeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(AppShapes.Sm)
            .background(if (isSelected) PrimarySoft else SurfaceTint)
            .border(1.dp, if (isSelected) Primary else OutlineSoft, AppShapes.Sm)
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Primary else TextPrimary
            )
        )
    }
}

@Composable
fun PaymentMethodTile(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(AppShapes.Sm)
            .background(if (isSelected) PrimarySoft else SurfaceTint)
            .border(1.dp, if (isSelected) Primary else OutlineSoft, AppShapes.Sm)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Primary else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Primary else TextPrimary
                )
            )
        }
    }
}

@Composable
fun QuickCashChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(SurfaceTint)
            .border(1.dp, OutlineSoft, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        )
    }
}
