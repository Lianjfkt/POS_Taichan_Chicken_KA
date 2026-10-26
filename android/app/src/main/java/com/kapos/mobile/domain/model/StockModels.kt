package com.kapos.mobile.domain.model

enum class StockFreshness(val displayName: String, val isSellable: Boolean) {
    FRESH("Segar (Hari Ini)", true),
    DAY_2("Hari ke-2", true),
    DAY_3("Hari ke-3 ⚠️", true),
    EXPIRED("Tidak Layak Jual ❌", false)
}

enum class BatchFreshness(val displayName: String, val isUsable: Boolean) {
    FRESH("Segar", true),
    EXPIRING_SOON("Mendekati Kadaluarsa (<= 2 Hari)", true),
    EXPIRED("Kadaluarsa", false),
    DAMAGED("Tidak Layak / Rusak", false)
}

data class MaterialBatch(
    val id: String,
    val materialId: String,
    val batchNumber: String,
    val initialQuantity: Double,
    val remainingQuantity: Double,
    val costTotal: Double,
    val costPerUnit: Double,
    val receivedDate: Long = System.currentTimeMillis(),
    val expiryDate: Long? = null,
    val isUsable: Boolean = true,
    val notes: String = ""
) : java.io.Serializable {
    fun freshnessStatus(currentTime: Long = System.currentTimeMillis()): BatchFreshness {
        if (!isUsable) return BatchFreshness.DAMAGED
        val exp = expiryDate ?: return BatchFreshness.FRESH
        val diffDays = (exp - currentTime).toDouble() / (24.0 * 60 * 60 * 1000)
        return when {
            diffDays < 0.0 -> BatchFreshness.EXPIRED
            diffDays <= 2.0 -> BatchFreshness.EXPIRING_SOON
            else -> BatchFreshness.FRESH
        }
    }
}

enum class ChickenPart(val id: String, val displayName: String) {
    SAYAP("sayap", "Sayap"),
    PAHA_ATAS("paha_atas", "Paha Atas"),
    PAHA_BAWAH("paha_bawah", "Paha Bawah"),
    DADA("dada", "Dada")
}

interface BatchFreshnessCalculable {
    val timestamp: Long
    fun freshness(currentTimestamp: Long = System.currentTimeMillis()): StockFreshness {
        val diffDays = ((currentTimestamp - timestamp) / (24L * 60 * 60 * 1000)).toInt()
        return when {
            diffDays <= 0 -> StockFreshness.FRESH
            diffDays == 1 -> StockFreshness.DAY_2
            diffDays == 2 -> StockFreshness.DAY_3
            else -> StockFreshness.EXPIRED
        }
    }
}

data class TaichanBatch(
    val id: String,
    val date: String,
    override val timestamp: Long,
    val raw: Int,
    val fried: Int = 0,
    val notes: String = ""
) : BatchFreshnessCalculable

data class ChickenBatch(
    val id: String,
    val part: ChickenPart,
    val date: String,
    override val timestamp: Long,
    val raw: Int,
    val fried: Int = 0,
    val notes: String = ""
) : BatchFreshnessCalculable
