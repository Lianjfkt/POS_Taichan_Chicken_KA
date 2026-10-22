package com.kapos.mobile.feature.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapos.mobile.core.components.PrimaryButton
import com.kapos.mobile.core.components.SecondaryButton
import com.kapos.mobile.core.theme.AppShapes
import com.kapos.mobile.core.theme.Danger
import com.kapos.mobile.core.theme.OutlineSoft
import com.kapos.mobile.core.theme.Primary
import com.kapos.mobile.core.theme.PrimarySoft
import com.kapos.mobile.core.theme.ScreenBackground
import com.kapos.mobile.core.theme.Success
import com.kapos.mobile.core.theme.SurfaceTint
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.core.utils.CurrencyFormatter
import com.kapos.mobile.domain.model.Category
import com.kapos.mobile.domain.model.ModifierCategory
import com.kapos.mobile.domain.model.ModifierOption
import com.kapos.mobile.domain.model.Product

@Composable
fun MenuManagementRoute(
    viewModel: MenuManagementViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    MenuManagementScreen(
        uiState = uiState,
        onBack = onBack,
        onTabSelected = viewModel::onTabSelected,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onCategoryFilterChange = viewModel::onCategoryFilterChange,
        onOpenAddProduct = viewModel::openAddProduct,
        onOpenEditProduct = viewModel::openEditProduct,
        onCloseAddEditProduct = viewModel::closeAddEditProductDialog,
        onSaveProduct = viewModel::saveProduct,
        onToggleProductActive = viewModel::toggleProductActive,
        onOpenDeleteProduct = viewModel::openDeleteProduct,
        onCloseDeleteProduct = viewModel::closeDeleteProductDialog,
        onConfirmDeleteProduct = viewModel::confirmDeleteProduct,
        onOpenAddCategory = viewModel::openAddCategory,
        onOpenEditCategory = viewModel::openEditCategory,
        onCloseAddEditCategory = viewModel::closeAddEditCategoryDialog,
        onSaveCategory = viewModel::saveCategory,
        onToggleCategoryActive = viewModel::toggleCategoryActive,
        onOpenDeleteCategory = viewModel::openDeleteCategory,
        onCloseDeleteCategory = viewModel::closeDeleteCategoryDialog,
        onConfirmDeleteCategory = viewModel::confirmDeleteCategory,
        onOpenRecipe = viewModel::openRecipeDialog,
        onCloseRecipe = viewModel::closeRecipeDialog,
        onAddRecipeItem = viewModel::addOrUpdateRecipeItem,
        onDeleteRecipeItem = viewModel::deleteRecipeItem,
        onModifierFilterChange = viewModel::onSelectModifierFilter,
        onOpenAddModifier = viewModel::openAddModifier,
        onOpenEditModifier = viewModel::openEditModifier,
        onCloseAddEditModifier = viewModel::closeAddEditModifierDialog,
        onSaveModifier = viewModel::saveModifier,
        onToggleModifierActive = viewModel::toggleModifierActive,
        onOpenDeleteModifier = viewModel::openDeleteModifierDialog,
        onCloseDeleteModifier = viewModel::closeDeleteModifierDialog,
        onConfirmDeleteModifier = viewModel::confirmDeleteModifier,
        modifier = modifier
    )
}

