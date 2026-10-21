package com.kapos.mobile.feature.pos

import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.ModifierCategory
import com.kapos.mobile.domain.model.ModifierOption
import com.kapos.mobile.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartModifierTest {

    private val testProduct = Product(
        id = "p1",
        categoryId = "cat_taichan",
        name = "Sate Taichan Daging (10 Tsk)",
        price = 25000.0,
        costPrice = 14000.0
    )

    @Test
    fun testCartItemWithoutModifiers_calculatesStandardSubtotal() {
        val item = CartItem(
            product = testProduct,
            quantity = 2,
            spiceLevel = "Lv.2"
        )
        assertEquals(25000.0, item.unitPrice, 0.001)
        assertEquals(50000.0, item.subtotal, 0.001)
        assertTrue(item.selectedModifiers.isEmpty())
    }

    @Test
    fun testCartItemWithFreeAndPaidModifiers_calculatesCorrectSubtotal() {
        val modifiers = listOf(
            ModifierOption("m1", "Sambal Matah", price = 0.0, category = ModifierCategory.SAMBAL),
            ModifierOption("m2", "Sarung Tangan Plastik", price = 0.0, category = ModifierCategory.COMPLIMENTARY),
            ModifierOption("m3", "Ekstra Sambal Cup", price = 3000.0, category = ModifierCategory.EXTRA),
            ModifierOption("m4", "Kemasan Box", price = 2000.0, category = ModifierCategory.PACKAGING)
        )

        val item = CartItem(
            product = testProduct, // Rp 25.000
            quantity = 3,
            spiceLevel = "Lv.3",
            selectedModifiers = modifiers, // + Rp 5.000 total ekstra
            notes = "Sambal dipisah"
        )

        // Unit Price: 25.000 + 3.000 + 2.000 = 30.000
        assertEquals(30000.0, item.unitPrice, 0.001)
        // Subtotal: 30.000 * 3 = 90.000
        assertEquals(90000.0, item.subtotal, 0.001)
        assertEquals(4, item.selectedModifiers.size)
    }
}
