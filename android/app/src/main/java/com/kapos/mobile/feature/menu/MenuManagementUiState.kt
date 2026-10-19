package com.kapos.mobile.feature.menu

import com.kapos.mobile.domain.model.Category
import com.kapos.mobile.domain.model.Product

enum class MenuTab(val title: String) {
    PRODUCTS("Daftar Produk"),
    CATEGORIES("Kategori Menu")
}

data class MenuManagementUiState(
    val selectedTab: MenuTab = MenuTab.PRODUCTS,
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val isLoading: Boolean = false,
    val isAddEditProductOpen: Boolean = false,
    val productToEdit: Product? = null,
    val productToDelete: Product? = null,
    val isAddEditCategoryOpen: Boolean = false,
    val categoryToEdit: Category? = null,
    val categoryToDelete: Category? = null
) {
    val filteredProducts: List<Product>
        get() {
            var list = products
            if (selectedCategoryId != null) {
                list = list.filter { it.categoryId == selectedCategoryId }
            }
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.lowercase().trim()
                list = list.filter { it.name.lowercase().contains(q) || it.description.lowercase().contains(q) }
            }
            return list
        }

    val activeProductCount: Int
        get() = products.count { it.isActive }

    val activeCategoryCount: Int
        get() = categories.count { it.isActive }
}
