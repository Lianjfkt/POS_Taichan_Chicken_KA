package com.kapos.mobile.domain.model

enum class StockFreshness(val displayName: String, val isSellable: Boolean) {
    FRESH("Segar (Hari Ini)", true),
    DAY_2("Hari ke-2", true),
    DAY_3("Hari ke-3 ⚠️", true),
    EXPIRED("Tidak Layak Jual ❌", false)
}

enum class ChickenPart(val id: String, val displayName: String) {
    SAYAP("sayap", "Sayap"),
    PAHA_ATAS("paha_atas", "Paha Atas"),
    PAHA_BAWAH("paha_bawah", "Paha Bawah"),
    DADA("dada", "Dada")
}

data class TaichanBatch(
    val id: String,
    val date: String,
    val timestamp: Long,
    val raw: Int,
    val fried: Int = 0,
    val notes: String = ""
) {
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

data class ChickenBatch(
    val id: String,
    val part: ChickenPart,
    val date: String,
    val timestamp: Long,
    val raw: Int,
    val fried: Int = 0,
    val notes: String = ""
) {
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
