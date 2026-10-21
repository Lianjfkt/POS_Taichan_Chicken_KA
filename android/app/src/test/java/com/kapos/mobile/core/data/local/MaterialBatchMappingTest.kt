package com.kapos.mobile.core.data.local

import com.kapos.mobile.core.data.local.entity.MaterialBatchEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class MaterialBatchMappingTest {

    @Test
    fun testMaterialBatchEntityFields() {
        val entity = MaterialBatchEntity(
            id = "batch-1",
            materialId = "mat-cabe",
            batchNumber = "B-20261021-001",
            initialQuantity = 5000.0,
            remainingQuantity = 4500.0,
            costTotal = 225000.0,
            costPerUnit = 45.0,
            receivedDate = 1700000000L,
            expiryDate = 1700500000L,
            isUsable = true,
            notes = "Pasar Induk"
        )
        assertEquals("batch-1", entity.id)
        assertEquals(45.0, entity.costPerUnit, 0.001)
        assertEquals(4500.0, entity.remainingQuantity, 0.001)
    }
}
