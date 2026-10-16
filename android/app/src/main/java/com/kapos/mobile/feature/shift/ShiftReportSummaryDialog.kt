package com.kapos.mobile.feature.shift

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kapos.mobile.core.components.PrimaryButton
import com.kapos.mobile.core.theme.AppShapes
import com.kapos.mobile.core.theme.OutlineSoft
import com.kapos.mobile.core.theme.Primary
import com.kapos.mobile.core.theme.PrimarySoft
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.core.utils.CurrencyFormatter

@Composable
fun ShiftReportSummaryDialog(
    summary: ShiftClosedSummary,
    isPrinting: Boolean,
    onPrintReceipt: () -> Unit,
    whatsAppShareText: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val selisihColor = when {
        summary.selisih > 0 -> Color(0xFFF59E0B) // Surplus
        summary.selisih < 0 -> Color(0xFFEF4444) // Deficit
        else -> Color(0xFF10B981) // Balanced
    }

    val selisihBadgeText = when {
        summary.selisih > 0 -> "Lebih: +${CurrencyFormatter.formatRupiah(summary.selisih)}"
        summary.selisih < 0 -> "Kurang: -${CurrencyFormatter.formatRupiah(-summary.selisih)}"
        else -> "Seimbang (PAS ✅)"
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.Lg)
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, AppShapes.Lg)
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
                            text = "Rekap Hasil Tutup Shift",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Kasir: ${summary.cashierName} • ${summary.closedAtStr}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selisih Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.Md)
                        .background(selisihColor.copy(alpha = 0.12f))
                        .border(1.dp, selisihColor.copy(alpha = 0.4f), AppShapes.Md)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status Rekonsiliasi:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = selisihColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = selisihBadgeText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = selisihColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Breakdown Rows
                SummaryRow("Modal Awal", CurrencyFormatter.formatRupiah(summary.startCash))
                SummaryRow("Penjualan Tunai", CurrencyFormatter.formatRupiah(summary.cashSales))
                SummaryRow("Penjualan Non-Tunai", CurrencyFormatter.formatRupiah(summary.nonCashSales))
                if (summary.cashIn > 0) {
                    SummaryRow("Kas Masuk (+)", CurrencyFormatter.formatRupiah(summary.cashIn), Color(0xFF10B981))
                }
                if (summary.cashOut > 0) {
                    SummaryRow("Kas Keluar (-)", "- ${CurrencyFormatter.formatRupiah(summary.cashOut)}", Color(0xFFEF4444))
                }

                Divider(color = OutlineSoft, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                SummaryRow("Total Kas Sistem", CurrencyFormatter.formatRupiah(summary.expectedCash), isBold = true)
                SummaryRow("Total Kas Fisik", CurrencyFormatter.formatRupiah(summary.actualCash), isBold = true, textColor = Primary)

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons: Print & WhatsApp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPrintReceipt,
                        enabled = !isPrinting,
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.Sm
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = "Print", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isPrinting) "Mencetak..." else "Cetak Struk", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, whatsAppShareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Rekap Shift"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.Sm
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "WhatsApp", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "WhatsApp", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                PrimaryButton(
                    text = "Selesai",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    textColor: Color = TextPrimary,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (isBold) TextPrimary else TextSecondary,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = textColor,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold
            )
        )
    }
}
