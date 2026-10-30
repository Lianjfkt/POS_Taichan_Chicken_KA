package com.kapos.mobile.feature.menu

import com.kapos.mobile.domain.model.ModifierCategory
import com.kapos.mobile.domain.model.ModifierOption
import org.junit.Assert.assertEquals
import org.junit.Test

class MenuManagementViewModelModifierTest {

    @Test
    fun testFilteredModifiersByCategoryAndSearch() {
        val sampleModifiers = listOf(
            ModifierOption(id = "m1", name = "Sambal Matah", price = 0.0, category = ModifierCategory.SAMBAL, isActive = true),
            ModifierOption(id = "m2", name = "Sambal Bawang", price = 0.0, category = ModifierCategory.SAMBAL, isActive = true),
            ModifierOption(id = "m3", name = "Lv. 1", price = 0.0, category = ModifierCategory.SPICE_LEVEL, isActive = true),
            ModifierOption(id = "m4", name = "Keju Melt", price = 5000.0, category = ModifierCategory.EXTRA, isActive = true)
        )

        val stateCategoryFilter = MenuManagementUiState(
            modifiers = sampleModifiers,
            selectedModifierCategoryFilter = ModifierCategory.SAMBAL
        )
        assertEquals(2, stateCategoryFilter.filteredModifiers.size)

        val stateSearchFilter = MenuManagementUiState(
            modifiers = sampleModifiers,
            searchQuery = "Keju"
        )
        assertEquals(1, stateSearchFilter.filteredModifiers.size)
        assertEquals("Keju Melt", stateSearchFilter.filteredModifiers[0].name)
    }

    @Test
    fun testActiveModifierCount() {
        val sampleModifiers = listOf(
            ModifierOption(id = "m1", name = "Sambal Matah", price = 0.0, category = ModifierCategory.SAMBAL, isActive = true),
            ModifierOption(id = "m2", name = "Sambal Bawang", price = 0.0, category = ModifierCategory.SAMBAL, isActive = false)
        )
        val state = MenuManagementUiState(modifiers = sampleModifiers)
        assertEquals(1, state.activeModifierCount)
    }
}
