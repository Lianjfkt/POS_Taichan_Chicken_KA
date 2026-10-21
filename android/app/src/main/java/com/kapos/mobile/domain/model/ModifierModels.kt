package com.kapos.mobile.domain.model

import java.io.Serializable

enum class ModifierCategory(val displayName: String) {
    SAMBAL("Pilihan Sambal"),
    COMPLIMENTARY("Pelengkap & Alat Makan"),
    EXTRA("Ekstra & Topping"),
    PACKAGING("Kemasan")
}

data class ModifierOption(
    val id: String,
    val name: String,
    val price: Double = 0.0,
    val category: ModifierCategory = ModifierCategory.SAMBAL,
    val isDefault: Boolean = false
) : Serializable {
    val isFree: Boolean get() = price <= 0.0
}

object PresetModifiers {
    val allOptions: List<ModifierOption> = listOf(
        // Sambal
        ModifierOption("smb_taichan", "Sambal Taichan Ori", 0.0, ModifierCategory.SAMBAL, isDefault = true),
        ModifierOption("smb_bawang", "Sambal Bawang", 0.0, ModifierCategory.SAMBAL),
        ModifierOption("smb_matah", "Sambal Matah", 0.0, ModifierCategory.SAMBAL),
        ModifierOption("smb_ijo", "Sambal Ijo", 0.0, ModifierCategory.SAMBAL),
        ModifierOption("smb_extra", "Ekstra Sambal (+3rb)", 3000.0, ModifierCategory.EXTRA),
        
        // Complimentary
        ModifierOption("comp_glove", "Sarung Tangan Plastik", 0.0, ModifierCategory.COMPLIMENTARY),
        ModifierOption("comp_straw", "Sedotan Minum", 0.0, ModifierCategory.COMPLIMENTARY),
        ModifierOption("comp_cutlery", "Sendok & Garpu", 0.0, ModifierCategory.COMPLIMENTARY),
        ModifierOption("comp_tissue", "Tisu Tambahan", 0.0, ModifierCategory.COMPLIMENTARY),
        
        // Extra Toppings
        ModifierOption("ext_lime", "Jeruk Nipis (+2rb)", 2000.0, ModifierCategory.EXTRA),
        ModifierOption("ext_onions", "Bawang Goreng (+2.5rb)", 2500.0, ModifierCategory.EXTRA),
        ModifierOption("ext_mozzarella", "Keju Melt (+5rb)", 5000.0, ModifierCategory.EXTRA),
        
        // Packaging
        ModifierOption("pkg_box", "Kotak Takeaway (+1rb)", 1000.0, ModifierCategory.PACKAGING),
        ModifierOption("pkg_bag", "Paper Bag (+2rb)", 2000.0, ModifierCategory.PACKAGING)
    )
}
