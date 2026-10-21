package com.kapos.mobile.core.data.repository

import com.kapos.mobile.domain.model.Promotion
import com.kapos.mobile.domain.model.PromotionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PromotionRepository {
    private val defaultPromotions = listOf(
        Promotion(
            id = "PROMO-HEMAT-5K",
            title = "Potongan Hemat Rp 5.000",
            description = "Potongan langsung Rp 5.000 min. belanja Rp 30.000",
            type = PromotionType.NOMINAL,
            value = 5000.0,
            minSpend = 30000.0,
            promoCode = "HEMAT5K",
            isActive = true
        ),
        Promotion(
            id = "PROMO-GAJIAN-10P",
            title = "Promo Gajian 10%",
            description = "Diskon 10% untuk transaksi minimal Rp 50.000",
            type = PromotionType.PERCENT,
            value = 10.0,
            minSpend = 50000.0,
            promoCode = "GAJIAN10",
            isActive = true
        ),
        Promotion(
            id = "PROMO-HAPPY-HOUR-15P",
            title = "Happy Hour Spesial 15%",
            description = "Diskon 15% khusus pembelian jam ramai",
            type = PromotionType.PERCENT,
            value = 15.0,
            minSpend = 40000.0,
            promoCode = "HAPPY15",
            isActive = true
        ),
        Promotion(
            id = "PROMO-SULTAN-10K",
            title = "Kupon Sultan Rp 10.000",
            description = "Potongan Rp 10.000 min. belanja Rp 75.000",
            type = PromotionType.NOMINAL,
            value = 10000.0,
            minSpend = 75000.0,
            promoCode = "SULTAN10K",
            isActive = true
        )
    )

    private val _promotions = MutableStateFlow(defaultPromotions)
    val promotions: Flow<List<Promotion>> = _promotions.asStateFlow()

    fun getActivePromotions(): List<Promotion> {
        return _promotions.value.filter { it.isActive }
    }
}
