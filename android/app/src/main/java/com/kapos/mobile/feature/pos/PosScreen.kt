package com.kapos.mobile.feature.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapos.mobile.core.components.AppTopBar
import com.kapos.mobile.core.components.CategoryChips
import com.kapos.mobile.core.components.SearchField
import com.kapos.mobile.core.theme.OutlineSoft
import com.kapos.mobile.core.theme.Primary
import com.kapos.mobile.core.theme.ScreenBackground
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.core.utils.CurrencyFormatter
import com.kapos.mobile.feature.cart.CartBottomSheet

@Composable
fun PosRoute(
    viewModel: PosViewModel,
    onNavigateToCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    PosScreen(
        uiState = uiState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onSelectCategory = viewModel::onSelectCategory,
        onProductClick = viewModel::onProductClick,
        onOpenCartSheet = { viewModel.setCartSheetOpen(true) },
        onCloseCartSheet = { viewModel.setCartSheetOpen(false) },
        onQuantityChange = viewModel::updateCartItemQuantity,
        onRemoveCartItem = viewModel::removeCartItem,
        onClearCart = viewModel::clearCart,
        onSyncClick = viewModel::refreshData,
        onCheckoutClick = {
            viewModel.setCartSheetOpen(false)
            onNavigateToCheckout()
        },
        modifier = modifier
    )
}

@Composable
fun PosScreen(
    uiState: PosUiState,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onProductClick: (com.kapos.mobile.domain.model.Product) -> Unit,
    onOpenCartSheet: () -> Unit,
    onCloseCartSheet: () -> Unit,
    onQuantityChange: (Int, Int) -> Unit,
    onRemoveCartItem: (Int) -> Unit,
    onClearCart: () -> Unit,
    onSyncClick: () -> Unit,
    onCheckoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = "KA POS",
                subtitle = "Taichan & Chicken KA",
                isOnline = uiState.isOnline,
                isSyncing = uiState.isSyncing,
                onSyncClick = onSyncClick
            )

            // Search Field
            SearchField(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = "Cari sate taichan, ayam, minuman...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Chips
            CategoryChips(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onCategorySelect = onSelectCategory
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Product Grid (2 columns)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = if (uiState.cartTotalCount > 0) 140.dp else 90.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.filteredProducts, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onAddClick = { onProductClick(product) }
                    )
                }
            }
        }

        // Floating Cart Summary Bar
        AnimatedVisibility(
            visible = uiState.cartTotalCount > 0,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
        ) {
            FloatingCartBar(
                itemCount = uiState.cartTotalCount,
                subtotal = uiState.cartSubtotal,
                onViewCartClick = onOpenCartSheet
            )
        }

        // Cart Bottom Sheet
        if (uiState.isCartSheetOpen) {
            CartBottomSheet(
                cartItems = uiState.cartItems,
                subtotal = uiState.cartSubtotal,
                onQuantityChange = onQuantityChange,
                onRemoveItem = onRemoveCartItem,
                onClearCart = onClearCart,
                onCheckoutClick = onCheckoutClick,
                onDismiss = onCloseCartSheet
            )
        }
    }
}

@Composable
fun FloatingCartBar(
    itemCount: Int,
    subtotal: Double,
    onViewCartClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(elevation = 16.dp, shape = CircleShape, spotColor = Primary.copy(alpha = 0.2f))
            .clip(CircleShape)
            .background(SurfaceWhite)
            .border(1.dp, OutlineSoft, CircleShape)
            .clickable(onClick = onViewCartClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = "Cart",
                        tint = SurfaceWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "$itemCount Items",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = CurrencyFormatter.formatRupiah(subtotal),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Primary)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Lihat",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = SurfaceWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = SurfaceWhite,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
