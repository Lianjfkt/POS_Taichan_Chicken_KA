package com.kapos.mobile.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapos.mobile.core.theme.*
import com.kapos.mobile.core.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// Route
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    DashboardScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val today = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id")).format(Date())
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 96.dp)  // space for bottom nav
        ) {
            // ── Header ──────────────────────────────────────────────────────
            DashboardHeader(
                storeName = uiState.storeName,
                ownerName = uiState.ownerName,
                dateLabel = today,
                lowStockCount = uiState.lowStockCount,
                expiredStockCount = uiState.expiredStockCount,
                onRefresh = onRefresh,
                isLoading = uiState.isLoading
            )

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Revenue hero card ────────────────────────────────────────
                RevenueHeroCard(uiState)

                // ── BEP Progress ─────────────────────────────────────────────
                BepProgressCard(uiState)

                // ── Profit row ───────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfitCard(
                        label = "Laba Kotor",
                        value = uiState.todayGrossProfit,
                        percent = uiState.grossMarginPercent,
                        color = Success,
                        bgColor = SuccessBg,
                        modifier = Modifier.weight(1f)
                    )
                    ProfitCard(
                        label = "Laba Bersih",
                        value = uiState.todayNetProfit,
                        percent = null,
                        color = if (uiState.todayNetProfit >= 0) Success else Danger,
                        bgColor = if (uiState.todayNetProfit >= 0) SuccessBg else DangerBg,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ── Transaction summary ──────────────────────────────────────
                TransactionSummaryRow(uiState)

                // ── Payment breakdown ────────────────────────────────────────
                PaymentBreakdownCard(uiState)

                // ── Top products ─────────────────────────────────────────────
                TopProductsCard(uiState)

                // ── Hourly chart ─────────────────────────────────────────────
                HourlySalesCard(uiState)

                // ── Stock alert ──────────────────────────────────────────────
                if (uiState.lowStockCount > 0 || uiState.expiredStockCount > 0) {
                    StockAlertCard(
                        lowStockCount = uiState.lowStockCount,
                        expiredStockCount = uiState.expiredStockCount
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardHeader(
    storeName: String,
    ownerName: String,
    dateLabel: String,
    lowStockCount: Int,
    expiredStockCount: Int,
    onRefresh: () -> Unit,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Primary, PrimaryDark))
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Store avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceWhite.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏪", fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dashboard Owner",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = SurfaceWhite.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = storeName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = SurfaceWhite,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Refresh button
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceWhite.copy(alpha = 0.15f))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = SurfaceWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = SurfaceWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = SurfaceWhite.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SurfaceWhite.copy(alpha = 0.75f)
                    )
                )

                if (lowStockCount > 0 || expiredStockCount > 0) {
                    Spacer(Modifier.width(12.dp))
                    val alertText = buildString {
                        if (lowStockCount > 0) append("⚠️ $lowStockCount stok menipis")
                        if (expiredStockCount > 0) {
                            if (lowStockCount > 0) append(" · ")
                            append("❌ $expiredStockCount kadaluarsa")
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(AppShapes.Full)
                            .background(Warning.copy(alpha = 0.25f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = alertText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SurfaceWhite,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Revenue Hero Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RevenueHeroCard(state: DashboardUiState) {
    val growth = state.revenueGrowthPercent
    val isPositive = growth >= 0
    val trendIcon = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown
    val trendColor = if (isPositive) Success else Danger

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.Md)
            .background(
                Brush.linearGradient(
                    listOf(Primary, Color(0xFF0E7A96))
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Pendapatan Hari Ini",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = SurfaceWhite.copy(alpha = 0.75f)
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = CurrencyFormatter.formatRupiah(state.todayRevenue),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = SurfaceWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp
                )
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(AppShapes.Full)
                        .background(
                            if (isPositive) Success.copy(alpha = 0.25f)
                            else Danger.copy(alpha = 0.25f)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(trendIcon, null, tint = trendColor, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format("%.1f", growth)}% vs kemarin",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SurfaceWhite,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Kemarin: ${CurrencyFormatter.formatRupiah(state.yesterdayRevenue)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SurfaceWhite.copy(alpha = 0.65f),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BEP Progress Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BepProgressCard(state: DashboardUiState) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.bepProgressPercent / 100f,
        animationSpec = tween(durationMillis = 900),
        label = "bep_progress"
    )

    SurfaceCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CardIconBadge(
                        icon = Icons.Default.Flag,
                        tint = if (state.isBepAchieved) Success else Warning
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Target BEP Harian",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary, fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(AppShapes.Xs)
                        .background(
                            if (state.isBepAchieved) SuccessBg else InfoBg
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (state.isBepAchieved) "✅ TERCAPAI!" else "${String.format("%.0f", state.bepProgressPercent)}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (state.isBepAchieved) Success else Info,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(AppShapes.Full),
                color = if (state.isBepAchieved) Success else Primary,
                trackColor = OutlineSoft,
                strokeCap = StrokeCap.Round
            )

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = CurrencyFormatter.formatRupiah(state.todayRevenue),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Primary, fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = if (state.isBepAchieved)
                        "🎉 Profit sudah aman!"
                    else
                        "Kurang: ${CurrencyFormatter.formatRupiah(state.remainingToBep)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (state.isBepAchieved) Success else TextSecondary
                    )
                )
                Text(
                    text = "Target: ${CurrencyFormatter.formatRupiah(state.dailyRevenueTarget)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Profit Card (Gross / Net)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfitCard(
    label: String,
    value: Double,
    percent: Int?,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(AppShapes.Md)
            .background(SurfaceWhite)
            .border(1.dp, OutlineSoft, AppShapes.Md)
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = CurrencyFormatter.formatRupiah(value),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (value >= 0) TextPrimary else Danger,
                    fontSize = 16.sp
                )
            )
            if (percent != null) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(AppShapes.Xs)
                        .background(bgColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Margin $percent%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = color, fontWeight = FontWeight.SemiBold, fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Transaction Summary Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TransactionSummaryRow(state: DashboardUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStatCard(
            icon = Icons.Default.ShoppingBag,
            label = "Transaksi",
            value = "${state.todayTransactionCount}x",
            iconTint = Primary,
            iconBg = PrimarySoft,
            modifier = Modifier.weight(1f)
        )
        MiniStatCard(
            icon = Icons.Default.AttachMoney,
            label = "Rata-rata",
            value = CurrencyFormatter.formatRupiah(state.averageOrderValue),
            iconTint = Info,
            iconBg = InfoBg,
            modifier = Modifier.weight(1f)
        )
        MiniStatCard(
            icon = Icons.Default.Cancel,
            label = "Void",
            value = "${state.todayVoidCount}x",
            iconTint = if (state.todayVoidCount > 0) Danger else TextSecondary,
            iconBg = if (state.todayVoidCount > 0) DangerBg else SurfaceTint,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MiniStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(AppShapes.Md)
            .background(SurfaceWhite)
            .border(1.dp, OutlineSoft, AppShapes.Md)
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary, fontSize = 10.sp
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Payment Breakdown Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PaymentBreakdownCard(state: DashboardUiState) {
    val totalRevenue = state.todayRevenue.coerceAtLeast(1.0)

    SurfaceCard {
        Column {
            SectionTitle(
                icon = Icons.Default.AccountBalanceWallet,
                title = "Metode Pembayaran"
            )
            Spacer(Modifier.height(12.dp))

            listOf(
                Triple("💵 Tunai", state.cashRevenue, Primary),
                Triple("📱 QRIS", state.qrisRevenue, Color(0xFF7C3AED)),
                Triple("🏦 Transfer", state.transferRevenue, Info),
            ).forEach { (label, amount, color) ->
                val ratio = (amount / totalRevenue).toFloat().coerceIn(0f, 1f)
                val animRatio by animateFloatAsState(ratio, tween(800), label = label)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                        modifier = Modifier.width(88.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(AppShapes.Full)
                            .background(OutlineSoft)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animRatio)
                                .clip(AppShapes.Full)
                                .background(color)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = CurrencyFormatter.formatRupiah(amount),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = color, fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top Products Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopProductsCard(state: DashboardUiState) {
    SurfaceCard {
        Column {
            SectionTitle(icon = Icons.Default.Leaderboard, title = "Produk Terlaris Hari Ini")
            Spacer(Modifier.height(12.dp))

            if (state.topProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Belum ada transaksi hari ini",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            } else {
                val maxQty = state.topProducts.maxOf { it.quantity }.coerceAtLeast(1)
                state.topProducts.forEachIndexed { index, product ->
                    TopProductRow(
                        rank = index + 1,
                        product = product,
                        maxQty = maxQty
                    )
                    if (index < state.topProducts.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = OutlineSoft.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopProductRow(rank: Int, product: DashboardTopProduct, maxQty: Int) {
    val rankColor = when (rank) {
        1 -> Color(0xFFF59E0B)  // gold
        2 -> Color(0xFF94A3B8)  // silver
        3 -> Color(0xFFCD7F32)  // bronze
        else -> TextSecondary
    }
    val animProgress by animateFloatAsState(
        targetValue = product.quantity.toFloat() / maxQty,
        animationSpec = tween(900),
        label = "prod_$rank"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        // Rank badge
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(rankColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (rank <= 3) listOf("🥇", "🥈", "🥉")[rank - 1] else "#$rank",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = if (rank <= 3) 14.sp else 10.sp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(product.emoji, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold, color = TextPrimary
                ),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { animProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(AppShapes.Full),
                color = Primary,
                trackColor = OutlineSoft,
                strokeCap = StrokeCap.Round
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${product.quantity}x",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold, color = Primary
                )
            )
            Text(
                text = CurrencyFormatter.formatRupiah(product.revenue),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary, fontSize = 10.sp
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hourly Sales Chart (bar-based)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HourlySalesCard(state: DashboardUiState) {
    SurfaceCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(icon = Icons.Default.BarChart, title = "Penjualan Per Jam")
                state.peakHour?.let { peak ->
                    Text(
                        text = "Puncak: ${String.format("%02d", peak.hour)}:00",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Warning, fontWeight = FontWeight.Bold, fontSize = 11.sp
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            val hours = state.hourlySales
            if (hours.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Belum ada data jam ini",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            } else {
                val maxRevenue = hours.maxOf { it.revenue }.coerceAtLeast(1.0)
                val barHeight = 60.dp

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight + 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    hours.takeLast(12).forEach { hourData ->
                        val fraction = (hourData.revenue / maxRevenue).toFloat()
                        val animFraction by animateFloatAsState(
                            targetValue = fraction,
                            animationSpec = tween(700),
                            label = "bar_${hourData.hour}"
                        )
                        val isPeak = hourData.hour == state.peakHour?.hour
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            val fillH: Dp = (barHeight.value * animFraction.coerceAtLeast(0.04f)).dp
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(fillH)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (isPeak) Warning else Primary.copy(alpha = 0.7f))
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = String.format("%02d", hourData.hour),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isPeak) Warning else TextSecondary,
                                    fontSize = 8.sp,
                                    fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stock Alert Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StockAlertCard(lowStockCount: Int, expiredStockCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.Md)
            .background(DangerBg)
            .border(1.dp, Danger.copy(alpha = 0.3f), AppShapes.Md)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Danger,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Perhatian Stok Bahan",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Danger, fontWeight = FontWeight.Bold
                    )
                )
                val detail = buildList {
                    if (lowStockCount > 0) add("$lowStockCount bahan hampir habis")
                    if (expiredStockCount > 0) add("$expiredStockCount batch kadaluarsa")
                }.joinToString(" · ")
                Text(
                    detail,
                    style = MaterialTheme.typography.labelSmall.copy(color = Danger.copy(alpha = 0.8f))
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.Md)
            .background(SurfaceWhite)
            .border(1.dp, OutlineSoft, AppShapes.Md)
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CardIconBadge(icon = icon)
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelMedium.copy(
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun CardIconBadge(
    icon: ImageVector,
    tint: Color = Primary,
    bg: Color = PrimarySoft,
    size: Dp = 32.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size((size.value * 0.5f).dp))
    }
}
