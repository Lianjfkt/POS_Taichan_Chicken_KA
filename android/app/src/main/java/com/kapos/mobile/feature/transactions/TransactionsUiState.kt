package com.kapos.mobile.feature.transactions

import com.kapos.mobile.domain.model.DateFilterPreset
import com.kapos.mobile.domain.model.DateRangeFilter
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
    val dateFilter: DateRangeFilter = DateRangeFilter(preset = DateFilterPreset.TODAY),
    val showDatePickerDialog: Boolean = false,
    val orders: List<Order> = emptyList(),
    val selectedOrder: Order? = null,
    val selectedOrderItems: List<OrderItem> = emptyList(),
    val isLoadingDetail: Boolean = false,
    val isSyncing: Boolean = false,
    // VOID with PIN & Reason
    val showVoidPinDialog: Boolean = false,
    val orderToVoid: Order? = null,
    val voidPinInput: String = "",
    val voidPinError: String? = null,
    val selectedVoidReason: String = "Salah Input Menu",
    val voidReasonNotes: String = "",
    val voidReasonError: String? = null,
    val isVoiding: Boolean = false,
    val printFeedbackMessage: String? = null,
    val storeName: String = "🍢 Taichan & Chicken KA",
    val storeAddress: String = "Bandar Lampung"
) {
    val filteredOrders: List<Order>
        get() {
            var list = orders
            val (startCutoff, endCutoff) = dateFilter.getTimeRange()
            list = list.filter { it.createdAt in startCutoff..endCutoff }

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
