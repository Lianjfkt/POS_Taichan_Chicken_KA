package com.kapos.mobile.feature.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Divider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.kapos.mobile.core.theme.Warning
import com.kapos.mobile.core.utils.CurrencyFormatter
import com.kapos.mobile.domain.model.Customer
import com.kapos.mobile.domain.model.OrderType
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.Promotion

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
        onSelectCustomer = viewModel::onSelectCustomer,
        onToggleRedeemPoints = viewModel::onToggleRedeemPoints,
        onOpenSelectCustomerDialog = { viewModel.setSelectCustomerDialogOpen(true) },
        onCloseSelectCustomerDialog = { viewModel.setSelectCustomerDialogOpen(false) },
        onOpenQuickAddCustomerDialog = { viewModel.setQuickAddCustomerDialogOpen(true) },
        onCloseQuickAddCustomerDialog = { viewModel.setQuickAddCustomerDialogOpen(false) },
        onQuickAddCustomer = viewModel::quickAddCustomer,
        onDiscountTypeChange = viewModel::onDiscountTypeChange,
        onDiscountInputChange = viewModel::onDiscountInputChange,
        onPaymentMethodChange = viewModel::onPaymentMethodChange,
        onCashGivenChange = viewModel::onCashGivenChange,
        onNotesChange = viewModel::onNotesChange,
        onToggleSplitPayment = viewModel::onToggleSplitPayment,
        onSplitCashPortionChange = viewModel::onSplitCashPortionChange,
        onSplitCashGivenChange = viewModel::onSplitCashGivenChange,
        onSplitNonCashMethodChange = viewModel::onSplitNonCashMethodChange,
        onSelectPromotion = viewModel::onSelectPromotion,
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
    onSelectCustomer: (Customer?) -> Unit,
    onToggleRedeemPoints: (Boolean) -> Unit,
    onOpenSelectCustomerDialog: () -> Unit,
    onCloseSelectCustomerDialog: () -> Unit,
    onOpenQuickAddCustomerDialog: () -> Unit,
    onCloseQuickAddCustomerDialog: () -> Unit,
    onQuickAddCustomer: (String, String) -> Unit,
    onDiscountTypeChange: (com.kapos.mobile.domain.model.DiscountType) -> Unit,
    onDiscountInputChange: (Double) -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onCashGivenChange: (Double) -> Unit,
    onNotesChange: (String) -> Unit,
    onToggleSplitPayment: (Boolean) -> Unit = {},
    onSplitCashPortionChange: (Double) -> Unit = {},
    onSplitCashGivenChange: (Double) -> Unit = {},
    onSplitNonCashMethodChange: (PaymentMethod) -> Unit = {},
    onSelectPromotion: (Promotion?) -> Unit = {},
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Customer / Member CRM Section
                        if (uiState.selectedCustomer != null) {
                            val member = uiState.selectedCustomer
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppShapes.Sm)
                                    .background(PrimarySoft)
                                    .border(1.dp, Primary, AppShapes.Sm)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Primary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = SurfaceWhite,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = member.name,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFFEF3C7))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "⭐ ${member.points} Poin",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = Color(0xFF92400E),
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.sp
                                                            )
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "Member • ${member.phone.ifBlank { "Tanpa No HP" }}",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(SurfaceWhite)
                                                .border(1.dp, OutlineSoft, CircleShape)
                                                .clickable { onOpenSelectCustomerDialog() }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Ganti",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Primary
                                                )
                                            )
                                        }
                                    }

                                    // Loyalty points redemption toggle
                                    if (uiState.availablePoints > 0) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Divider(color = Primary.copy(alpha = 0.2f), thickness = 1.dp)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = Warning,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Tukar Poin Loyalty",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                }
                                                Text(
                                                    text = if (uiState.isRedeemingPoints) {
                                                        "Gunakan ${uiState.pointsToRedeem} Poin (-${CurrencyFormatter.formatRupiah(uiState.pointDiscount)})"
                                                    } else if (uiState.availablePoints >= uiState.minPointsForRedeem) {
                                                        "Tersedia ${uiState.availablePoints} Poin (10 Poin = Rp 5.000)"
                                                    } else {
                                                        "Tersedia ${uiState.availablePoints} Poin (Min. 10 Poin untuk tukar)"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (uiState.isRedeemingPoints) Primary else TextSecondary,
                                                        fontWeight = if (uiState.isRedeemingPoints) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                )
                                            }

                                            Switch(
                                                checked = uiState.isRedeemingPoints,
                                                onCheckedChange = onToggleRedeemPoints,
                                                enabled = uiState.availablePoints >= uiState.minPointsForRedeem && uiState.maxPointsCanRedeem > 0,
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = SurfaceWhite,
                                                    checkedTrackColor = Primary
                                                )
                                            )
                                        }
                                    }

                                    // Points earned preview
                                    if (uiState.pointsEarned > 0) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "🎁 Member akan mendapatkan +${uiState.pointsEarned} Poin dari order ini",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Success,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = uiState.customerName,
                                    onValueChange = onCustomerNameChange,
                                    label = { Text("Nama Pelanggan (Umum)") },
                                    placeholder = { Text("Contoh: Kak Andi") },
                                    singleLine = true,
                                    shape = AppShapes.Sm,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Primary,
                                        unfocusedBorderColor = OutlineSoft
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(AppShapes.Sm)
                                        .background(PrimarySoft)
                                        .border(1.dp, Primary, AppShapes.Sm)
                                        .clickable { onOpenSelectCustomerDialog() }
                                        .padding(horizontal = 12.dp, vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.PersonSearch,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Member",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Primary
                                            )
                                        )
                                    }
                                }
                            }
                        }
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
                            val metaParts = mutableListOf<String>()
                            if (item.spiceLevel.isNotBlank()) metaParts.add(item.spiceLevel)
                            item.selectedModifiers.forEach { metaParts.add(it.name) }
                            if (item.notes.isNotBlank()) metaParts.add(item.notes)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.quantity}x ${item.product.name}",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                                    )
                                    if (metaParts.isNotEmpty()) {
                                        Text(
                                            text = metaParts.joinToString(" • "),
                                            style = MaterialTheme.typography.labelSmall.copy(color = Primary, fontSize = 11.sp)
                                        )
                                    }
                                }
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

                        if (uiState.manualDiscount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Potongan Diskon Menu", style = MaterialTheme.typography.bodySmall.copy(color = Danger))
                                Text(
                                    text = "- ${CurrencyFormatter.formatRupiah(uiState.manualDiscount)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Danger, fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        if (uiState.pointDiscount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Warning, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Diskon Poin (${uiState.pointsToRedeem} pts)", style = MaterialTheme.typography.bodySmall.copy(color = Danger))
                                }
                                Text(
                                    text = "- ${CurrencyFormatter.formatRupiah(uiState.pointDiscount)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Danger, fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        // Section 2C: Promo & Kupon Marketing
                        if (uiState.availablePromotions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Pilih Promo / Kupon Kasir",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val isNoneSelected = uiState.selectedPromotion == null
                                Box(
                                    modifier = Modifier
                                        .clip(AppShapes.Sm)
                                        .background(if (isNoneSelected) Primary else SurfaceTint)
                                        .clickable { onSelectPromotion(null) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Tanpa Promo",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isNoneSelected) SurfaceWhite else TextPrimary,
                                            fontWeight = if (isNoneSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                                uiState.availablePromotions.forEach { promo ->
                                    val isSelected = uiState.selectedPromotion?.id == promo.id
                                    val isEligible = uiState.subtotal >= promo.minSpend
                                    Box(
                                        modifier = Modifier
                                            .clip(AppShapes.Sm)
                                            .background(if (isSelected) Primary else if (isEligible) SurfaceTint else Color(0xFFF0F0F0))
                                            .clickable(enabled = isEligible) {
                                                onSelectPromotion(if (isSelected) null else promo)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${promo.title}${if (!isEligible) " (Min. ${CurrencyFormatter.formatRupiah(promo.minSpend)})" else ""}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSelected) SurfaceWhite else if (isEligible) TextPrimary else Color.Gray,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.promoDiscount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Promo: ${uiState.selectedPromotion?.title ?: ""}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Success, fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "- ${CurrencyFormatter.formatRupiah(uiState.promoDiscount)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Success, fontWeight = FontWeight.Bold)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Metode Pembayaran",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Split Pembayaran",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (uiState.isSplitPayment) Primary else TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = uiState.isSplitPayment,
                                    onCheckedChange = onToggleSplitPayment,
                                    colors = SwitchDefaults.colors(checkedThumbColor = SurfaceWhite, checkedTrackColor = Primary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (!uiState.isSplitPayment) {
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
                        } else {
                            // Split Payment View: Tunai + Non-Tunai
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(AppShapes.Sm)
                                        .background(PrimarySoft)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "⚡ Pembayaran Ganda Aktif: Bagi tagihan antara Tunai dan Non-Tunai",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Primary, fontWeight = FontWeight.SemiBold)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "1. Porsi Pembayaran Tunai",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    QuickCashChip("50%", onClick = { onSplitCashPortionChange((uiState.total / 2.0).toInt().toDouble()) })
                                    QuickCashChip("20.000", onClick = { onSplitCashPortionChange(20000.0) })
                                    QuickCashChip("50.000", onClick = { onSplitCashPortionChange(50000.0) })
                                    QuickCashChip("100.000", onClick = { onSplitCashPortionChange(100000.0) })
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = if (uiState.splitCashPortion > 0) uiState.splitCashPortion.toLong().toString() else "",
                                    onValueChange = { str ->
                                        val num = str.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                                        onSplitCashPortionChange(num.coerceAtMost(uiState.total))
                                    },
                                    label = { Text("Porsi Tagihan Tunai (Rp)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = AppShapes.Sm,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = if (uiState.splitCashGiven > 0) uiState.splitCashGiven.toLong().toString() else "",
                                    onValueChange = { str ->
                                        val num = str.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                                        onSplitCashGivenChange(num)
                                    },
                                    label = { Text("Uang Tunai Diterima dari Pelanggan (Rp)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = AppShapes.Sm,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (uiState.splitCashChange > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Kembalian Tunai", style = MaterialTheme.typography.bodySmall.copy(color = Success))
                                        Text(
                                            text = CurrencyFormatter.formatRupiah(uiState.splitCashChange),
                                            style = MaterialTheme.typography.bodySmall.copy(color = Success, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "2. Sisa Pembayaran Non-Tunai",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sisa Tagihan: ${CurrencyFormatter.formatRupiah(uiState.splitNonCashAmount)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Primary)
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PaymentMethodTile(
                                        label = "QRIS",
                                        icon = Icons.Default.QrCode,
                                        isSelected = uiState.splitNonCashMethod == PaymentMethod.QRIS,
                                        onClick = { onSplitNonCashMethodChange(PaymentMethod.QRIS) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    PaymentMethodTile(
                                        label = "Transfer",
                                        icon = Icons.Default.CreditCard,
                                        isSelected = uiState.splitNonCashMethod == PaymentMethod.TRANSFER,
                                        onClick = { onSplitNonCashMethodChange(PaymentMethod.TRANSFER) },
                                        modifier = Modifier.weight(1f)
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

        // Customer Selection Dialog
        if (uiState.isSelectCustomerDialogOpen) {
            SelectCustomerDialog(
                customers = uiState.availableCustomers,
                selectedCustomer = uiState.selectedCustomer,
                onSelectCustomer = onSelectCustomer,
                onOpenQuickAdd = {
                    onCloseSelectCustomerDialog()
                    onOpenQuickAddCustomerDialog()
                },
                onDismiss = onCloseSelectCustomerDialog
            )
        }

        // Quick Add Customer Dialog
        if (uiState.isQuickAddCustomerDialogOpen) {
            QuickAddCustomerDialog(
                onSave = onQuickAddCustomer,
                onDismiss = onCloseQuickAddCustomerDialog
            )
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
