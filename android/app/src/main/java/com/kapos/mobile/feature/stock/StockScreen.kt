package com.kapos.mobile.feature.stock

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.kapos.mobile.domain.model.BatchFreshness
import com.kapos.mobile.domain.model.ChickenBatch
import com.kapos.mobile.domain.model.ChickenPart
import com.kapos.mobile.domain.model.MaterialBatch
import com.kapos.mobile.domain.model.RawMaterial
import com.kapos.mobile.domain.model.RawMaterialCategory
import com.kapos.mobile.domain.model.StockFreshness
import com.kapos.mobile.domain.model.StockMovement
import com.kapos.mobile.domain.model.StockMovementType
import com.kapos.mobile.domain.model.TaichanBatch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        // Raw Materials callbacks
        onMaterialCategorySelect = viewModel::onMaterialCategorySelect,
        onMaterialSearchQueryChange = viewModel::onMaterialSearchQueryChange,
        onOpenAddMaterial = viewModel::openAddMaterial,
        onOpenEditMaterial = viewModel::openEditMaterial,
        onCloseAddEditMaterial = viewModel::closeAddEditMaterial,
        onSaveMaterial = viewModel::saveMaterial,
        onDeleteMaterial = viewModel::deleteMaterial,
        onOpenRestockMaterial = viewModel::openRestockMaterial,
        onCloseRestockMaterial = viewModel::closeRestockMaterial,
        onConfirmRestockMaterial = viewModel::confirmRestockMaterial,
        onConfirmRestockWithBatch = viewModel::confirmRestockWithBatch,
        onOpenBatchDetail = viewModel::openBatchDetail,
        onCloseBatchDetail = viewModel::closeBatchDetail,
        onToggleBatchUsability = viewModel::toggleBatchUsability,
        onOpenWasteForBatch = viewModel::openWasteForBatch,
        onCloseWasteForBatch = viewModel::closeWasteForBatch,
        onConfirmBatchWaste = viewModel::confirmBatchWaste,
        onOpenWasteMaterial = viewModel::openWasteMaterial,
        onCloseWasteMaterial = viewModel::closeWasteMaterial,
        onConfirmWasteMaterial = viewModel::confirmWasteMaterial,
        onOpenOpnameMaterial = viewModel::openOpnameMaterial,
        onCloseOpnameMaterial = viewModel::closeOpnameMaterial,
        onConfirmOpnameMaterial = viewModel::confirmOpnameMaterial,
        onMovementFilterSelect = viewModel::onSelectMovementFilter,
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
    // Raw Materials
    onMaterialCategorySelect: (RawMaterialCategory?) -> Unit,
    onMaterialSearchQueryChange: (String) -> Unit,
    onOpenAddMaterial: () -> Unit,
    onOpenEditMaterial: (RawMaterial) -> Unit,
    onCloseAddEditMaterial: () -> Unit,
    onSaveMaterial: (name: String, category: RawMaterialCategory, unit: String, currentStock: Double, minStock: Double, costPerUnit: Double) -> Unit,
    onDeleteMaterial: (id: String) -> Unit,
    onOpenRestockMaterial: (RawMaterial) -> Unit,
    onCloseRestockMaterial: () -> Unit,
    onConfirmRestockMaterial: (materialId: String, quantity: Double, cost: Double, notes: String) -> Unit,
    onConfirmRestockWithBatch: (materialId: String, quantity: Double, cost: Double, expiryDate: Long?, notes: String) -> Unit = { _, _, _, _, _ -> },
    onOpenBatchDetail: (RawMaterial) -> Unit = {},
    onCloseBatchDetail: () -> Unit = {},
    onToggleBatchUsability: (batchId: String, isUsable: Boolean) -> Unit = { _, _ -> },
    onOpenWasteForBatch: (com.kapos.mobile.domain.model.MaterialBatch) -> Unit = {},
    onCloseWasteForBatch: () -> Unit = {},
    onConfirmBatchWaste: (batchId: String, quantity: Double, notes: String) -> Unit = { _, _, _ -> },
    onOpenWasteMaterial: (RawMaterial) -> Unit,
    onCloseWasteMaterial: () -> Unit,
    onConfirmWasteMaterial: (materialId: String, quantity: Double, notes: String) -> Unit,
    onOpenOpnameMaterial: (RawMaterial) -> Unit,
    onCloseOpnameMaterial: () -> Unit,
    onConfirmOpnameMaterial: (materialId: String, newStock: Double, notes: String) -> Unit,
    onMovementFilterSelect: (StockMovementType?) -> Unit = {},
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
                        text = "Inventaris & Bahan Baku",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = when (uiState.activeTab) {
                            StockTab.INVENTORY, StockTab.MATERIALS -> "Stok bahan baku, resep BOM & opname"
                            StockTab.AUDIT -> "Riwayat mutasi & audit stok otomatis"
                            else -> "Kontrol stok harian, batch & penggorengan"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                // Action Button in Header (only for Taichan, Chicken & Materials)
                if (uiState.activeTab != StockTab.AUDIT) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Primary)
                            .clickable {
                                if (uiState.activeTab == StockTab.INVENTORY || uiState.activeTab == StockTab.MATERIALS) {
                                    onOpenAddMaterial()
                                } else {
                                    onOpenRestock(uiState.restockChickenPart)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah", tint = SurfaceWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.activeTab == StockTab.INVENTORY || uiState.activeTab == StockTab.MATERIALS) "Bahan Baru" else "Restock",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SurfaceWhite
                                )
                            )
                        }
                    }
                }
            }

            // Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val visibleTabs = StockTab.values().filter { !it.isLegacy || it == uiState.activeTab }
                visibleTabs.forEach { tab ->
                    val isSelected = uiState.activeTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Primary else SurfaceWhite)
                            .border(1.dp, if (isSelected) Primary else OutlineSoft, RoundedCornerShape(10.dp))
                            .clickable { onTabSelect(tab) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
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

            when (uiState.activeTab) {
                StockTab.TAICHAN -> {
                    // Metric Summary Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Stok Mentah",
                            value = "${uiState.taichanTotalRaw}",
                            subtitle = "tusuk",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Sudah Digoreng",
                            value = "${uiState.taichanTotalFried}",
                            subtitle = "tusuk",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Terjual Hari Ini",
                            value = "${uiState.taichanSoldToday}",
                            subtitle = "tusuk",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Sisa Siap Saji",
                            value = "${uiState.taichanRemaining}",
                            subtitle = "tusuk",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Daftar Batch Siap Saji",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.taichanBatches.isEmpty()) {
                        EmptyStockView(tabName = "Sate Taichan")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.taichanBatches, key = { it.id }) { batch ->
                                TaichanBatchCard(
                                    batch = batch,
                                    onUpdateFried = { newFried -> onUpdateTaichanFried(batch.id, newFried) },
                                    onDelete = { onDeleteTaichan(batch.id) }
                                )
                            }
                        }
                    }
                }

                StockTab.CHICKEN -> {
                    // Grid summary per chicken part
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChickenPart.values().forEach { part ->
                            val summary = uiState.chickenSummaries[part]
                            StatCard(
                                title = part.displayName,
                                value = "${summary?.remaining ?: 0}",
                                subtitle = "ptg siap",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Daftar Batch Potongan Ayam",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.chickenBatches.isEmpty()) {
                        EmptyStockView(tabName = "Ayam Potong")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.chickenBatches, key = { it.id }) { batch ->
                                ChickenBatchCard(
                                    batch = batch,
                                    onUpdateFried = { newFried -> onUpdateChickenFried(batch.id, newFried) },
                                    onDelete = { onDeleteChicken(batch.id) }
                                )
                            }
                        }
                    }
                }

                StockTab.INVENTORY, StockTab.MATERIALS -> {
                    // Low Stock Alert Warning Banner
                    if (uiState.lowStockCount > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(AppShapes.Sm)
                                .background(Color(0xFFFEF3C7))
                                .border(1.dp, Warning, AppShapes.Sm)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Warning, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Peringatan: Ada ${uiState.lowStockCount} bahan baku di bawah stok minimum! Segera restock.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Category Filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryFilterChip(
                            label = "Semua (${uiState.rawMaterials.size})",
                            isSelected = uiState.selectedMaterialCategory == null,
                            onClick = { onMaterialCategorySelect(null) }
                        )
                        RawMaterialCategory.values().forEach { cat ->
                            val count = uiState.rawMaterials.count { it.category == cat }
                            CategoryFilterChip(
                                label = "${cat.displayName} ($count)",
                                isSelected = uiState.selectedMaterialCategory == cat,
                                onClick = { onMaterialCategorySelect(cat) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search input
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        OutlinedTextField(
                            value = uiState.materialSearchQuery,
                            onValueChange = onMaterialSearchQueryChange,
                            placeholder = { Text("Cari nama bahan baku (e.g. cabe, beras, fillet)...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                            trailingIcon = {
                                if (uiState.materialSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { onMaterialSearchQueryChange("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Hapus", tint = TextSecondary)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = AppShapes.Sm,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OutlineSoft,
                                focusedContainerColor = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Materials List
                    if (uiState.filteredMaterials.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text(text = "Tidak ada bahan baku ditemukan", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.filteredMaterials, key = { it.id }) { material ->
                                val batches = uiState.getBatchesFor(material.id)
                                RawMaterialItemCard(
                                    material = material,
                                    batches = batches,
                                    onRestock = { onOpenRestockMaterial(material) },
                                    onOpenBatchDetail = { onOpenBatchDetail(material) },
                                    onWaste = { onOpenWasteMaterial(material) },
                                    onOpname = { onOpenOpnameMaterial(material) },
                                    onEdit = { onOpenEditMaterial(material) },
                                    onDelete = { onDeleteMaterial(material.id) }
                                )
                            }
                        }
                    }
                }

                StockTab.AUDIT -> {
                    StockAuditTabContent(
                        movements = uiState.filteredMovements,
                        selectedFilter = uiState.movementFilterType,
                        onFilterSelect = onMovementFilterSelect,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }

        // Dialogs
        if (uiState.isRestockDialogOpen) {
            AddStockBatchDialog(
                selectedChickenPart = uiState.restockChickenPart,
                activeTab = uiState.activeTab,
                onAddTaichan = onAddTaichan,
                onAddChicken = onAddChicken,
                onDismiss = onCloseRestock
            )
        }

        if (uiState.isAddEditMaterialOpen) {
            AddEditMaterialDialog(
                material = uiState.materialToEdit,
                onSave = onSaveMaterial,
                onDismiss = onCloseAddEditMaterial
            )
        }

        if (uiState.isRestockMaterialOpen && uiState.selectedMaterialForAction != null) {
            RestockMaterialDialog(
                material = uiState.selectedMaterialForAction!!,
                onConfirm = { qty, cost, expiryDate, notes ->
                    onConfirmRestockWithBatch(uiState.selectedMaterialForAction!!.id, qty, cost, expiryDate, notes)
                },
                onDismiss = onCloseRestockMaterial
            )
        }

        if (uiState.isBatchDetailOpen && uiState.selectedMaterialForBatches != null) {
            BatchDetailDialog(
                material = uiState.selectedMaterialForBatches!!,
                batches = uiState.getBatchesFor(uiState.selectedMaterialForBatches!!.id),
                onToggleUsable = onToggleBatchUsability,
                onOpenWaste = onOpenWasteForBatch,
                onDismiss = onCloseBatchDetail
            )
        }

        if (uiState.isWasteDialogOpen && uiState.selectedBatchForWaste != null && uiState.selectedMaterialForBatches != null) {
            WasteBatchDialog(
                material = uiState.selectedMaterialForBatches!!,
                batch = uiState.selectedBatchForWaste!!,
                onConfirm = { batchId, qty, notes ->
                    onConfirmBatchWaste(batchId, qty, notes)
                },
                onDismiss = onCloseWasteForBatch
            )
        }

        if (uiState.isWasteMaterialOpen && uiState.selectedMaterialForAction != null) {
            WasteMaterialDialog(
                material = uiState.selectedMaterialForAction!!,
                onConfirm = { qty, notes ->
                    onConfirmWasteMaterial(uiState.selectedMaterialForAction!!.id, qty, notes)
                },
                onDismiss = onCloseWasteMaterial
            )
        }

        if (uiState.isOpnameMaterialOpen && uiState.selectedMaterialForAction != null) {
            OpnameMaterialDialog(
                material = uiState.selectedMaterialForAction!!,
                onConfirm = { newStock, notes ->
                    onConfirmOpnameMaterial(uiState.selectedMaterialForAction!!.id, newStock, notes)
                },
                onDismiss = onCloseOpnameMaterial
            )
        }
    }
}

@Composable
fun CategoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) PrimarySoft else SurfaceWhite)
            .border(1.dp, if (isSelected) Primary else OutlineSoft, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
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
fun RawMaterialItemCard(
    material: RawMaterial,
    batches: List<MaterialBatch> = emptyList(),
    onRestock: () -> Unit,
    onOpenBatchDetail: () -> Unit = {},
    onWaste: () -> Unit,
    onOpname: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = (material.currentStock / (material.minStock * 2.0).coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    val isLow = material.isLowStock
    val hasExpired = batches.any { it.freshnessStatus() == BatchFreshness.EXPIRED || it.freshnessStatus() == BatchFreshness.DAMAGED }
    val hasExpiringSoon = !hasExpired && batches.any { it.freshnessStatus() == BatchFreshness.EXPIRING_SOON }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.Md)
            .background(SurfaceWhite)
            .border(
                1.dp,
                if (hasExpired) Danger else if (hasExpiringSoon || isLow) Warning else OutlineSoft,
                AppShapes.Md
            )
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasExpired) Color(0xFFFEE2E2)
                                else if (hasExpiringSoon || isLow) Color(0xFFFEF3C7)
                                else PrimarySoft
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasExpired || hasExpiringSoon || isLow) Icons.Default.Warning else Icons.Default.Inventory,
                            contentDescription = null,
                            tint = if (hasExpired) Danger else if (hasExpiringSoon || isLow) Warning else Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = material.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            if (hasExpired) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEE2E2))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Kadaluarsa",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Danger,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            } else if (hasExpiringSoon) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEF3C7))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Segera Exp",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF92400E),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            } else if (isLow) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEF3C7))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Menipis",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF92400E),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${material.category.displayName} • Min: ${material.minStock} ${material.unit}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Primary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Danger, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stock progress & values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sisa: ${material.currentStock} ${material.unit}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (hasExpired || isLow) Danger else Primary
                    )
                )
                if (material.costPerUnit > 0) {
                    Text(
                        text = "${CurrencyFormatter.formatRupiah(material.costPerUnit)} / ${material.unit}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (hasExpired || isLow) Danger else Primary,
                trackColor = SurfaceTint
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons row: + Masuk, Batch (N), - Rusak, Opname
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(AppShapes.Sm)
                        .background(PrimarySoft)
                        .border(1.dp, Primary, AppShapes.Sm)
                        .clickable(onClick = onRestock)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Masuk",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(AppShapes.Sm)
                        .background(
                            if (hasExpired) Color(0xFFFEE2E2)
                            else if (hasExpiringSoon) Color(0xFFFEF3C7)
                            else SurfaceTint
                        )
                        .border(
                            1.dp,
                            if (hasExpired) Danger
                            else if (hasExpiringSoon) Warning
                            else OutlineSoft,
                            AppShapes.Sm
                        )
                        .clickable(onClick = onOpenBatchDetail)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Batch (${batches.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (hasExpired) Danger else if (hasExpiringSoon) Color(0xFF92400E) else TextPrimary
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(AppShapes.Sm)
                        .background(SurfaceTint)
                        .border(1.dp, OutlineSoft, AppShapes.Sm)
                        .clickable(onClick = onWaste)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "- Rusak",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Danger
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(AppShapes.Sm)
                        .background(SurfaceTint)
                        .border(1.dp, OutlineSoft, AppShapes.Sm)
                        .clickable(onClick = onOpname)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Opname",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }
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
                        text = "Batch ${batch.date}",
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

private data class MovementBadgeStyle(
    val bgColor: Color,
    val textColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val deltaPrefix: String,
    val deltaColor: Color
)

@Composable
fun StockAuditTabContent(
    movements: List<StockMovement>,
    selectedFilter: StockMovementType?,
    onFilterSelect: (StockMovementType?) -> Unit,
    modifier: Modifier = Modifier
) {
    val filterOptions = listOf(
        null to "Semua",
        StockMovementType.ORDER_DEDUCTION to "Potong BOM",
        StockMovementType.RESTOCK to "Stok Masuk",
        StockMovementType.ADJUSTMENT to "Opname",
        StockMovementType.VOID_RESTORATION to "Void Restore",
        StockMovementType.WASTE to "Rusak / Basi"
    )

    Column(modifier = modifier) {
        // Horizontal filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filterOptions.forEach { (type, label) ->
                val isSelected = selectedFilter == type
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Primary else SurfaceWhite)
                        .border(1.dp, if (isSelected) Primary else OutlineSoft, CircleShape)
                        .clickable { onFilterSelect(type) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) SurfaceWhite else TextPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Total info bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${movements.size} Catatan Mutasi",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Text(
                text = "Log otomatis potong bahan & restock",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (movements.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = OutlineSoft,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Belum ada riwayat mutasi stok",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(movements, key = { it.id }) { movement ->
                    StockMovementCard(movement = movement)
                }
            }
        }
    }
}

@Composable
fun StockMovementCard(
    movement: StockMovement,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")) }
    val formattedDate = remember(movement.timestamp) { dateFormat.format(Date(movement.timestamp)) }

    val style = when (movement.type) {
        StockMovementType.RESTOCK -> {
            MovementBadgeStyle(Color(0xFFDCFCE7), Color(0xFF15803D), Icons.Default.ArrowDownward, "+", Color(0xFF15803D))
        }
        StockMovementType.ORDER_DEDUCTION -> {
            MovementBadgeStyle(PrimarySoft, Primary, Icons.Default.ArrowUpward, "-", Primary)
        }
        StockMovementType.ADJUSTMENT -> {
            MovementBadgeStyle(Color(0xFFEFF6FF), Color(0xFF1D4ED8), Icons.Default.Tune, "±", Color(0xFF1D4ED8))
        }
        StockMovementType.VOID_RESTORATION -> {
            MovementBadgeStyle(Color(0xFFFEF3C7), Color(0xFFD97706), Icons.Default.History, "+", Color(0xFFD97706))
        }
        StockMovementType.WASTE -> {
            MovementBadgeStyle(Color(0xFFFEE2E2), Color(0xFFDC2626), Icons.Default.DeleteSweep, "-", Color(0xFFDC2626))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.Md)
            .background(SurfaceWhite)
            .border(1.dp, OutlineSoft, AppShapes.Md)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(style.bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.textColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = movement.materialName.ifBlank { "Bahan Baku" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(style.bgColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = movement.type.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = style.textColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                if (movement.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = movement.notes,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                )
            }

            // Delta Qty
            Column(horizontalAlignment = Alignment.End) {
                val formattedQty = if (movement.quantity % 1.0 == 0.0) {
                    movement.quantity.toInt().toString()
                } else {
                    String.format(Locale.US, "%.1f", movement.quantity)
                }
                val deltaText: String = "${style.deltaPrefix}$formattedQty"
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = style.deltaColor
                    )
                )
                if (movement.costTotal > 0) {
                    Text(
                        text = CurrencyFormatter.formatRupiah(movement.costTotal),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}
