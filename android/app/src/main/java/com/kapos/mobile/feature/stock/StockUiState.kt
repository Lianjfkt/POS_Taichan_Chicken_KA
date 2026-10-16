package com.kapos.mobile.feature.stock

import com.kapos.mobile.domain.model.ChickenBatch
import com.kapos.mobile.domain.model.ChickenPart
import com.kapos.mobile.domain.model.TaichanBatch

enum class StockTab(val displayName: String) {
    TAICHAN("Sate Taichan (Tusuk)"),
    CHICKEN("Ayam Potong")
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
                ChickenPartSummary(
                    part = part,
                    totalRaw = raw,
                    totalFried = fried,
                    totalSold = 0,
                    remaining = fried
                )
            }
        }
}
