package com.kapos.mobile.feature.stock

import com.kapos.mobile.domain.model.ChickenBatch
import com.kapos.mobile.domain.model.ChickenPart
import com.kapos.mobile.domain.model.RawMaterial
import com.kapos.mobile.domain.model.RawMaterialCategory
import com.kapos.mobile.domain.model.TaichanBatch

enum class StockTab(val displayName: String) {
    TAICHAN("Sate Taichan (Tusuk)"),
    CHICKEN("Ayam Potong"),
    MATERIALS("Bahan Baku (BOM)")
}

data class ChickenPartSummary(
    val part: ChickenPart,
    val totalRaw: Int = 0,
    val totalFried: Int = 0,
    val totalSold: Int = 0,
    val remaining: Int = 0
)

data class StockUiState(
    val activeTab: StockTab = StockTab.TAICHAN,
    val taichanBatches: List<TaichanBatch> = emptyList(),
    val chickenBatches: List<ChickenBatch> = emptyList(),
    val taichanSoldToday: Int = 0,
    val isRestockDialogOpen: Boolean = false,
    val restockChickenPart: ChickenPart = ChickenPart.SAYAP,
    // Raw materials BOM state
    val rawMaterials: List<RawMaterial> = emptyList(),
    val selectedMaterialCategory: RawMaterialCategory? = null,
    val materialSearchQuery: String = "",
    val selectedMaterialForAction: RawMaterial? = null,
    val isAddEditMaterialOpen: Boolean = false,
    val materialToEdit: RawMaterial? = null,
    val isRestockMaterialOpen: Boolean = false,
    val isWasteMaterialOpen: Boolean = false,
    val isOpnameMaterialOpen: Boolean = false
) {
    val taichanTotalRaw: Int
        get() = taichanBatches.sumOf { it.raw }

    val taichanTotalFried: Int
        get() = taichanBatches.sumOf { it.fried }

    val taichanRemaining: Int
        get() = (taichanTotalFried - taichanSoldToday).coerceAtLeast(0)

    val chickenSummaries: Map<ChickenPart, ChickenPartSummary>
        get() {
            return ChickenPart.values().associateWith { part ->
                val batches = chickenBatches.filter { it.part == part }
                val raw = batches.sumOf { it.raw }
                val fried = batches.sumOf { it.fried }
                ChickenPartSummary(
                    part = part,
                    totalRaw = raw,
                    totalFried = fried,
                    totalSold = 0,
                    remaining = fried
                )
            }
        }

    val filteredMaterials: List<RawMaterial>
        get() {
            var list = rawMaterials
            if (selectedMaterialCategory != null) {
                list = list.filter { it.category == selectedMaterialCategory }
            }
            if (materialSearchQuery.isNotBlank()) {
                val q = materialSearchQuery.lowercase().trim()
                list = list.filter { it.name.lowercase().contains(q) }
            }
            return list
        }

    val lowStockCount: Int
        get() = rawMaterials.count { it.isLowStock }

    val lowStockMaterials: List<RawMaterial>
        get() = rawMaterials.filter { it.isLowStock }
}