@Composable
fun MenuManagementScreen(
    uiState: MenuManagementUiState,
    onBack: (() -> Unit)? = null,
    onTabSelected: (MenuTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onOpenAddProduct: () -> Unit,
    onOpenEditProduct: (Product) -> Unit,
    onCloseAddEditProduct: () -> Unit,
    onSaveProduct: (name: String, categoryId: String?, price: Double, costPrice: Double, emoji: String, spiceLevel: String, description: String, isActive: Boolean, allowSpiceLevel: Boolean, allowSambal: Boolean, allowComplimentary: Boolean, allowToppings: Boolean) -> Unit,
    onToggleProductActive: (Product, Boolean) -> Unit,
    onOpenDeleteProduct: (Product) -> Unit,
    onCloseDeleteProduct: () -> Unit,
    onConfirmDeleteProduct: () -> Unit,
    onOpenAddCategory: () -> Unit,
    onOpenEditCategory: (Category) -> Unit,
    onCloseAddEditCategory: () -> Unit,
    onSaveCategory: (name: String, emoji: String, color: String, sortOrder: Int, isActive: Boolean) -> Unit,
    onToggleCategoryActive: (Category, Boolean) -> Unit,
    onOpenDeleteCategory: (Category) -> Unit,
    onCloseDeleteCategory: () -> Unit,
    onConfirmDeleteCategory: () -> Unit,
    onOpenRecipe: (Product) -> Unit = {},
    onCloseRecipe: () -> Unit = {},
    onAddRecipeItem: (productId: String, materialId: String, quantity: Double) -> Unit = { _, _, _ -> },
    onDeleteRecipeItem: (recipeId: String) -> Unit = {},
    onModifierFilterChange: (ModifierCategory?) -> Unit = {},
    onOpenAddModifier: () -> Unit = {},
    onOpenEditModifier: (ModifierOption) -> Unit = {},
    onCloseAddEditModifier: () -> Unit = {},
    onSaveModifier: (name: String, price: Double, category: ModifierCategory, isDefault: Boolean) -> Unit = { _, _, _, _ -> },
    onToggleModifierActive: (id: String, isActive: Boolean) -> Unit = { _, _ -> },
    onOpenDeleteModifier: (ModifierOption) -> Unit = {},
    onCloseDeleteModifier: () -> Unit = {},
    onConfirmDeleteModifier: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .padding(bottom = 90.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Column {
                        Text(
                            text = "Kelola Menu & Kategori",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Atur produk, harga jual, HPP modal & kategori",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Primary)
                        .clickable {
                            when (uiState.selectedTab) {
                                MenuTab.PRODUCTS -> onOpenAddProduct()
                                MenuTab.CATEGORIES -> onOpenAddCategory()
                                MenuTab.MODIFIERS -> onOpenAddModifier()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = SurfaceWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (uiState.selectedTab) {
                                MenuTab.PRODUCTS -> "Tambah Produk"
                                MenuTab.CATEGORIES -> "Tambah Kategori"
                                MenuTab.MODIFIERS -> "Tambah Varian"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SurfaceWhite,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Tabs Selector: Produk | Kategori | Modifier
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MenuTabItem(
                    label = "Produk (${uiState.products.size})",
                    isSelected = uiState.selectedTab == MenuTab.PRODUCTS,
                    onClick = { onTabSelected(MenuTab.PRODUCTS) },
                    modifier = Modifier.weight(1f)
                )
                MenuTabItem(
                    label = "Kategori (${uiState.categories.size})",
                    isSelected = uiState.selectedTab == MenuTab.CATEGORIES,
                    onClick = { onTabSelected(MenuTab.CATEGORIES) },
                    modifier = Modifier.weight(1f)
                )
                MenuTabItem(
                    label = "Varian (${uiState.modifiers.size})",
                    isSelected = uiState.selectedTab == MenuTab.MODIFIERS,
                    onClick = { onTabSelected(MenuTab.MODIFIERS) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.selectedTab == MenuTab.PRODUCTS) {
                // Products Tab UI
                // Category Filter chips
                val filterScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(filterScrollState)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryChip(
                        name = "Semua (${uiState.products.size})",
                        isSelected = uiState.selectedCategoryId == null,
                        onClick = { onCategoryFilterChange(null) }
                    )
                    uiState.categories.forEach { cat ->
                        val count = uiState.products.count { it.categoryId == cat.id }
                        CategoryChip(
                            name = "${cat.emoji} ${cat.name} ($count)",
                            isSelected = uiState.selectedCategoryId == cat.id,
                            onClick = { onCategoryFilterChange(cat.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Cari nama produk...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Hapus", tint = TextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        shape = AppShapes.Sm,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = OutlineSoft,
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else if (uiState.filteredProducts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Tidak ada produk ditemukan", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.filteredProducts, key = { it.id }) { product ->
                            val category = uiState.categories.find { it.id == product.categoryId }
                            ProductItemCard(
                                product = product,
                                category = category,
                                onToggleActive = { active -> onToggleProductActive(product, active) },
                                onEdit = { onOpenEditProduct(product) },
                                onDelete = { onOpenDeleteProduct(product) },
                                onOpenRecipe = { onOpenRecipe(product) }
                            )
                        }
                    }
                }
            } else if (uiState.selectedTab == MenuTab.CATEGORIES) {
                // Categories Tab UI
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else if (uiState.categories.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Belum ada kategori", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.categories, key = { it.id }) { category ->
                            val productCount = uiState.products.count { it.categoryId == category.id }
                            CategoryItemCard(
                                category = category,
                                productCount = productCount,
                                onToggleActive = { active -> onToggleCategoryActive(category, active) },
                                onEdit = { onOpenEditCategory(category) },
                                onDelete = { onOpenDeleteCategory(category) }
                            )
                        }
                    }
                }
            } else {
                // Modifiers Tab UI
                val modFilterScroll = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(modFilterScroll)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryChip(
                        name = "Semua (${uiState.modifiers.size})",
                        isSelected = uiState.selectedModifierCategoryFilter == null,
                        onClick = { onModifierFilterChange(null) }
                    )
                    ModifierCategory.values().forEach { cat ->
                        val count = uiState.modifiers.count { it.category == cat }
                        CategoryChip(
                            name = "${cat.displayName} ($count)",
                            isSelected = uiState.selectedModifierCategoryFilter == cat,
                            onClick = { onModifierFilterChange(cat) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else if (uiState.filteredModifiers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Belum ada varian / modifier", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.filteredModifiers, key = { it.id }) { modifier ->
                            ModifierItemCard(
                                modifier = modifier,
                                onToggleActive = { active -> onToggleModifierActive(modifier.id, active) },
                                onEdit = { onOpenEditModifier(modifier) },
                                onDelete = { onOpenDeleteModifier(modifier) }
                            )
                        }
                    }
                }
            }
        }

        // Product Dialogs
        if (uiState.isAddEditProductOpen) {
            AddEditProductDialog(
                product = uiState.productToEdit,
                categories = uiState.categories,
                onSave = onSaveProduct,
                onDismiss = onCloseAddEditProduct
            )
        }

        if (uiState.productToDelete != null) {
            val toDelete = uiState.productToDelete
            DeleteConfirmDialog(
                title = "Hapus Produk?",
                message = "Apakah Anda yakin ingin menghapus produk '${toDelete.name}'?",
                onConfirm = onConfirmDeleteProduct,
                onDismiss = onCloseDeleteProduct
            )
        }

        // Category Dialogs
        if (uiState.isAddEditCategoryOpen) {
            AddEditCategoryDialog(
                category = uiState.categoryToEdit,
                onSave = onSaveCategory,
                onDismiss = onCloseAddEditCategory
            )
        }

        if (uiState.categoryToDelete != null) {
            val toDelete = uiState.categoryToDelete
            DeleteConfirmDialog(
                title = "Hapus Kategori?",
                message = "Apakah Anda yakin ingin menghapus kategori '${toDelete.name}'?",
                onConfirm = onConfirmDeleteCategory,
                onDismiss = onCloseDeleteCategory
            )
        }

        if (uiState.selectedProductForRecipe != null) {
            val prod = uiState.selectedProductForRecipe
            RecipeBomDialog(
                product = prod,
                recipes = uiState.recipeItems,
                allMaterials = uiState.allRawMaterials,
                onAddRecipeItem = { matId, qty -> onAddRecipeItem(prod.id, matId, qty) },
                onDeleteRecipeItem = onDeleteRecipeItem,
                onDismiss = onCloseRecipe
            )
        }

        // Modifier Dialogs
        if (uiState.isAddEditModifierOpen) {
            AddEditModifierDialog(
                modifier = uiState.modifierToEdit,
                onSave = onSaveModifier,
                onDismiss = onCloseAddEditModifier
            )
        }

        if (uiState.modifierToDelete != null) {
            val toDelete = uiState.modifierToDelete
            DeleteConfirmDialog(
                title = "Hapus Varian/Modifier?",
                message = "Apakah Anda yakin ingin menghapus '${toDelete.name}'?",
                onConfirm = onConfirmDeleteModifier,
                onDismiss = onCloseDeleteModifier
            )
        }
    }
}

@Composable
fun MenuTabItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(AppShapes.Sm)
            .background(if (isSelected) Primary else SurfaceWhite)
            .border(1.dp, if (isSelected) Primary else OutlineSoft, AppShapes.Sm)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isSelected) SurfaceWhite else TextSecondary
            )
        )
    }
}

@Composable
fun CategoryChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) PrimarySoft else SurfaceWhite)
            .border(1.dp, if (isSelected) Primary else OutlineSoft, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Primary else TextPrimary
            )
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    category: Category?,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenRecipe: () -> Unit = {}
) {
    val profit = product.price - product.costPrice
    val marginPercent = if (product.price > 0) ((profit / product.price) * 100).toInt() else 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.Md)
            .background(SurfaceWhite)
            .border(1.dp, OutlineSoft, AppShapes.Md)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SurfaceTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = product.emoji, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (product.isActive) TextPrimary else TextSecondary
                                )
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (category != null) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            if (product.spiceLevel.isNotBlank()) {
                                Text(text = "•", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                                Text(
                                    text = product.spiceLevel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Danger,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = product.isActive,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SurfaceWhite,
                            checkedTrackColor = Primary
                        )
                    )
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Danger, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing & Margin Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column {
                        Text(text = "Harga Jual", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
                        Text(
                            text = CurrencyFormatter.formatRupiah(product.price),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Primary)
                        )
                    }
                    Column {
                        Text(text = "BOP (HPP)", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
                        Text(
                            text = CurrencyFormatter.formatRupiah(product.costPrice),
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(AppShapes.Sm)
                            .background(SurfaceTint)
                            .clickable(onClick = onOpenRecipe)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "📋 Resep BOM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }

                    val marginBgColor = when {
                        marginPercent < 0 -> Color(0xFFFEE2E2)
                        marginPercent < 20 -> Color(0xFFFEF3C7)
                        else -> Color(0xFFDCFCE7)
                    }
                    val marginTextColor = when {
                        marginPercent < 0 -> Color(0xFF991B1B)
                        marginPercent < 20 -> Color(0xFF92400E)
                        else -> Color(0xFF166534)
                    }
                    val marginText = when {
                        marginPercent < 0 -> "$marginPercent% Rugi"
                        else -> "+$marginPercent% Margin"
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(marginBgColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = marginText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = marginTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItemCard(
    category: Category,
    productCount: Int,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.Md)
            .background(SurfaceWhite)
            .border(1.dp, OutlineSoft, AppShapes.Md)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SurfaceTint),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = category.emoji, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (category.isActive) TextPrimary else TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceTint)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Urutan #${category.sortOrder}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TextSecondary)
                            )
                        }
                    }
                    Text(
                        text = "$productCount Produk terdaftar",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = category.isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SurfaceWhite,
                        checkedTrackColor = Primary
                    )
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Danger, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    product: Product?,
    categories: List<Category>,
    onSave: (name: String, categoryId: String?, price: Double, costPrice: Double, emoji: String, spiceLevel: String, description: String, isActive: Boolean, allowSpiceLevel: Boolean, allowSambal: Boolean, allowComplimentary: Boolean, allowToppings: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var selectedCategoryId by remember { mutableStateOf(product?.categoryId ?: categories.firstOrNull()?.id) }
    var priceStr by remember { mutableStateOf(if (product != null) product.price.toLong().toString() else "") }
    var costPriceStr by remember { mutableStateOf(if (product != null) product.costPrice.toLong().toString() else "") }
    var emoji by remember { mutableStateOf(product?.emoji ?: "🍢") }
    var spiceLevel by remember { mutableStateOf(product?.spiceLevel ?: "Sedang") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var isActive by remember { mutableStateOf(product?.isActive ?: true) }
    var allowSpiceLevel by remember { mutableStateOf(product?.allowSpiceLevel ?: false) }
    var allowSambal by remember { mutableStateOf(product?.allowSambal ?: false) }
    var allowComplimentary by remember { mutableStateOf(product?.allowComplimentary ?: false) }
    var allowToppings by remember { mutableStateOf(product?.allowToppings ?: false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val emojiOptions = listOf("🍢", "🍗", "🍚", "🌶️", "🥤", "🥟", "🍟", "🥩", "🍜", "🧃", "☕", "🥣")
    val spiceOptions = listOf("Tidak Pedas", "Sedang", "Pedas", "Ekstra Pedas")

    val price = priceStr.toDoubleOrNull() ?: 0.0
    val costPrice = costPriceStr.toDoubleOrNull() ?: 0.0
    val margin = if (price > 0) (((price - costPrice) / price) * 100).toInt() else 0

    BasicAlertDialog(onDismissRequest = onDismiss) {
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.Lg)
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, AppShapes.Lg)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (product == null) "Tambah Produk Baru" else "Edit Produk",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Nama Produk *") },
                    placeholder = { Text("Contoh: Sate Taichan Kulit") },
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Kategori Selector
                Text(text = "Kategori Produk", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) PrimarySoft else SurfaceTint)
                                .border(1.dp, if (isSelected) Primary else OutlineSoft, CircleShape)
                                .clickable { selectedCategoryId = cat.id }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${cat.emoji} ${cat.name}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Primary else TextPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Harga Jual & HPP
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Harga Jual (Rp) *") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = AppShapes.Sm,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = costPriceStr,
                        onValueChange = { costPriceStr = it.filter { c -> c.isDigit() } },
                        label = { Text("HPP Modal (Rp)") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = AppShapes.Sm,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (price > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Estimasi Margin: +$margin% (Laba Bersih ${CurrencyFormatter.formatRupiah(price - costPrice)})",
                        style = MaterialTheme.typography.labelSmall.copy(color = Success, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Emoji Picker
                Text(text = "Pilih Ikon Emoji", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    emojiOptions.forEach { em ->
                        val isSelected = emoji == em
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimarySoft else SurfaceTint)
                                .border(1.dp, if (isSelected) Primary else OutlineSoft, CircleShape)
                                .clickable { emoji = em },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = em, fontSize = 18.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Level Pedas
                Text(text = "Tingkat Pedas", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    spiceOptions.forEach { sp ->
                        val isSelected = spiceLevel == sp
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) Danger.copy(alpha = 0.15f) else SurfaceTint)
                                .border(1.dp, if (isSelected) Danger else OutlineSoft, CircleShape)
                                .clickable { spiceLevel = sp }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = sp,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Danger else TextPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi Singkat (Opsional)") },
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Opsi Modifier Tersedia untuk Menu Ini
                Text(
                    text = "Opsi Customisasi (tampil saat order)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Triple("🌶️", "Level Pedas", Triple(allowSpiceLevel, { v: Boolean -> allowSpiceLevel = v }, "Pilih tingkat kepedasan")),
                        Triple("🥫", "Pilihan Sambal", Triple(allowSambal, { v: Boolean -> allowSambal = v }, "Jenis sambal yang diinginkan")),
                        Triple("🍽️", "Pelengkap & Alat Makan", Triple(allowComplimentary, { v: Boolean -> allowComplimentary = v }, "Sarung tangan, sendok, tisu")),
                        Triple("➕", "Ekstra Topping & Kemasan", Triple(allowToppings, { v: Boolean -> allowToppings = v }, "Topping berbayar & kotak"))
                    ).forEach { (emoji, label, stateTriple) ->
                        val (checked, onCheck, desc) = stateTriple
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppShapes.Sm)
                                .background(if (checked) PrimarySoft else SurfaceTint)
                                .border(1.dp, if (checked) Primary else OutlineSoft, AppShapes.Sm)
                                .clickable { onCheck(!checked) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = emoji, fontSize = 14.sp)
                                Column {
                                    Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = if (checked) Primary else TextPrimary))
                                    Text(text = desc, style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                                }
                            }
                            Switch(
                                checked = checked,
                                onCheckedChange = onCheck,
                                colors = SwitchDefaults.colors(checkedThumbColor = SurfaceWhite, checkedTrackColor = Primary)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Status Produk Aktif", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Switch(checked = isActive, onCheckedChange = { isActive = it }, colors = SwitchDefaults.colors(checkedThumbColor = SurfaceWhite, checkedTrackColor = Primary))
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage!!, style = MaterialTheme.typography.bodySmall.copy(color = Danger))
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(text = "Batal", onClick = onDismiss, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "Simpan",
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Nama produk wajib diisi!"
                            } else if (price <= 0) {
                                errorMessage = "Harga jual harus lebih dari 0!"
                            } else {
                                onSave(name, selectedCategoryId, price, costPrice, emoji, spiceLevel, description, isActive, allowSpiceLevel, allowSambal, allowComplimentary, allowToppings)
                            }
                        },
                        modifier = Modifier.weight(1.3f)
                    )
                }

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategoryDialog(
    category: Category?,
    onSave: (name: String, emoji: String, color: String, sortOrder: Int, isActive: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var emoji by remember { mutableStateOf(category?.emoji ?: "🍽️") }
    var color by remember { mutableStateOf(category?.color ?: "#0B5C73") }
    var sortOrderStr by remember { mutableStateOf(category?.sortOrder?.toString() ?: "1") }
    var isActive by remember { mutableStateOf(category?.isActive ?: true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val emojiOptions = listOf("🍽️", "🍢", "🍗", "🥤", "🍚", "🥟", "🌶️", "🍰", "🍟", "☕")
    val colorOptions = listOf("#0B5C73", "#E8A33D", "#3C7A8C", "#2E9E6B", "#DC3545", "#6F42C1", "#FD7E14")

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.Lg)
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, AppShapes.Lg)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (category == null) "Tambah Kategori Baru" else "Edit Kategori",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Nama Kategori *") },
                    placeholder = { Text("Contoh: Sate Taichan") },
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Emoji Picker
                Text(text = "Ikon Emoji Kategori", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    emojiOptions.forEach { em ->
                        val isSelected = emoji == em
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimarySoft else SurfaceTint)
                                .border(1.dp, if (isSelected) Primary else OutlineSoft, CircleShape)
                                .clickable { emoji = em },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = em, fontSize = 18.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Color Palette Picker
                Text(text = "Warna Tema Kategori", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorOptions.forEach { hex ->
                        val isSelected = color.equals(hex, ignoreCase = true)
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (_: Exception) {
                            Primary
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(if (isSelected) 3.dp else 1.dp, if (isSelected) TextPrimary else OutlineSoft, CircleShape)
                                .clickable { color = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sort Order
                OutlinedTextField(
                    value = sortOrderStr,
                    onValueChange = { sortOrderStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Urutan Tampilan (1, 2, 3...)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Status Kategori Aktif", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Switch(checked = isActive, onCheckedChange = { isActive = it }, colors = SwitchDefaults.colors(checkedThumbColor = SurfaceWhite, checkedTrackColor = Primary))
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage!!, style = MaterialTheme.typography.bodySmall.copy(color = Danger))
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(text = "Batal", onClick = onDismiss, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "Simpan",
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Nama kategori wajib diisi!"
                            } else {
                                onSave(name, emoji, color, sortOrderStr.toIntOrNull() ?: 1, isActive)
                            }
                        },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.Lg)
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, AppShapes.Lg)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFDE4E5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Danger, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(text = message, style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(text = "Batal", onClick = onDismiss, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(AppShapes.Sm)
                            .background(Danger)
                            .clickable(onClick = onConfirm)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Hapus", style = MaterialTheme.typography.labelMedium.copy(color = SurfaceWhite, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun ModifierItemCard(
    modifier: ModifierOption,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = when (modifier.category) {
        ModifierCategory.SPICE_LEVEL -> Color(0xFFE53935)
        ModifierCategory.SAMBAL -> Color(0xFFFF7043)
        ModifierCategory.COMPLIMENTARY -> Color(0xFF43A047)
        ModifierCategory.EXTRA -> Color(0xFF1E88E5)
        ModifierCategory.PACKAGING -> Color(0xFF8E24AA)
    }
    val categoryEmoji = when (modifier.category) {
        ModifierCategory.SPICE_LEVEL -> "🌶️"
        ModifierCategory.SAMBAL -> "🥫"
        ModifierCategory.COMPLIMENTARY -> "🍽️"
        ModifierCategory.EXTRA -> "➕"
        ModifierCategory.PACKAGING -> "📦"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.Md)
            .background(SurfaceWhite)
            .border(1.dp, OutlineSoft, AppShapes.Md)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(AppShapes.Sm)
                        .background(categoryColor.copy(alpha = 0.12f))
                        .border(1.dp, categoryColor.copy(alpha = 0.3f), AppShapes.Sm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = categoryEmoji, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = modifier.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (modifier.isActive) TextPrimary else TextSecondary
                        )
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(categoryColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = modifier.category.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(color = categoryColor, fontWeight = FontWeight.Bold)
                            )
                        }
                        if (modifier.price > 0) {
                            Text(
                                text = "+${CurrencyFormatter.formatRupiah(modifier.price)}",
                                style = MaterialTheme.typography.labelSmall.copy(color = Success, fontWeight = FontWeight.Bold)
                            )
                        } else {
                            Text(
                                text = "Gratis",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                        if (modifier.isDefault) {
                            Text(
                                text = "• Default",
                                style = MaterialTheme.typography.labelSmall.copy(color = Primary, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = modifier.isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SurfaceWhite,
                        checkedTrackColor = Primary
                    )
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Danger, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditModifierDialog(
    modifier: ModifierOption?,
    onSave: (name: String, price: Double, category: ModifierCategory, isDefault: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(modifier?.name ?: "") }
    var priceStr by remember { mutableStateOf(if (modifier != null && modifier.price > 0) modifier.price.toLong().toString() else "") }
    var selectedCategory by remember { mutableStateOf(modifier?.category ?: ModifierCategory.SPICE_LEVEL) }
    var isDefault by remember { mutableStateOf(modifier?.isDefault ?: false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categoryMeta = listOf(
        Triple(ModifierCategory.SPICE_LEVEL, "🌶️", "Tingkat kepedasan menu"),
        Triple(ModifierCategory.SAMBAL, "🥫", "Pilihan jenis sambal"),
        Triple(ModifierCategory.COMPLIMENTARY, "🍽️", "Alat makan & pelengkap gratis"),
        Triple(ModifierCategory.EXTRA, "➕", "Topping & tambahan berbayar"),
        Triple(ModifierCategory.PACKAGING, "📦", "Pilihan kemasan & wadah")
    )
    val categoryColors = mapOf(
        ModifierCategory.SPICE_LEVEL to Color(0xFFE53935),
        ModifierCategory.SAMBAL to Color(0xFFFF7043),
        ModifierCategory.COMPLIMENTARY to Color(0xFF43A047),
        ModifierCategory.EXTRA to Color(0xFF1E88E5),
        ModifierCategory.PACKAGING to Color(0xFF8E24AA)
    )

    BasicAlertDialog(onDismissRequest = onDismiss) {
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.Lg)
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, AppShapes.Lg)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (modifier == null) "Tambah Varian / Modifier" else "Edit Varian",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nama
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Nama Varian *") },
                    placeholder = { Text("Contoh: Lv. 3 (Ekstra Pedas)") },
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Harga Tambahan
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Harga Tambahan (Rp, kosongkan jika gratis)") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Kelompok Varian
                Text(
                    text = "Kelompok Varian",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categoryMeta.forEach { (cat, emoji, desc) ->
                        val color = categoryColors[cat] ?: Primary
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppShapes.Sm)
                                .background(if (isSelected) color.copy(alpha = 0.10f) else SurfaceTint)
                                .border(1.5.dp, if (isSelected) color else OutlineSoft, AppShapes.Sm)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = emoji, fontSize = 16.sp)
                                Column {
                                    Text(
                                        text = cat.displayName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) color else TextPrimary
                                        )
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Default toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Jadikan Pilihan Default", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(text = "Dipilih otomatis saat order", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    }
                    Switch(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = SurfaceWhite, checkedTrackColor = Primary)
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage!!, style = MaterialTheme.typography.bodySmall.copy(color = Danger))
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(text = "Batal", onClick = onDismiss, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "Simpan",
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Nama varian wajib diisi!"
                            } else {
                                onSave(name, priceStr.toDoubleOrNull() ?: 0.0, selectedCategory, isDefault)
                            }
                        },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

