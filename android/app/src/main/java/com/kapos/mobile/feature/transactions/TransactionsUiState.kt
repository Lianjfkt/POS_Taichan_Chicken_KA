package com.kapos.mobile.feature.transactions

import com.kapos.mobile.domain.model.Order
import com.kapos.mobile.domain.model.OrderItem
import com.kapos.mobile.domain.model.PaymentStatus

enum class TransactionFilter {
    ALL,
    PAID,
    UNPAID
}

data class TransactionsUiState(
    val searchQuery: String = "",
    val filter: TransactionFilter = TransactionFilter.ALL,
    val orders: List<Order> = emptyList(),
    val selectedOrder: Order? = null,
    val selectedOrderItems: List<OrderItem> = emptyList(),
    val isLoadingDetail: Boolean = false,
    val isSyncing: Boolean = false
) {
    val filteredOrders: List<Order>
        get() {
            var list = orders
            list = when (filter) {
                TransactionFilter.ALL -> list
                TransactionFilter.PAID -> list.filter { it.paymentStatus == PaymentStatus.PAID }
                TransactionFilter.UNPAID -> list.filter { it.paymentStatus != PaymentStatus.PAID }
            }
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.orderNo.lowercase().contains(q) ||
                    (it.customerName?.lowercase()?.contains(q) == true)
                }
            }
            return list
        }
}
