package com.kapos.mobile.feature.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.core.data.repository.SettingsRepository
import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.Order
import com.kapos.mobile.domain.model.OrderType
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.PaymentStatus
import com.kapos.mobile.domain.model.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CheckoutViewModel(
    private val repository: PosRepository,
    private val settingsRepository: SettingsRepository,
    private val bluetoothPrinterService: com.kapos.mobile.core.printer.BluetoothPrinterService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    fun initCheckout(items: List<CartItem>) {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
        val generatedOrderNo = "KA-${dateFormat.format(Date())}"
        val total = items.sumOf { it.subtotal }

        _uiState.update {
            it.copy(
                orderNo = generatedOrderNo,
                cartItems = items,
                cashGiven = total, // Default to exact cash
                discountInput = 0.0,
                isCompleted = false,
                isSubmitting = false
            )
        }
    }

    fun onOrderTypeChange(type: OrderType) {
        _uiState.update { it.copy(orderType = type) }
    }

    fun onTableNoChange(table: String) {
        _uiState.update { it.copy(tableNo = table) }
    }

    fun onCustomerNameChange(name: String) {
        _uiState.update { it.copy(customerName = name) }
    }

    fun onDiscountTypeChange(type: com.kapos.mobile.domain.model.DiscountType) {
        _uiState.update { it.copy(discountType = type) }
    }

    fun onDiscountInputChange(amount: Double) {
        _uiState.update { it.copy(discountInput = amount) }
    }

    fun onPaymentMethodChange(method: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun onCashGivenChange(amount: Double) {
        _uiState.update { it.copy(cashGiven = amount) }
    }

    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun submitOrder(onSuccess: (Order) -> Unit) {
        val state = _uiState.value
        if (state.cartItems.isEmpty() || !state.isCashSufficient) return

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.value
            val cashier = settings.cashierName
            val order = Order(
                id = UUID.randomUUID().toString(),
                orderNo = state.orderNo,
                customerName = state.customerName.ifBlank { null },
                orderType = state.orderType,
                tableNo = if (state.orderType == OrderType.DINE_IN) state.tableNo.ifBlank { null } else null,
                subtotal = state.subtotal,
                discount = state.discount,
                total = state.total,
                paymentMethod = state.paymentMethod,
                paymentStatus = PaymentStatus.PAID,
                cashierName = cashier,
                notes = state.notes.ifBlank { null },
                createdAt = System.currentTimeMillis(),
                paidAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_SYNC
            )

            val created = repository.submitOrder(order, state.cartItems)

            // Auto-print receipt if enabled and printer is paired
            if (settings.autoPrintCheckout && settings.printerMacAddress.isNotBlank()) {
                try {
                    val builder = com.kapos.mobile.core.printer.EscPosBuilder(settings.paperWidth)
                    val escPosItems = state.cartItems.map {
                        com.kapos.mobile.core.printer.EscPosItem(
                            name = it.product.name,
                            price = it.product.price,
                            quantity = it.quantity,
                            spiceLevel = it.spiceLevel,
                            notes = it.notes,
                            subtotal = it.subtotal
                        )
                    }
                    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(order.createdAt))
                    val receiptBytes = builder.buildReceipt(
                        storeName = settings.storeName,
                        address = settings.storeAddress,
                        phone = settings.storePhone,
                        receiptFooter = settings.receiptFooter,
                        orderNo = order.orderNo,
                        dateStr = dateStr,
                        orderType = order.orderType.displayName,
                        tableNo = order.tableNo,
                        cashierName = order.cashierName,
                        customerName = order.customerName,
                        items = escPosItems,
                        subtotal = order.subtotal,
                        discount = order.discount,
                        total = order.total,
                        paymentMethod = order.paymentMethod.displayName,
                        cashGiven = state.cashGiven,
                        change = state.changeDue
                    )
                    bluetoothPrinterService.printBytes(settings.printerMacAddress, receiptBytes)
                } catch (_: Exception) {}
            }

            _uiState.update { it.copy(isSubmitting = false, isCompleted = true) }
            onSuccess(created)
        }
    }
}
