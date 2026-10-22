package com.kapos.mobile.domain.model

import java.io.Serializable

enum class ModifierCategory(val displayName: String) {
    SPICE_LEVEL("Level Pedas"),
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
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
) : Serializable {
    val isFree: Boolean get() = price <= 0.0
}

object PresetModifiers {
    val allOptions: List<ModifierOption> = listOf(
        // Level Pedas
        ModifierOption("spc_lv0", "Original (Tidak Pedas)", 0.0, ModifierCategory.SPICE_LEVEL, isDefault = true, sortOrder = 1),
        ModifierOption("spc_lv1", "Lv. 1 (Sedang)", 0.0, ModifierCategory.SPICE_LEVEL, sortOrder = 2),
        ModifierOption("spc_lv2", "Lv. 2 (Pedas)", 0.0, ModifierCategory.SPICE_LEVEL, sortOrder = 3),
        ModifierOption("spc_lv3", "Lv. 3 (Ekstra Pedas)", 0.0, ModifierCategory.SPICE_LEVEL, sortOrder = 4),
        ModifierOption("spc_lv4", "Lv. 4 (Super Pedas)", 0.0, ModifierCategory.SPICE_LEVEL, sortOrder = 5),
        ModifierOption("spc_lv5", "Lv. 5 (Maksimal)", 0.0, ModifierCategory.SPICE_LEVEL, sortOrder = 6),

        // Sambal
        ModifierOption("smb_taichan", "Sambal Taichan Ori", 0.0, ModifierCategory.SAMBAL, isDefault = true, sortOrder = 10),
        ModifierOption("smb_bawang", "Sambal Bawang", 0.0, ModifierCategory.SAMBAL, sortOrder = 11),
        ModifierOption("smb_matah", "Sambal Matah", 0.0, ModifierCategory.SAMBAL, sortOrder = 12),
        ModifierOption("smb_ijo", "Sambal Ijo", 0.0, ModifierCategory.SAMBAL, sortOrder = 13),
        ModifierOption("smb_extra", "Ekstra Sambal (+3rb)", 3000.0, ModifierCategory.EXTRA, sortOrder = 14),
        
        // Complimentary
        ModifierOption("comp_glove", "Sarung Tangan Plastik", 0.0, ModifierCategory.COMPLIMENTARY, sortOrder = 20),
        ModifierOption("comp_straw", "Sedotan Minum", 0.0, ModifierCategory.COMPLIMENTARY, sortOrder = 21),
        ModifierOption("comp_cutlery", "Sendok & Garpu", 0.0, ModifierCategory.COMPLIMENTARY, sortOrder = 22),
        ModifierOption("comp_tissue", "Tisu Tambahan", 0.0, ModifierCategory.COMPLIMENTARY, sortOrder = 23),
        
        // Extra Toppings
        ModifierOption("ext_lime", "Jeruk Nipis (+2rb)", 2000.0, ModifierCategory.EXTRA, sortOrder = 30),
        ModifierOption("ext_onions", "Bawang Goreng (+2.5rb)", 2500.0, ModifierCategory.EXTRA, sortOrder = 31),
        ModifierOption("ext_mozzarella", "Keju Melt (+5rb)", 5000.0, ModifierCategory.EXTRA, sortOrder = 32),
        
        // Packaging
        ModifierOption("pkg_box", "Kotak Takeaway (+1rb)", 1000.0, ModifierCategory.PACKAGING, sortOrder = 40),
        ModifierOption("pkg_bag", "Paper Bag (+2rb)", 2000.0, ModifierCategory.PACKAGING, sortOrder = 41)
    )
}
