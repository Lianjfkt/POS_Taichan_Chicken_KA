package com.kapos.mobile.feature.pos

import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.Category
import com.kapos.mobile.domain.model.Product

data class PosUiState(
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val isCartSheetOpen: Boolean = false,
    val selectedProductForModifier: Product? = null,
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false
) {
    val filteredProducts: List<Product>
        get() {
            var list = products
            if (selectedCategoryId != null) {
                list = list.filter { it.categoryId == selectedCategoryId }
            }
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
                }
            }
            return list
        }

    val cartTotalCount: Int
        get() = cartItems.sumOf { it.quantity }

    val cartSubtotal: Double
        get() = cartItems.sumOf { it.subtotal }
}
