package com.kapos.mobile.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kapos.mobile.domain.model.MaterialBatch

@Entity(
    tableName = "material_batches",
    foreignKeys = [
        ForeignKey(
            entity = RawMaterialEntity::class,
            parentColumns = ["id"],
            childColumns = ["materialId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("materialId"), Index("expiryDate")]
)
data class MaterialBatchEntity(
    @PrimaryKey val id: String,
    val materialId: String,
    val batchNumber: String,
    val initialQuantity: Double,
    val remainingQuantity: Double,
    val costTotal: Double,
    val costPerUnit: Double,
    val receivedDate: Long = System.currentTimeMillis(),
    val expiryDate: Long? = null,
    val isUsable: Boolean = true,
    val notes: String = ""
) {
    fun toDomain(): MaterialBatch = MaterialBatch(
        id = id,
        materialId = materialId,
        batchNumber = batchNumber,
        initialQuantity = initialQuantity,
        remainingQuantity = remainingQuantity,
        costTotal = costTotal,
        costPerUnit = costPerUnit,
        receivedDate = receivedDate,
        expiryDate = expiryDate,
        isUsable = isUsable,
        notes = notes
    )
}
