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
    private val repository: PosRepository
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
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSelectCategory(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun onProductClick(product: Product) {
        // Direct tap adds 1 to cart
        addToCart(product, spiceLevel = "Sedang", notes = "")
    }

    fun addToCart(product: Product, spiceLevel: String = "Sedang", notes: String = "") {
        _uiState.update { state ->
            val existingIndex = state.cartItems.indexOfFirst {
                it.product.id == product.id && it.spiceLevel == spiceLevel && it.notes == notes
            }
            val updated = state.cartItems.toMutableList()
            if (existingIndex >= 0) {
                val existing = updated[existingIndex]
                updated[existingIndex] = existing.copy(quantity = existing.quantity + 1)
            } else {
                updated.add(CartItem(product = product, quantity = 1, spiceLevel = spiceLevel, notes = notes))
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

    fun refreshData() {
        viewModelScope.launch {
            repository.refreshFromSupabase()
        }
    }
}
