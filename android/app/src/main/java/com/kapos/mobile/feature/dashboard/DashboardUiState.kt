package com.kapos.mobile.feature.dashboard

import com.kapos.mobile.domain.model.Order
import com.kapos.mobile.domain.model.OrderItem
import com.kapos.mobile.domain.model.PaymentStatus
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.Product
import com.kapos.mobile.domain.model.RawMaterial
import com.kapos.mobile.domain.model.calculatePaymentSplit
import java.util.Calendar

data class DashboardTopProduct(
    val name: String,
    val quantity: Int,
    val revenue: Double,
    val emoji: String = "🍢"
)

data class HourlySales(
    val hour: Int,   // 0..23
    val revenue: Double,
    val count: Int
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val storeName: String = "Taichan & Chicken KA",
    val ownerName: String = "Owner Gerai",
    val dailyRevenueTarget: Double = 1500000.0,
    val dailyFixedCost: Double = 350000.0,

    val orders: List<Order> = emptyList(),
    val orderItems: List<OrderItem> = emptyList(),
    val products: List<Product> = emptyList(),
    val rawMaterials: List<RawMaterial> = emptyList(),
) {
    // ─── Today's date range ──────────────────────────────────────────────────
    private val todayStart: Long
        get() {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }
    private val todayEnd: Long get() = System.currentTimeMillis()

    private val yesterdayStart: Long get() = todayStart - 86_400_000L
    private val yesterdayEnd: Long get() = todayStart - 1L

    // ─── Filtered order lists ────────────────────────────────────────────────
    val todayOrders: List<Order>
        get() = orders.filter {
            it.createdAt in todayStart..todayEnd && it.paymentStatus == PaymentStatus.PAID
        }

    val todayVoidOrders: List<Order>
        get() = orders.filter {
            it.createdAt in todayStart..todayEnd && it.paymentStatus == PaymentStatus.VOID
        }

    val yesterdayOrders: List<Order>
        get() = orders.filter {
            it.createdAt in yesterdayStart..yesterdayEnd && it.paymentStatus == PaymentStatus.PAID
        }

    // ─── Revenue KPIs ────────────────────────────────────────────────────────
    val todayRevenue: Double
        get() = todayOrders.sumOf { it.total }

    val yesterdayRevenue: Double
        get() = yesterdayOrders.sumOf { it.total }

    val revenueGrowthPercent: Double
        get() {
            val yRev = yesterdayRevenue
            return if (yRev > 0) ((todayRevenue - yRev) / yRev * 100.0) else 0.0
        }

    val bepProgressPercent: Float
        get() = if (dailyRevenueTarget > 0)
            (todayRevenue / dailyRevenueTarget * 100.0).toFloat().coerceIn(0f, 100f)
        else 0f

    val remainingToBep: Double
        get() = (dailyRevenueTarget - todayRevenue).coerceAtLeast(0.0)

    val isBepAchieved: Boolean
        get() = todayRevenue >= dailyRevenueTarget

    // ─── Profit KPIs ─────────────────────────────────────────────────────────
    private val todayOrderIds: Set<String>
        get() = todayOrders.map { it.id }.toSet()

    private val todayItems: List<OrderItem>
        get() = orderItems.filter { it.orderId in todayOrderIds }

    private val productCostMap: Map<String, Double>
        get() = products.associate { it.id to it.costPrice }

    val todayCogs: Double
        get() = todayItems.sumOf { item ->
            val cost = productCostMap[item.productId] ?: (item.price * 0.55)
            cost * item.quantity
        }

    val todayGrossProfit: Double
        get() = (todayRevenue - todayCogs).coerceAtLeast(0.0)

    val todayNetProfit: Double
        get() = todayGrossProfit - dailyFixedCost

    val grossMarginPercent: Int
        get() = if (todayRevenue > 0) ((todayGrossProfit / todayRevenue) * 100).toInt() else 0

    // ─── Transaction KPIs ────────────────────────────────────────────────────
    val todayTransactionCount: Int get() = todayOrders.size
    val todayVoidCount: Int get() = todayVoidOrders.size

    val averageOrderValue: Double
        get() = if (todayTransactionCount > 0) todayRevenue / todayTransactionCount else 0.0

    // ─── Payment split ───────────────────────────────────────────────────────
    private val paymentSplit get() = todayOrders.calculatePaymentSplit()
    val cashRevenue: Double get() = paymentSplit.cashTotal
    val qrisRevenue: Double get() = paymentSplit.qrisTotal
    val transferRevenue: Double get() = paymentSplit.transferTotal

    // ─── Top products ─────────────────────────────────────────────────────────
    val topProducts: List<DashboardTopProduct>
        get() {
            val emojiMap = products.associate { it.id to it.emoji }
            return todayItems
                .groupBy { it.productId to it.productName }
                .map { (key, items) ->
                    DashboardTopProduct(
                        name = key.second,
                        quantity = items.sumOf { it.quantity },
                        revenue = items.sumOf { it.subtotal },
                        emoji = emojiMap[key.first] ?: "🍢"
                    )
                }
                .sortedByDescending { it.quantity }
                .take(5)
        }

    // ─── Hourly sales ─────────────────────────────────────────────────────────
    val hourlySales: List<HourlySales>
        get() {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return (0..currentHour).map { h ->
                val hourOrders = todayOrders.filter { order ->
                    val cal = Calendar.getInstance().apply { timeInMillis = order.createdAt }
                    cal.get(Calendar.HOUR_OF_DAY) == h
                }
                HourlySales(h, hourOrders.sumOf { it.total }, hourOrders.size)
            }
        }

    val peakHour: HourlySales?
        get() = hourlySales.maxByOrNull { it.revenue }

    // ─── Stock alerts ─────────────────────────────────────────────────────────
    val lowStockCount: Int get() = rawMaterials.count { it.isLowStock }
    val expiredStockCount: Int get() = rawMaterials.count { it.hasExpiredBatch }
}
