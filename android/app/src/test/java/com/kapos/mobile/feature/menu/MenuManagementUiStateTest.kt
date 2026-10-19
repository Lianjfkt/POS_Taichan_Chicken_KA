package com.kapos.mobile.feature.menu

import com.kapos.mobile.domain.model.Category
import com.kapos.mobile.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Test

class MenuManagementUiStateTest {

    private val sampleCategories = listOf(
        Category(id = "cat_taichan", name = "Sate Taichan", isActive = true),
        Category(id = "cat_drinks", name = "Minuman", isActive = true),
        Category(id = "cat_old", name = "Menu Lama", isActive = false)
    )

    private val sampleProducts = listOf(
        Product(
            id = "p1",
            categoryId = "cat_taichan",
            name = "Sate Taichan Daging",
            price = 25000.0,
            costPrice = 14000.0,
            isActive = true
        ),
        Product(
            id = "p2",
            categoryId = "cat_taichan",
            name = "Sate Taichan Kulit",
            price = 22000.0,
            costPrice = 11000.0,
            isActive = true
        ),
        Product(
            id = "p3",
            categoryId = "cat_drinks",
            name = "Es Teh Manis",
            price = 5000.0,
            costPrice = 1500.0,
            isActive = true
        ),
        Product(
            id = "p4",
            categoryId = "cat_drinks",
            name = "Es Jeruk Murni",
            price = 8000.0,
            costPrice = 3500.0,
            isActive = false
        )
    )

    @Test
    fun testActiveCounts() {
        val state = MenuManagementUiState(
            products = sampleProducts,
            categories = sampleCategories
        )

        assertEquals(3, state.activeProductCount)
        assertEquals(2, state.activeCategoryCount)
    }

    @Test
    fun testFilterByCategory() {
        val state = MenuManagementUiState(
            products = sampleProducts,
            categories = sampleCategories,
            selectedCategoryId = "cat_drinks"
        )

        assertEquals(2, state.filteredProducts.size)
        assertEquals(listOf("Es Teh Manis", "Es Jeruk Murni"), state.filteredProducts.map { it.name })
    }

    @Test
    fun testFilterBySearchQuery() {
        val state = MenuManagementUiState(
            products = sampleProducts,
            categories = sampleCategories,
            searchQuery = "kulit"
        )

        assertEquals(1, state.filteredProducts.size)
        assertEquals("Sate Taichan Kulit", state.filteredProducts.first().name)
    }

    @Test
    fun testProfitMarginCalculation() {
        val product = sampleProducts.first() // price 25.000, costPrice 14.000
        val profit = product.price - product.costPrice // 11.000
        val marginPercent = ((profit / product.price) * 100).toInt() // 44%

        assertEquals(11000.0, profit, 0.001)
        assertEquals(44, marginPercent)
    }
}
