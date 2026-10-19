package com.kapos.mobile.feature.report

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapos.mobile.core.components.StatCard
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

@Composable
fun ReportRoute(
    viewModel: ReportViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ReportScreen(
            uiState = uiState,
            onPeriodChange = viewModel::onPeriodChange,
            onPrintReceipt = viewModel::printFinancialSummaryReceipt,
            onExportTransactionsCsv = { viewModel.exportTransactionsCsv(context) },
            onExportSummaryCsv = { viewModel.exportFinancialSummaryCsv(context) },
            modifier = Modifier.fillMaxSize()
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        )
    }
}

@Composable
fun ReportScreen(
    uiState: ReportUiState,
    onPeriodChange: (ReportPeriod) -> Unit,
    onPrintReceipt: () -> Unit = {},
    onExportTransactionsCsv: () -> Unit = {},
    onExportSummaryCsv: () -> Unit = {},
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
                .verticalScroll(scrollState)
                .padding(bottom = 100.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Laporan Keuangan",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Laba Rugi & Performa Gerai KA",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                // Period Chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ReportPeriod.values().forEach { period ->
                        val isSelected = uiState.selectedPeriod == period
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) Primary else SurfaceWhite)
                                .border(1.dp, if (isSelected) Primary else OutlineSoft, CircleShape)
                                .clickable { onPeriodChange(period) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = period.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) SurfaceWhite else TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 1. Executive Card: Net Profit (Laba Bersih) & Gross Profit Summary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(AppShapes.Lg)
                    .background(SurfaceWhite)
                    .border(1.dp, OutlineSoft, AppShapes.Lg)
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Laba Bersih (Net Profit)",
                                style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyFormatter.formatRupiah(uiState.netProfit),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (uiState.netProfit >= 0) Success else Danger
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (uiState.netProfit >= 0) Color(0xFFDCFCE7) else Color(0xFFFDE4E5))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (uiState.netProfit >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (uiState.netProfit >= 0) Color(0xFF166534) else Danger,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${uiState.netProfitMarginPercent}% Margin",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (uiState.netProfit >= 0) Color(0xFF166534) else Danger,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    Divider(color = OutlineSoft, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                    // Financial Breakdown Table
                    FinancialRow(label = "Penjualan Kotor (Gross Revenue)", amount = uiState.grossRevenue)
                    if (uiState.totalDiscount > 0) {
                        FinancialRow(label = "Potongan Diskon Promo", amount = -uiState.totalDiscount, isDeduction = true)
                    }
                    FinancialRow(label = "Penjualan Bersih (Net Revenue)", amount = uiState.netRevenue, isHighlight = true)
                    FinancialRow(label = "HPP Modal Barang (COGS)", amount = -uiState.totalCogs, isDeduction = true)
                    FinancialRow(label = "Laba Kotor (Gross Profit)", amount = uiState.grossProfit, subtext = "${uiState.grossProfitMarginPercent}% Margin Kotor")
                    if (uiState.operationalExpenses > 0) {
                        FinancialRow(label = "Kas Keluar (Biaya Operasional)", amount = -uiState.operationalExpenses, isDeduction = true)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Action Bar: Cetak Thermal, CSV Transaksi, CSV Laba Rugi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportActionTile(
                    label = "Cetak Rekap",
                    icon = Icons.Default.Print,
                    isLoading = false,
                    onClick = onPrintReceipt,
                    modifier = Modifier.weight(1f)
                )
                ReportActionTile(
                    label = "CSV Transaksi",
                    icon = Icons.Default.Download,
                    isLoading = uiState.isExporting,
                    onClick = onExportTransactionsCsv,
                    modifier = Modifier.weight(1f)
                )
                ReportActionTile(
                    label = "CSV Laba Rugi",
                    icon = Icons.Default.Share,
                    isLoading = uiState.isExporting,
                    onClick = onExportSummaryCsv,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Stat Grid (2x2)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Omzet Bersih",
                        value = CurrencyFormatter.formatRupiah(uiState.netRevenue),
                        icon = Icons.Default.AttachMoney,
                        subtitle = "${uiState.selectedPeriod.displayName}",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Total Transaksi",
                        value = "${uiState.totalTransactionsCount} Struk",
                        icon = Icons.Default.Receipt,
                        subtitle = if (uiState.voidTransactionsCount > 0) "${uiState.voidTransactionsCount} Void" else "Semua Lunas",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Uang Tunai (Laci)",
                        value = CurrencyFormatter.formatRupiah(uiState.cashTotal),
                        icon = Icons.Default.AccountBalanceWallet,
                        subtitle = "Kas Tunai",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Non-Tunai (QRIS & Transfer)",
                        value = CurrencyFormatter.formatRupiah(uiState.nonCashTotal),
                        icon = Icons.Default.CreditCard,
                        subtitle = "QRIS: ${CurrencyFormatter.formatRupiah(uiState.qrisTotal)}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Shift & Active Cashier Info Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(AppShapes.Md)
                    .background(SurfaceWhite)
                    .border(1.dp, OutlineSoft, AppShapes.Md)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(PrimarySoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Kasir Aktif: ${uiState.cashierShiftName}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                )
                                Text(
                                    text = "Shift Operasional KA POS",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(AppShapes.Xs)
                                .background(PrimarySoft)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Aktif",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Divider(color = OutlineSoft, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Rata-rata Struk (Basket Size / AOV)",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(uiState.averageOrderValue),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Best-Selling Menu Items
            if (uiState.topSellingProducts.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(AppShapes.Md)
                        .background(SurfaceWhite)
                        .border(1.dp, OutlineSoft, AppShapes.Md)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "5 Menu Terlaris (${uiState.selectedPeriod.displayName})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        uiState.topSellingProducts.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (index == 0) Primary else SurfaceTint),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (index == 0) SurfaceWhite else TextSecondary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item.productName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${item.quantity} porsi",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Primary)
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(item.totalSales),
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                                    )
                                }
                            }

                            if (index < uiState.topSellingProducts.lastIndex) {
                                Divider(color = SurfaceTint, thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialRow(
    label: String,
    amount: Double,
    isDeduction: Boolean = false,
    isHighlight: Boolean = false,
    subtext: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = if (isHighlight) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                else MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
            if (subtext != null) {
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall.copy(color = Primary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                )
            }
        }

        Text(
            text = if (isDeduction) "- ${CurrencyFormatter.formatRupiah(kotlin.math.abs(amount))}" else CurrencyFormatter.formatRupiah(amount),
            style = if (isHighlight) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Primary)
            else if (isDeduction) MaterialTheme.typography.bodySmall.copy(color = Danger, fontWeight = FontWeight.Medium)
            else MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
        )
    }
}

@Composable
fun ReportActionTile(
    label: String,
    icon: ImageVector,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(AppShapes.Sm)
            .background(SurfaceWhite)
            .border(1.dp, OutlineSoft, AppShapes.Sm)
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Primary,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 11.sp
                )
            )
        }
    }
}
