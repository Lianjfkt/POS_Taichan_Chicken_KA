package com.kapos.mobile.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kapos.mobile.domain.model.ModifierCategory
import com.kapos.mobile.domain.model.ModifierOption

@Entity(tableName = "modifiers")
data class ModifierEntity(
    @PrimaryKey val id: String,
    val name: String,
    val price: Double = 0.0,
    val category: String, // SPICE_LEVEL, SAMBAL, COMPLIMENTARY, EXTRA, PACKAGING
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
) {
    fun toDomain(): ModifierOption {
        val cat = try {
            ModifierCategory.valueOf(category)
        } catch (_: Exception) {
            ModifierCategory.SAMBAL
        }
        return ModifierOption(
            id = id,
            name = name,
            price = price,
            category = cat,
            isDefault = isDefault,
            isActive = isActive,
            sortOrder = sortOrder
        )
    }

    companion object {
        fun fromDomain(option: ModifierOption): ModifierEntity {
            return ModifierEntity(
                id = option.id,
                name = option.name,
                price = option.price,
                category = option.category.name,
                isDefault = option.isDefault,
                isActive = option.isActive,
                sortOrder = option.sortOrder
            )
        }
    }
}
