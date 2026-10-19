package com.kapos.mobile.feature.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapos.mobile.core.data.repository.CustomerRepository
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.core.data.repository.SettingsRepository
import com.kapos.mobile.core.printer.BluetoothPrinterService
import com.kapos.mobile.core.printer.EscPosBuilder
import com.kapos.mobile.core.printer.EscPosItem
import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.Customer
import com.kapos.mobile.domain.model.DiscountType
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
    private val bluetoothPrinterService: BluetoothPrinterService,
    private val customerRepository: CustomerRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        customerRepository?.let { repo ->
            viewModelScope.launch {
                repo.observeCustomers().collect { customers ->
                    _uiState.update { state ->
                        // Also update selectedCustomer instance if its points or stats changed
                        val updatedSelected = state.selectedCustomer?.let { cur ->
                            customers.find { it.id == cur.id } ?: cur
                        }
                        state.copy(
                            availableCustomers = customers,
                            selectedCustomer = updatedSelected
                        )
                    }
                }
            }
        }
    }

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
                selectedCustomer = null,
                customerName = "",
                customerPhone = "",
                isRedeemingPoints = false,
                pointsToRedeem = 0,
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
        _uiState.update {
            it.copy(
                customerName = name,
                selectedCustomer = if (it.selectedCustomer?.name != name) null else it.selectedCustomer
            )
        }
    }

    fun onSelectCustomer(customer: Customer?) {
        _uiState.update { state ->
            if (customer != null) {
                state.copy(
                    selectedCustomer = customer,
                    customerName = customer.name,
                    customerPhone = customer.phone,
                    isRedeemingPoints = false,
                    pointsToRedeem = 0,
                    isSelectCustomerDialogOpen = false
                )
            } else {
                state.copy(
                    selectedCustomer = null,
                    customerName = "",
                    customerPhone = "",
                    isRedeemingPoints = false,
                    pointsToRedeem = 0,
                    isSelectCustomerDialogOpen = false
                )
            }
        }
    }

    fun onToggleRedeemPoints(redeem: Boolean) {
        _uiState.update { state ->
            val points = if (redeem) state.maxPointsCanRedeem else 0
            state.copy(
                isRedeemingPoints = redeem,
                pointsToRedeem = points
            )
        }
    }

    fun onPointsToRedeemChange(points: Int) {
        _uiState.update { state ->
            val valid = points.coerceIn(0, state.maxPointsCanRedeem)
            state.copy(
                pointsToRedeem = valid,
                isRedeemingPoints = valid > 0
            )
        }
    }

    fun setSelectCustomerDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isSelectCustomerDialogOpen = open) }
    }

    fun setQuickAddCustomerDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isQuickAddCustomerDialogOpen = open) }
    }

    fun quickAddCustomer(name: String, phone: String) {
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        if (trimmedName.isBlank()) return

        viewModelScope.launch {
            val repo = customerRepository ?: return@launch
            val newCustomer = Customer(
                id = UUID.randomUUID().toString(),
                name = trimmedName,
                phone = trimmedPhone,
                points = 0,
                totalSpend = 0.0,
                orderCount = 0
            )
            val saved = repo.saveCustomer(newCustomer)
            _uiState.update {
                it.copy(
                    selectedCustomer = saved,
                    customerName = saved.name,
                    customerPhone = saved.phone,
                    isQuickAddCustomerDialogOpen = false,
                    isSelectCustomerDialogOpen = false
                )
            }
        }
    }

    fun onDiscountTypeChange(type: DiscountType) {
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
            val finalCustomerName = state.customerName.ifBlank {
                state.selectedCustomer?.name
            }?.ifBlank { null }
            val finalCustomerPhone = state.customerPhone.ifBlank {
                state.selectedCustomer?.phone
            }?.ifBlank { null }

            val order = Order(
                id = UUID.randomUUID().toString(),
                orderNo = state.orderNo,
                customerName = finalCustomerName,
                customerPhone = finalCustomerPhone,
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

            // Update customer loyalty points & spend statistics if customer was selected
            state.selectedCustomer?.let { customer ->
                val pointsRedeemed = if (state.isRedeemingPoints) state.pointsToRedeem else 0
                val pointsEarned = state.pointsEarned
                customerRepository?.recordCustomerTransaction(
                    customerId = customer.id,
                    totalSpent = order.total,
                    pointsRedeemed = pointsRedeemed,
                    pointsEarned = pointsEarned
                )
            }

            // Auto-print receipt if enabled and printer is paired
            if (settings.autoPrintCheckout && settings.printerMacAddress.isNotBlank()) {
                try {
                    val builder = EscPosBuilder(settings.paperWidth)
                    val escPosItems = state.cartItems.map {
                        EscPosItem(
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
