package com.kapos.mobile.feature.stock

import com.kapos.mobile.domain.model.BatchFreshness
import com.kapos.mobile.domain.model.MaterialBatch
import com.kapos.mobile.domain.model.RawMaterial
import com.kapos.mobile.domain.model.RawMaterialCategory
import com.kapos.mobile.domain.model.StockMovement
import com.kapos.mobile.domain.model.StockMovementType

enum class StockTab(val displayName: String) {
    INVENTORY("Bahan Baku & Batch"),
    AUDIT("Audit Mutasi & Waste")
}

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
    val movementFilterType: StockMovementType? = null
) {
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
