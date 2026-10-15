package com.kapos.mobile.feature.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PosViewModel(
    private val repository: PosRepository,
    private val savedOrderRepository: com.kapos.mobile.core.data.repository.SavedOrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        viewModelScope.launch {
            repository.observeProducts().collect { products ->
                _uiState.update { it.copy(products = products) }
            }
        }
        viewModelScope.launch {
            repository.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
        viewModelScope.launch {
            repository.isSyncing.collect { syncing ->
                _uiState.update { it.copy(isSyncing = syncing) }
            }
        }
        viewModelScope.launch {
            savedOrderRepository.savedOrdersFlow.collect { savedList ->
                _uiState.update { it.copy(savedOrders = savedList) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSelectCategory(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun onProductClick(product: Product) {
        val requiresModifier = product.spiceLevel.isNotBlank() ||
                product.name.contains("Taichan", ignoreCase = true) ||
                product.name.contains("Chicken", ignoreCase = true) ||
                product.name.contains("Pedas", ignoreCase = true)

        if (requiresModifier) {
            _uiState.update { it.copy(selectedProductForModifier = product) }
        } else {
            addToCart(product, quantity = 1, spiceLevel = "", notes = "")
        }
    }

    fun dismissModifier() {
        _uiState.update { it.copy(selectedProductForModifier = null) }
    }

    fun confirmModifierAddToCart(product: Product, quantity: Int, spiceLevel: String, notes: String) {
        addToCart(product, quantity = quantity, spiceLevel = spiceLevel, notes = notes)
        dismissModifier()
    }

    fun addToCart(product: Product, quantity: Int = 1, spiceLevel: String = "", notes: String = "") {
        _uiState.update { state ->
            val existingIndex = state.cartItems.indexOfFirst {
                it.product.id == product.id && it.spiceLevel == spiceLevel && it.notes == notes
            }
            val updated = state.cartItems.toMutableList()
            if (existingIndex >= 0) {
                val existing = updated[existingIndex]
                updated[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
            } else {
                updated.add(CartItem(product = product, quantity = quantity, spiceLevel = spiceLevel, notes = notes))
            }
            state.copy(cartItems = updated)
        }
    }

    fun updateCartItemQuantity(index: Int, delta: Int) {
        _uiState.update { state ->
            if (index !in state.cartItems.indices) return@update state
            val updated = state.cartItems.toMutableList()
            val current = updated[index]
            val newQty = current.quantity + delta
            if (newQty <= 0) {
                updated.removeAt(index)
            } else {
                updated[index] = current.copy(quantity = newQty)
            }
            state.copy(cartItems = updated)
        }
    }

    fun removeCartItem(index: Int) {
        _uiState.update { state ->
            if (index !in state.cartItems.indices) return@update state
            val updated = state.cartItems.toMutableList()
            updated.removeAt(index)
            state.copy(cartItems = updated)
        }
    }

    fun clearCart() {
        _uiState.update { it.copy(cartItems = emptyList(), isCartSheetOpen = false) }
    }

    fun setCartSheetOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isCartSheetOpen = isOpen) }
    }

    // --- Hold / Saved Orders ---
    fun onOpenHoldDialog() {
        if (_uiState.value.cartItems.isEmpty()) return
        _uiState.update {
            it.copy(
                showHoldDialog = true,
                customerNameInput = "",
                tableNoInput = ""
            )
        }
    }

    fun onDismissHoldDialog() {
        _uiState.update { it.copy(showHoldDialog = false) }
    }

    fun onCustomerNameInputChange(name: String) {
        _uiState.update { it.copy(customerNameInput = name) }
    }

    fun onTableNoInputChange(table: String) {
        _uiState.update { it.copy(tableNoInput = table) }
    }

    fun confirmHoldOrder() {
        val state = _uiState.value
        if (state.cartItems.isEmpty()) return

        val customer = if (state.customerNameInput.isNotBlank()) state.customerNameInput.trim() else "Umum"
        val table = state.tableNoInput.trim()
        val order = com.kapos.mobile.domain.model.SavedOrder(
            id = "HOLD-" + System.currentTimeMillis(),
            customerName = customer,
            tableNo = table,
            items = state.cartItems,
            subtotal = state.cartSubtotal,
            createdAt = System.currentTimeMillis()
        )

        savedOrderRepository.saveOrder(order)
        _uiState.update {
            it.copy(
                cartItems = emptyList(),
                isCartSheetOpen = false,
                showHoldDialog = false,
                customerNameInput = "",
                tableNoInput = ""
            )
        }
    }

    fun onOpenSavedOrdersDialog() {
        _uiState.update { it.copy(showSavedOrdersDialog = true) }
    }

    fun onDismissSavedOrdersDialog() {
        _uiState.update { it.copy(showSavedOrdersDialog = false) }
    }

    fun recallSavedOrder(order: com.kapos.mobile.domain.model.SavedOrder) {
        savedOrderRepository.removeSavedOrder(order.id)
        _uiState.update {
            it.copy(
                cartItems = order.items,
                showSavedOrdersDialog = false,
                isCartSheetOpen = true
            )
        }
    }

    fun deleteSavedOrder(orderId: String) {
        savedOrderRepository.removeSavedOrder(orderId)
    }

    fun refreshData() {
        viewModelScope.launch {
            repository.refreshFromSupabase()
        }
    }
}

