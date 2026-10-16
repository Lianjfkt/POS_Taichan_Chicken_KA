package com.kapos.mobile.feature.stock

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
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
import com.kapos.mobile.core.components.PrimaryButton
import com.kapos.mobile.core.components.StatCard
import com.kapos.mobile.core.theme.OutlineSoft
import com.kapos.mobile.core.theme.Primary
import com.kapos.mobile.core.theme.PrimarySoft
import com.kapos.mobile.core.theme.ScreenBackground
import com.kapos.mobile.core.theme.SurfaceTint
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.domain.model.ChickenBatch
import com.kapos.mobile.domain.model.ChickenPart
import com.kapos.mobile.domain.model.StockFreshness
import com.kapos.mobile.domain.model.TaichanBatch

@Composable
fun StockRoute(
    viewModel: StockViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    StockScreen(
        uiState = uiState,
        onTabSelect = viewModel::onTabSelect,
        onOpenRestock = viewModel::openRestockDialog,
        onCloseRestock = viewModel::closeRestockDialog,
        onAddTaichan = viewModel::addTaichanBatch,
        onUpdateTaichanFried = viewModel::updateTaichanFried,
        onDeleteTaichan = viewModel::deleteTaichanBatch,
        onAddChicken = viewModel::addChickenBatch,
        onUpdateChickenFried = viewModel::updateChickenFried,
        onDeleteChicken = viewModel::deleteChickenBatch,
        modifier = modifier
    )
}

@Composable
fun StockScreen(
    uiState: StockUiState,
    onTabSelect: (StockTab) -> Unit,
    onOpenRestock: (ChickenPart) -> Unit,
    onCloseRestock: () -> Unit,
    onAddTaichan: (raw: Int, notes: String) -> Unit,
    onUpdateTaichanFried: (id: String, fried: Int) -> Unit,
    onDeleteTaichan: (id: String) -> Unit,
    onAddChicken: (part: ChickenPart, raw: Int, notes: String) -> Unit,
    onUpdateChickenFried: (id: String, fried: Int) -> Unit,
    onDeleteChicken: (id: String) -> Unit,
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
                        text = "Pelacak Stok Harian",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Kontrol batch mentah, matang & kesegaran",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                // Add Batch Button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Primary)
                        .clickable { onOpenRestock(uiState.restockChickenPart) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah", tint = SurfaceWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Restock",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = SurfaceWhite
                            )
                        )
                    }
                }
            }

            // Tab Selector: Taichan vs Ayam
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StockTab.values().forEach { tab ->
                    val isSelected = uiState.activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Primary else SurfaceWhite)
                            .border(1.dp, if (isSelected) Primary else OutlineSoft, RoundedCornerShape(10.dp))
                            .clickable { onTabSelect(tab) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.displayName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) SurfaceWhite else TextPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metric Summary Row
            if (uiState.activeTab == StockTab.TAICHAN) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Total Mentah",
                        value = "${uiState.taichanTotalRaw} tsk",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Siap Jual",
                        value = "${uiState.taichanRemaining} tsk",
                        subtitle = "Goreng: ${uiState.taichanTotalFried}",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Terjual Hari Ini",
                        value = "${uiState.taichanSoldToday} tsk",
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChickenPart.values().forEach { part ->
                        val summary = uiState.chickenSummaries[part]
                        StatCard(
                            title = part.displayName,
                            value = "${summary?.remaining ?: 0} ptg",
                            subtitle = "M: ${summary?.totalRaw ?: 0}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Batches List
            Text(
                text = "Daftar Batch Masuk",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (uiState.activeTab == StockTab.TAICHAN) {
                if (uiState.taichanBatches.isEmpty()) {
                    EmptyStockView(tabName = "Sate Taichan")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.taichanBatches, key = { it.id }) { batch ->
                            TaichanBatchCard(
                                batch = batch,
                                onUpdateFried = { onUpdateTaichanFried(batch.id, it) },
                                onDelete = { onDeleteTaichan(batch.id) }
                            )
                        }
                    }
                }
            } else {
                if (uiState.chickenBatches.isEmpty()) {
                    EmptyStockView(tabName = "Ayam Potong")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.chickenBatches, key = { it.id }) { batch ->
                            ChickenBatchCard(
                                batch = batch,
                                onUpdateFried = { onUpdateChickenFried(batch.id, it) },
                                onDelete = { onDeleteChicken(batch.id) }
                            )
                        }
                    }
                }
            }
        }

        // Restock Dialog
        if (uiState.isRestockDialogOpen) {
            AddStockBatchDialog(
                activeTab = uiState.activeTab,
                selectedChickenPart = uiState.restockChickenPart,
                onDismiss = onCloseRestock,
                onAddTaichan = onAddTaichan,
                onAddChicken = onAddChicken
            )
        }
    }
}

@Composable
private fun TaichanBatchCard(
    batch: TaichanBatch,
    onUpdateFried: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val freshness = batch.freshness()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceWhite)
            .border(1.dp, OutlineSoft, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tanggal: ${batch.date}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FreshnessBadge(freshness = freshness)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color(0xFFDC2626))
                }
            }

            if (batch.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = batch.notes,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stepper for fried quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mentah: ${batch.raw} tusuk",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Goreng:",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SurfaceTint)
                            .clickable { onUpdateFried((batch.fried - 10).coerceAtLeast(0)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "${batch.fried}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Primary)
                            .clickable { onUpdateFried((batch.fried + 10).coerceAtMost(batch.raw)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah", tint = SurfaceWhite, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChickenBatchCard(
    batch: ChickenBatch,
    onUpdateFried: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val freshness = batch.freshness()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceWhite)
            .border(1.dp, OutlineSoft, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${batch.part.displayName} • ${batch.date}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FreshnessBadge(freshness = freshness)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color(0xFFDC2626))
                }
            }

            if (batch.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = batch.notes,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stepper for fried quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mentah: ${batch.raw} potong",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Goreng:",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SurfaceTint)
                            .clickable { onUpdateFried((batch.fried - 1).coerceAtLeast(0)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "${batch.fried}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Primary)
                            .clickable { onUpdateFried((batch.fried + 1).coerceAtMost(batch.raw)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah", tint = SurfaceWhite, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FreshnessBadge(freshness: StockFreshness) {
    val (bgColor, textColor) = when (freshness) {
        StockFreshness.FRESH -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        StockFreshness.DAY_2 -> Color(0xFFDBEAFE) to Color(0xFF1D4ED8)
        StockFreshness.DAY_3 -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        StockFreshness.EXPIRED -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = freshness.displayName,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
private fun EmptyStockView(tabName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Belum Ada Batch $tabName",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Klik tombol Restock di atas untuk mencatat batch masuk",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }
    }
}
