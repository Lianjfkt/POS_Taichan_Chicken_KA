package com.kapos.mobile.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.domain.model.Category
import com.kapos.mobile.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

import com.kapos.mobile.core.data.repository.InventoryRepository
import kotlinx.coroutines.Job

class MenuManagementViewModel(
    private val posRepository: PosRepository,
    private val inventoryRepository: InventoryRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuManagementUiState(isLoading = true))
    val uiState: StateFlow<MenuManagementUiState> = _uiState.asStateFlow()
    private var recipeObservationJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            posRepository.observeAllProducts(includeInactive = true).collect { prods ->
                _uiState.update { it.copy(products = prods, isLoading = false) }
            }
        }
        viewModelScope.launch {
            posRepository.observeAllCategories(includeInactive = true).collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
        inventoryRepository?.let { inv ->
            viewModelScope.launch {
                inv.observeRawMaterials().collect { mats ->
                    _uiState.update { it.copy(allRawMaterials = mats) }
                }
            }
        }
    }

    fun onTabSelected(tab: MenuTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onCategoryFilterChange(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    // --- Product Actions ---
    fun openAddProduct() {
        _uiState.update { it.copy(isAddEditProductOpen = true, productToEdit = null) }
    }

    fun openEditProduct(product: Product) {
        _uiState.update { it.copy(isAddEditProductOpen = true, productToEdit = product) }
    }

    fun closeAddEditProductDialog() {
        _uiState.update { it.copy(isAddEditProductOpen = false, productToEdit = null) }
    }

    fun saveProduct(
        name: String,
        categoryId: String?,
        price: Double,
        costPrice: Double,
        emoji: String,
        spiceLevel: String,
        description: String,
        isActive: Boolean
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return

        val currentEdit = _uiState.value.productToEdit
        viewModelScope.launch {
            val product = if (currentEdit != null) {
                currentEdit.copy(
                    name = trimmedName,
                    categoryId = categoryId,
                    price = price.coerceAtLeast(0.0),
                    costPrice = costPrice.coerceAtLeast(0.0),
                    emoji = emoji.ifBlank { "🍢" },
                    spiceLevel = spiceLevel,
                    description = description.trim(),
                    isActive = isActive
                )
            } else {
                Product(
                    id = UUID.randomUUID().toString(),
                    name = trimmedName,
                    categoryId = categoryId,
                    price = price.coerceAtLeast(0.0),
                    costPrice = costPrice.coerceAtLeast(0.0),
                    emoji = emoji.ifBlank { "🍢" },
                    spiceLevel = spiceLevel,
                    description = description.trim(),
                    isActive = isActive
                )
            }
            posRepository.saveProduct(product)
            closeAddEditProductDialog()
        }
    }

    fun toggleProductActive(product: Product, isActive: Boolean) {
        viewModelScope.launch {
            posRepository.toggleProductActive(product.id, isActive)
        }
    }

    fun openDeleteProduct(product: Product) {
        _uiState.update { it.copy(productToDelete = product) }
    }

    fun closeDeleteProductDialog() {
        _uiState.update { it.copy(productToDelete = null) }
    }

    fun confirmDeleteProduct() {
        val toDelete = _uiState.value.productToDelete ?: return
        viewModelScope.launch {
            posRepository.deleteProduct(toDelete.id)
            closeDeleteProductDialog()
        }
    }

    // --- Category Actions ---
    fun openAddCategory() {
        _uiState.update { it.copy(isAddEditCategoryOpen = true, categoryToEdit = null) }
    }

    fun openEditCategory(category: Category) {
        _uiState.update { it.copy(isAddEditCategoryOpen = true, categoryToEdit = category) }
    }

    fun closeAddEditCategoryDialog() {
        _uiState.update { it.copy(isAddEditCategoryOpen = false, categoryToEdit = null) }
    }

    fun saveCategory(
        name: String,
        emoji: String,
        color: String,
        sortOrder: Int,
        isActive: Boolean
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return

        val currentEdit = _uiState.value.categoryToEdit
        viewModelScope.launch {
            val category = if (currentEdit != null) {
                currentEdit.copy(
                    name = trimmedName,
                    emoji = emoji.ifBlank { "🍽️" },
                    color = color.ifBlank { "#0B5C73" },
                    sortOrder = sortOrder,
                    isActive = isActive
                )
            } else {
                Category(
                    id = UUID.randomUUID().toString(),
                    name = trimmedName,
                    emoji = emoji.ifBlank { "🍽️" },
                    color = color.ifBlank { "#0B5C73" },
                    sortOrder = sortOrder,
                    isActive = isActive
                )
            }
            posRepository.saveCategory(category)
            closeAddEditCategoryDialog()
        }
    }

    fun toggleCategoryActive(category: Category, isActive: Boolean) {
        viewModelScope.launch {
            posRepository.toggleCategoryActive(category.id, isActive)
        }
    }

    fun openDeleteCategory(category: Category) {
        _uiState.update { it.copy(categoryToDelete = category) }
    }

    fun closeDeleteCategoryDialog() {
        _uiState.update { it.copy(categoryToDelete = null) }
    }

    fun confirmDeleteCategory() {
        val toDelete = _uiState.value.categoryToDelete ?: return
        viewModelScope.launch {
            posRepository.deleteCategory(toDelete.id)
            closeDeleteCategoryDialog()
        }
    }

    // --- Recipe BOM Actions ---
    fun openRecipeDialog(product: Product) {
        recipeObservationJob?.cancel()
        _uiState.update { it.copy(selectedProductForRecipe = product, recipeItems = emptyList()) }
        inventoryRepository?.let { inv ->
            recipeObservationJob = viewModelScope.launch {
                inv.observeRecipesForProduct(product.id).collect { items ->
                    _uiState.update { it.copy(recipeItems = items) }
                }
            }
        }
    }

    fun closeRecipeDialog() {
        recipeObservationJob?.cancel()
        _uiState.update { it.copy(selectedProductForRecipe = null, recipeItems = emptyList()) }
    }

    fun addOrUpdateRecipeItem(productId: String, materialId: String, quantityRequired: Double) {
        viewModelScope.launch {
            inventoryRepository?.saveRecipeItem(productId, materialId, quantityRequired)
        }
    }

    fun deleteRecipeItem(recipeId: String) {
        viewModelScope.launch {
            inventoryRepository?.deleteRecipeItem(recipeId)
        }
    }
}
