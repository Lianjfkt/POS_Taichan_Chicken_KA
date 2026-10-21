package com.kapos.mobile.feature.stock

import com.kapos.mobile.domain.model.BatchFreshness
import com.kapos.mobile.domain.model.ChickenBatch
import com.kapos.mobile.domain.model.ChickenPart
import com.kapos.mobile.domain.model.MaterialBatch
import com.kapos.mobile.domain.model.RawMaterial
import com.kapos.mobile.domain.model.RawMaterialCategory
import com.kapos.mobile.domain.model.StockMovement
import com.kapos.mobile.domain.model.StockMovementType
import com.kapos.mobile.domain.model.TaichanBatch

enum class StockTab(val displayName: String) {
    INVENTORY("Bahan Baku & Batch"),
    AUDIT("Audit Mutasi & Waste"),
    @Deprecated("Legacy tab") TAICHAN("Sate Taichan"),
    @Deprecated("Legacy tab") CHICKEN("Ayam Potong"),
    @Deprecated("Legacy tab") MATERIALS("Bahan Baku (BOM)")
}

data class ChickenPartSummary(
    val part: ChickenPart,
    val totalRaw: Int = 0,
    val totalFried: Int = 0,
    val totalSold: Int = 0,
    val remaining: Int = 0
)

data class StockUiState(
    val activeTab: StockTab = StockTab.INVENTORY,
    // Unified Room DB Raw materials & Batches
    val rawMaterials: List<RawMaterial> = emptyList(),
    val batchesByMaterial: Map<String, List<MaterialBatch>> = emptyMap(),
    val selectedMaterialCategory: RawMaterialCategory? = null,
    val materialSearchQuery: String = "",
    val selectedMaterialForAction: RawMaterial? = null,
    val selectedMaterialForBatches: RawMaterial? = null,
    val isBatchDetailOpen: Boolean = false,
    val selectedBatchForWaste: MaterialBatch? = null,
    val isWasteDialogOpen: Boolean = false,
    val isAddEditMaterialOpen: Boolean = false,
    val materialToEdit: RawMaterial? = null,
    val isRestockMaterialOpen: Boolean = false,
    val isWasteMaterialOpen: Boolean = false,
    val isOpnameMaterialOpen: Boolean = false,
    // Stock Movements Audit Log
    val stockMovements: List<StockMovement> = emptyList(),
    val movementFilterType: StockMovementType? = null,
    // Legacy SharedPreferences compatibility
    val taichanBatches: List<TaichanBatch> = emptyList(),
    val chickenBatches: List<ChickenBatch> = emptyList(),
    val taichanSoldToday: Int = 0,
    val chickenSoldToday: Map<ChickenPart, Int> = emptyMap(),
    val isRestockDialogOpen: Boolean = false,
    val restockChickenPart: ChickenPart = ChickenPart.SAYAP
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
                val sold = chickenSoldToday[part] ?: 0
                val remaining = (fried - sold).coerceAtLeast(0)
                ChickenPartSummary(
                    part = part,
                    totalRaw = raw,
                    totalFried = fried,
                    totalSold = sold,
                    remaining = remaining
                )
            }
        }

    val filteredMovements: List<StockMovement>
        get() {
            val list = stockMovements
            return if (movementFilterType != null) {
                list.filter { it.type == movementFilterType }
            } else {
                list
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

    val totalInventoryAssetValue: Double
        get() = rawMaterials.sumOf { it.currentStock * it.costPerUnit }

    fun totalExpiredBatches(now: Long = System.currentTimeMillis()): Int {
        return batchesByMaterial.values.flatten().count {
            it.remainingQuantity > 0 && it.freshnessStatus(now) == BatchFreshness.EXPIRED
        }
    }

    fun totalExpiringSoonBatches(now: Long = System.currentTimeMillis()): Int {
        return batchesByMaterial.values.flatten().count {
            it.remainingQuantity > 0 && it.freshnessStatus(now) == BatchFreshness.EXPIRING_SOON
        }
    }

    fun getBatchesFor(materialId: String): List<MaterialBatch> {
        return batchesByMaterial[materialId] ?: emptyList()
    }
}
