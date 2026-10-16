package com.kapos.mobile.feature.analytics

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.kapos.mobile.core.components.StatCard
import com.kapos.mobile.core.theme.OutlineSoft
import com.kapos.mobile.core.theme.Primary
import com.kapos.mobile.core.theme.PrimarySoft
import com.kapos.mobile.core.theme.ScreenBackground
import com.kapos.mobile.core.theme.SurfaceTint
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.core.utils.CurrencyFormatter
import com.kapos.mobile.domain.model.AnalyticsPeriod

@Composable
fun AnalyticsRoute(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    AnalyticsScreen(
        uiState = uiState,
        onPeriodChange = viewModel::onPeriodChange,
        modifier = modifier
    )
}

@Composable
fun AnalyticsScreen(
    uiState: AnalyticsUiState,
    onPeriodChange: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val summary = uiState.summary

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
            // Header & Period Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Analisa Keuangan & BEP",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Titik impas, HPP modal & margin profit",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                // Period Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AnalyticsPeriod.values().forEach { period ->
                        val isSelected = uiState.selectedPeriod == period
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) Primary else SurfaceWhite)
                                .border(1.dp, if (isSelected) Primary else OutlineSoft, CircleShape)
                                .clickable { onPeriodChange(period) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = period.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) SurfaceWhite else TextPrimary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // BEP Tracking Progress Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, OutlineSoft, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                                    .background(if (summary.isBepAchieved) Color(0xFFDCFCE7) else SurfaceTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (summary.isBepAchieved) Icons.Default.CheckCircle else Icons.Default.TrendingUp,
                                    contentDescription = "BEP",
                                    tint = if (summary.isBepAchieved) Color(0xFF16A34A) else Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Progress Titik Impas (BEP)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Target: ${CurrencyFormatter.formatRupiah(summary.bepRevenueTarget)}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                            }
                        }

                        // Percentage Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (summary.isBepAchieved) Color(0xFFDCFCE7) else PrimarySoft)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "%.1f%%".format(summary.bepProgressPercentage),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.isBepAchieved) Color(0xFF16A34A) else Primary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Linear Progress Indicator
                    LinearProgressIndicator(
                        progress = { (summary.bepProgressPercentage / 100.0).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (summary.isBepAchieved) Color(0xFF16A34A) else Primary,
                        trackColor = SurfaceTint
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Motivation status text
                    Text(
                        text = if (summary.isBepAchieved) {
                            "🎉 Titik Impas (BEP) Tercapai! Omzet selanjutnya adalah akumulasi Laba Bersih gerai."
                        } else {
                            "Kurang ${CurrencyFormatter.formatRupiah(summary.bepRemainingRevenue)} untuk mencapai Titik Impas ${uiState.selectedPeriod.displayName.lowercase()}."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (summary.isBepAchieved) Color(0xFF16A34A) else TextSecondary,
                            fontWeight = if (summary.isBepAchieved) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profit & Loss Breakdown Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, OutlineSoft, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ringkasan Laba Rugi",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        )

                        // Net Margin Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (summary.netProfit >= 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Margin: %.1f%%".format(summary.netMarginPercentage),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.netProfit >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    FinancialRow(
                        label = "Omzet Penjualan (Gross Sales)",
                        value = CurrencyFormatter.formatRupiah(summary.grossRevenue),
                        isBold = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FinancialRow(
                        label = "HPP Bahan Baku (Est. 55%)",
                        value = "− ${CurrencyFormatter.formatRupiah(summary.totalCogs)}",
                        valueColor = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FinancialRow(
                        label = "Laba Kotor (Gross Profit)",
                        value = CurrencyFormatter.formatRupiah(summary.grossProfit),
                        isBold = true,
                        valueColor = Primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FinancialRow(
                        label = "Beban Tetap (${uiState.selectedPeriod.displayName})",
                        value = "− ${CurrencyFormatter.formatRupiah(summary.fixedCost)}",
                        valueColor = Color(0xFFD97706)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = OutlineSoft)
                    Spacer(modifier = Modifier.height(10.dp))

                    FinancialRow(
                        label = "Estimasi Laba Bersih (Net Profit)",
                        value = CurrencyFormatter.formatRupiah(summary.netProfit),
                        isBold = true,
                        valueColor = if (summary.netProfit >= 0) Color(0xFF16A34A) else Color(0xFFDC2626),
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Operational Metrics
            Text(
                text = "Metrik Transaksi & Pembayaran",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Jumlah Transaksi",
                    value = "${summary.transactionCount} struk",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Rata-rata Order",
                    value = CurrencyFormatter.formatRupiah(summary.averageOrderValue),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Tunai (Cash)",
                    value = CurrencyFormatter.formatRupiah(uiState.cashTotal),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Non-Tunai (QRIS)",
                    value = CurrencyFormatter.formatRupiah(uiState.nonCashTotal),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FinancialRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = TextPrimary,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = fontSize,
                color = if (isBold) TextPrimary else TextSecondary,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = fontSize,
                color = valueColor,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold
            )
        )
    }
}
