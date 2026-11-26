package com.kapos.mobile.core.data.local

import com.kapos.mobile.domain.model.Product
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModifierModelTest {
    @Test
    fun testProductHasModifiersHelper() {
        val drink = Product(
            id = "p1",
            categoryId = "cat_drinks",
            name = "Es Teh",
            price = 5000.0,
            allowSpiceLevel = false,
            allowSambal = false,
            allowComplimentary = false,
            allowToppings = false
        )
        assertFalse(drink.hasModifiers)

        val food = Product(
            id = "p2",
            categoryId = "cat_taichan",
            name = "Sate",
            price = 25000.0,
            allowSpiceLevel = true,
            allowSambal = true,
            allowComplimentary = false,
            allowToppings = true
        )
        assertTrue(food.hasModifiers)
    }
}
