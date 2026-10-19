package com.kapos.mobile.feature.stock

import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.Product
import com.kapos.mobile.domain.model.RawMaterial
import com.kapos.mobile.domain.model.RawMaterialCategory
import com.kapos.mobile.domain.model.RecipeItem
import com.kapos.mobile.domain.model.StockMovementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BomRecipeAndInventoryTest {

    private val sampleMaterials = listOf(
        RawMaterial(
            id = "mat_cabe",
            name = "Cabe Rawit Merah",
            category = RawMaterialCategory.BUMBU_SAMBAL,
            unit = "gram",
            currentStock = 1200.0,
            minStock = 500.0,
            costPerUnit = 45.0
        ),
        RawMaterial(
            id = "mat_bawang",
            name = "Bawang Putih Kating",
            category = RawMaterialCategory.BUMBU_SAMBAL,
            unit = "gram",
            currentStock = 300.0,
            minStock = 400.0, // Low stock!
            costPerUnit = 38.0
        ),
        RawMaterial(
            id = "mat_ayam_fillet",
            name = "Daging Ayam Fillet",
            category = RawMaterialCategory.PROTEIN,
            unit = "gram",
            currentStock = 5000.0,
            minStock = 2000.0,
            costPerUnit = 55.0
        ),
        RawMaterial(
            id = "mat_tusuk",
            name = "Tusuk Sate Bambu",
            category = RawMaterialCategory.PACKAGING,
            unit = "pcs",
            currentStock = 800.0,
            minStock = 300.0,
            costPerUnit = 30.0
        )
    )

    @Test
    fun testRawMaterialLowStockFlag() {
        val normalStock = sampleMaterials.first { it.id == "mat_cabe" }
        assertFalse(normalStock.isLowStock)

        val lowStock = sampleMaterials.first { it.id == "mat_bawang" }
        assertTrue(lowStock.isLowStock)

        val zeroStock = normalStock.copy(currentStock = 0.0)
        assertTrue(zeroStock.isLowStock)
    }

    @Test
    fun testStockUiStateFiltering() {
        val state = StockUiState(
            rawMaterials = sampleMaterials,
            selectedMaterialCategory = RawMaterialCategory.BUMBU_SAMBAL
        )

        assertEquals(2, state.filteredMaterials.size)
        assertTrue(state.filteredMaterials.all { it.category == RawMaterialCategory.BUMBU_SAMBAL })
    }

    @Test
    fun testStockUiStateSearch() {
        val state = StockUiState(
            rawMaterials = sampleMaterials,
            materialSearchQuery = "fillet"
        )

        assertEquals(1, state.filteredMaterials.size)
        assertEquals("Daging Ayam Fillet", state.filteredMaterials.first().name)
    }

    @Test
    fun testStockUiStateLowStockDetection() {
        val state = StockUiState(rawMaterials = sampleMaterials)
        assertEquals(1, state.lowStockMaterials.size)
        assertEquals("Bawang Putih Kating", state.lowStockMaterials.first().name)
    }

    @Test
    fun testBomRecipeDeductionFormula() {
        // Recipe: 1 Porsi Sate Taichan = 150g Ayam Fillet, 10 pcs Tusuk, 25g Cabe
        val recipes = listOf(
            RecipeItem(id = "r1", productId = "prod_taichan", materialId = "mat_ayam_fillet", materialName = "Daging Ayam Fillet", unit = "gram", quantityRequired = 150.0),
            RecipeItem(id = "r2", productId = "prod_taichan", materialId = "mat_tusuk", materialName = "Tusuk Sate Bambu", unit = "pcs", quantityRequired = 10.0),
            RecipeItem(id = "r3", productId = "prod_taichan", materialId = "mat_cabe", materialName = "Cabe Rawit Merah", unit = "gram", quantityRequired = 25.0)
        )

        val product = Product(id = "prod_taichan", categoryId = "cat_taichan", name = "Sate Taichan Daging", price = 25000.0, costPrice = 14000.0)
        val cartItems = listOf(
            CartItem(product = product, quantity = 3) // Sold 3 portions
        )

        // Calculate deductions
        val deductions = mutableMapOf<String, Double>()
        for (cart in cartItems) {
            val matchingRecipes = recipes.filter { it.productId == cart.product.id }
            for (r in matchingRecipes) {
                deductions[r.materialId] = (deductions[r.materialId] ?: 0.0) + (r.quantityRequired * cart.quantity)
            }
        }

        // 3 portions * 150g = 450g Ayam
        assertEquals(450.0, deductions["mat_ayam_fillet"] ?: 0.0, 0.001)
        // 3 portions * 10 pcs = 30 pcs Tusuk
        assertEquals(30.0, deductions["mat_tusuk"] ?: 0.0, 0.001)
        // 3 portions * 25g = 75g Cabe
        assertEquals(75.0, deductions["mat_cabe"] ?: 0.0, 0.001)
    }

    @Test
    fun testStockMovementTypes() {
        assertEquals("RESTOCK", StockMovementType.RESTOCK.name)
        assertEquals("ORDER_DEDUCTION", StockMovementType.ORDER_DEDUCTION.name)
        assertEquals("WASTE", StockMovementType.WASTE.name)
        assertEquals("ADJUSTMENT", StockMovementType.ADJUSTMENT.name)
    }
}
