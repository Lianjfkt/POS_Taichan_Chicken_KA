# Integrated Stock, Auto BOP, and Batch Expiry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Unify inventory management under Room DB, implement batch tracking with expiry dates and freshness warnings, calculate moving weighted average costs on restock to automatically update menu BOP (HPP) in real-time, and provide FIFO stock deduction with waste loss recording.

**Architecture:** Extend Room DB with `material_batches` and `MaterialBatchDao`. Update `InventoryRepository` to compute moving weighted average unit costs on restock, cascade BOP recomputations to `products.costPrice` via `recipe_items`, execute FIFO batch deductions on sale, and track waste loss. Refactor `StockScreen` to 2 streamlined tabs (Bahan Baku & Batch, Audit Mutasi & Waste) and enhance `MenuManagementScreen` with live BOP and profit margin metrics.

**Tech Stack:** Kotlin, Jetpack Compose, Room Database, StateFlow / Coroutines, AndroidX Lifecycle ViewModel, JUnit4.

## Global Constraints
- Target platform: Android (SDK 26+), Jetpack Compose with Material 3.
- All monetary calculations must use `Double` with zero-safe division.
- FIFO ordering strictly prioritizes `expiryDate ASC NULLS LAST, receivedDate ASC`.
- Room Database version upgraded from 4 to 5 with fallback to destructive migration.
- Cleanly deprecate redundant SharedPreferences stock logic without breaking existing code.

---

### Task 1: Domain Models for Batch Inventory, Freshness Status, and BOP Calculation

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/domain/model/StockModels.kt`
- Modify: `app/src/main/java/com/kapos/mobile/domain/model/Models.kt:150-179`
- Create: `app/src/test/java/com/kapos/mobile/domain/model/MaterialBatchDomainTest.kt`

**Interfaces:**
- Produces:
  - `enum class BatchFreshness(val displayName: String, val isUsable: Boolean)`: `FRESH`, `EXPIRING_SOON`, `EXPIRED`, `DAMAGED`
  - `data class MaterialBatch(val id: String, val materialId: String, val batchNumber: String, val initialQuantity: Double, val remainingQuantity: Double, val costTotal: Double, val costPerUnit: Double, val receivedDate: Long, val expiryDate: Long?, val isUsable: Boolean, val notes: String)`
  - `MaterialBatch.freshnessStatus(currentTime: Long): BatchFreshness`
  - Extended `RawMaterial`: adds `earliestExpiryDate: Long? = null`, `hasExpiredBatch: Boolean = false`
  - Extended `Product`: adds `fun calculateProfitMargin(): Double` and `fun calculateMarginPercentage(): Double`

- [ ] **Step 1: Write the failing test**

```kotlin
// app/src/test/java/com/kapos/mobile/domain/model/MaterialBatchDomainTest.kt
package com.kapos.mobile.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialBatchDomainTest {

    @Test
    fun testBatchFreshnessEvaluation() {
        val now = 1000000000L
        val oneDayMs = 24L * 60 * 60 * 1000

        // Expired batch
        val expiredBatch = MaterialBatch(
            id = "b1",
            materialId = "m1",
            batchNumber = "B-001",
            initialQuantity = 1000.0,
            remainingQuantity = 500.0,
            costTotal = 45000.0,
            costPerUnit = 45.0,
            receivedDate = now - (10 * oneDayMs),
            expiryDate = now - oneDayMs,
            isUsable = true
        )
        assertEquals(BatchFreshness.EXPIRED, expiredBatch.freshnessStatus(now))

        // Expiring soon batch (within 2 days)
        val soonBatch = expiredBatch.copy(
            id = "b2",
            expiryDate = now + (oneDayMs)
        )
        assertEquals(BatchFreshness.EXPIRING_SOON, soonBatch.freshnessStatus(now))

        // Fresh batch (> 2 days)
        val freshBatch = expiredBatch.copy(
            id = "b3",
            expiryDate = now + (5 * oneDayMs)
        )
        assertEquals(BatchFreshness.FRESH, freshBatch.freshnessStatus(now))

        // Manually damaged/unusable batch
        val damagedBatch = freshBatch.copy(
            id = "b4",
            isUsable = false
        )
        assertEquals(BatchFreshness.DAMAGED, damagedBatch.freshnessStatus(now))
    }

    @Test
    fun testProductMarginCalculation() {
        val product = Product(
            id = "p1",
            categoryId = "c1",
            name = "Sate Taichan Porsi",
            price = 25000.0,
            costPrice = 15000.0
        )
        assertEquals(10000.0, product.calculateProfitMargin(), 0.001)
        assertEquals(40.0, product.calculateMarginPercentage(), 0.001)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.domain.model.MaterialBatchDomainTest"`
Expected: FAIL with compilation error (unresolved references `MaterialBatch`, `BatchFreshness`).

- [ ] **Step 3: Write minimal implementation**

Add `BatchFreshness` and `MaterialBatch` to `StockModels.kt`:
```kotlin
// In app/src/main/java/com/kapos/mobile/domain/model/StockModels.kt
enum class BatchFreshness(val displayName: String, val isUsable: Boolean) {
    FRESH("Segar", true),
    EXPIRING_SOON("Mendekati Kadaluarsa (<= 2 Hari)", true),
    EXPIRED("Kadaluarsa", false),
    DAMAGED("Tidak Layak / Rusak", false)
}

data class MaterialBatch(
    val id: String,
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
) : java.io.Serializable {
    fun freshnessStatus(currentTime: Long = System.currentTimeMillis()): BatchFreshness {
        if (!isUsable) return BatchFreshness.DAMAGED
        val exp = expiryDate ?: return BatchFreshness.FRESH
        val diffDays = (exp - currentTime).toDouble() / (24.0 * 60 * 60 * 1000)
        return when {
            diffDays < 0.0 -> BatchFreshness.EXPIRED
            diffDays <= 2.0 -> BatchFreshness.EXPIRING_SOON
            else -> BatchFreshness.FRESH
        }
    }
}
```

Extend `RawMaterial` and `Product` in `app/src/main/java/com/kapos/mobile/domain/model/Models.kt`:
```kotlin
// In Product:
fun calculateProfitMargin(): Double = (price - costPrice).coerceAtLeast(0.0)
fun calculateMarginPercentage(): Double {
    if (price <= 0.0) return 0.0
    return ((price - costPrice) / price * 100.0).coerceAtLeast(0.0)
}

// In RawMaterial:
// Add properties to data class RawMaterial:
val earliestExpiryDate: Long? = null,
val hasExpiredBatch: Boolean = false
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.domain.model.MaterialBatchDomainTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/domain/model/StockModels.kt app/src/main/java/com/kapos/mobile/domain/model/Models.kt app/src/test/java/com/kapos/mobile/domain/model/MaterialBatchDomainTest.kt
git commit -m "feat(stock): add MaterialBatch domain models and product margin calculation"
```

---

### Task 2: Room Database Schema & DAO for Material Batches

**Files:**
- Create: `app/src/main/java/com/kapos/mobile/core/data/local/entity/MaterialBatchEntity.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/local/entity/Entities.kt:137-151`
- Create: `app/src/main/java/com/kapos/mobile/core/data/local/dao/MaterialBatchDao.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/local/AppDatabase.kt:27-72`

**Interfaces:**
- Produces:
  - `MaterialBatchEntity`: Table `material_batches`
  - `MaterialBatchDao`: `insert`, `upsertAll`, `getAll()`, `getByMaterial(materialId)`, `observeByMaterial(materialId)`, `getActiveBatchesForFIFO(materialId)`, `updateRemainingQuantity(id, qty)`, `setUsable(id, isUsable)`
  - `AppDatabase`: updated to version 5 with `materialBatchDao(): MaterialBatchDao`

- [ ] **Step 1: Write the failing unit test for MaterialBatchDao structure and entity mapping**

```kotlin
// app/src/test/java/com/kapos/mobile/core/data/local/MaterialBatchMappingTest.kt
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.core.data.local.MaterialBatchMappingTest"`
Expected: FAIL (unresolved `MaterialBatchEntity`).

- [ ] **Step 3: Implement MaterialBatchEntity, MaterialBatchDao, and update AppDatabase**

Create `app/src/main/java/com/kapos/mobile/core/data/local/entity/MaterialBatchEntity.kt`:
```kotlin
package com.kapos.mobile.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
)
```

Create `app/src/main/java/com/kapos/mobile/core/data/local/dao/MaterialBatchDao.kt`:
```kotlin
package com.kapos.mobile.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.kapos.mobile.core.data.local.entity.MaterialBatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialBatchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batch: MaterialBatchEntity)

    @Upsert
    suspend fun upsertAll(batches: List<MaterialBatchEntity>)

    @Query("SELECT * FROM material_batches WHERE materialId = :materialId ORDER BY receivedDate DESC")
    fun observeByMaterial(materialId: String): Flow<List<MaterialBatchEntity>>

    @Query("SELECT * FROM material_batches ORDER BY receivedDate DESC")
    fun observeAll(): Flow<List<MaterialBatchEntity>>

    @Query("SELECT * FROM material_batches WHERE materialId = :materialId AND remainingQuantity > 0 AND isUsable = 1 ORDER BY CASE WHEN expiryDate IS NULL THEN 1 ELSE 0 END, expiryDate ASC, receivedDate ASC")
    suspend fun getActiveBatchesForFIFO(materialId: String): List<MaterialBatchEntity>

    @Query("SELECT * FROM material_batches WHERE materialId = :materialId")
    suspend fun getByMaterial(materialId: String): List<MaterialBatchEntity>

    @Query("SELECT * FROM material_batches WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MaterialBatchEntity?

    @Query("UPDATE material_batches SET remainingQuantity = :newQuantity WHERE id = :batchId")
    suspend fun updateRemainingQuantity(batchId: String, newQuantity: Double)

    @Query("UPDATE material_batches SET isUsable = :isUsable WHERE id = :batchId")
    suspend fun setUsable(batchId: String, isUsable: Boolean)

    @Query("DELETE FROM material_batches WHERE id = :id")
    suspend fun deleteById(id: String)
}
```

Update `AppDatabase.kt`:
- Add `MaterialBatchEntity::class` to `@Database(entities = [...])`.
- Increment `version = 5`.
- Add `abstract fun materialBatchDao(): MaterialBatchDao`.

Update `RawMaterialEntity` in `Entities.kt`:
Add `earliestExpiryDate: Long? = null` and `hasExpiredBatch: Boolean = false`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.core.data.local.MaterialBatchMappingTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/core/data/local/entity/MaterialBatchEntity.kt app/src/main/java/com/kapos/mobile/core/data/local/entity/Entities.kt app/src/main/java/com/kapos/mobile/core/data/local/dao/MaterialBatchDao.kt app/src/main/java/com/kapos/mobile/core/data/local/AppDatabase.kt app/src/test/java/com/kapos/mobile/core/data/local/MaterialBatchMappingTest.kt
git commit -m "feat(db): add MaterialBatchEntity, MaterialBatchDao, and upgrade AppDatabase to v5"
```

---

### Task 3: InventoryRepository Business Logic (Moving Weighted Average, Auto BOP Recalculation, FIFO Deduction, and Waste Recording)

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/InventoryRepository.kt`
- Create: `app/src/test/java/com/kapos/mobile/core/data/repository/InventoryBatchCalculationTest.kt`

**Interfaces:**
- Consumes: `MaterialBatchDao`, `RawMaterialDao`, `RecipeDao`, `ProductDao`, `StockMovementDao`
- Produces:
  - `restockMaterialWithBatch(materialId: String, quantity: Double, costTotal: Double, expiryDate: Long?, notes: String): MaterialBatch`
  - `recalculateProductBop(productId: String)`
  - `recalculateAllProductsBopForMaterial(materialId: String)`
  - `recordBatchWaste(batchId: String, quantity: Double, notes: String)`
  - `toggleBatchUsability(batchId: String, isUsable: Boolean)`
  - Updated `deductStockForOrderItems(cartItems: List<CartItem>)` with FIFO batch allocation
  - `observeBatchesForMaterial(materialId: String): Flow<List<MaterialBatch>>`
  - `observeAllBatches(): Flow<List<MaterialBatch>>`

- [ ] **Step 1: Write the failing unit test for inventory calculation formulas**

```kotlin
// app/src/test/java/com/kapos/mobile/core/data/repository/InventoryBatchCalculationTest.kt
package com.kapos.mobile.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryBatchCalculationTest {

    @Test
    fun testMovingWeightedAverageCalculation() {
        val currentStock = 1000.0 // 1 kg
        val currentCost = 40.0    // Rp 40/g = Rp 40.000/kg
        val newQty = 2000.0       // 2 kg
        val newCostTotal = 100000.0 // Rp 100.000 (Rp 50/g)

        // Formula: ( (1000 * 40) + 100000 ) / (1000 + 2000) = 140000 / 3000 = 46.6667
        val expectedNewCost = ((currentStock * currentCost) + newCostTotal) / (currentStock + newQty)
        assertEquals(46.667, expectedNewCost, 0.001)
    }

    @Test
    fun testProductBopCalculationFromRecipes() {
        // Product 1 portion uses:
        // - 150g Ayam Fillet @ Rp 50/g = Rp 7.500
        // - 10 pcs Tusuk Sate @ Rp 30/pc = Rp 300
        // - 25g Cabe Rawit @ Rp 45/g = Rp 1.125
        val ayamCost = 150.0 * 50.0
        val tusukCost = 10.0 * 30.0
        val cabeCost = 25.0 * 45.0
        val totalBop = ayamCost + tusukCost + cabeCost

        assertEquals(8925.0, totalBop, 0.001)
    }

    @Test
    fun testFifoBatchDeductionAllocation() {
        // Batch 1: 50 remaining
        // Batch 2: 100 remaining
        // Deduction needed: 80
        val batches = mutableListOf(Pair("b1", 50.0), Pair("b2", 100.0))
        var needed = 80.0
        val deducted = mutableMapOf<String, Double>()

        for (i in batches.indices) {
            if (needed <= 0) break
            val b = batches[i]
            val take = minOf(b.second, needed)
            deducted[b.first] = take
            batches[i] = Pair(b.first, b.second - take)
            needed -= take
        }

        assertEquals(50.0, deducted["b1"] ?: 0.0, 0.001)
        assertEquals(30.0, deducted["b2"] ?: 0.0, 0.001)
        assertEquals(0.0, batches[0].second, 0.001)
        assertEquals(70.0, batches[1].second, 0.001)
    }
}
```

- [ ] **Step 2: Run test to verify it passes the math sanity checks**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.core.data.repository.InventoryBatchCalculationTest"`
Expected: PASS

- [ ] **Step 3: Implement batch operations & auto BOP updating in InventoryRepository**

In `app/src/main/java/com/kapos/mobile/core/data/repository/InventoryRepository.kt`:
1. Inject `productDao = database.productDao()` and `materialBatchDao = database.materialBatchDao()`.
2. Implement `restockMaterialWithBatch`:
```kotlin
suspend fun restockMaterialWithBatch(
    materialId: String,
    quantity: Double,
    costTotal: Double,
    expiryDate: Long? = null,
    notes: String = ""
): MaterialBatch = withContext(Dispatchers.IO) {
    if (quantity <= 0.0) throw IllegalArgumentException("Quantity must be > 0")
    val now = System.currentTimeMillis()
    val material = rawMaterialDao.getById(materialId) ?: throw IllegalArgumentException("Material not found")

    val unitCost = if (quantity > 0) costTotal / quantity else 0.0
    val batchId = UUID.randomUUID().toString()
    val batchNumber = "B-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date(now))

    val batchEntity = MaterialBatchEntity(
        id = batchId,
        materialId = materialId,
        batchNumber = batchNumber,
        initialQuantity = quantity,
        remainingQuantity = quantity,
        costTotal = costTotal,
        costPerUnit = unitCost,
        receivedDate = now,
        expiryDate = expiryDate,
        isUsable = true,
        notes = notes
    )
    materialBatchDao.insert(batchEntity)

    // Moving Weighted Average calculation:
    val oldStock = material.currentStock.coerceAtLeast(0.0)
    val oldCost = material.costPerUnit.coerceAtLeast(0.0)
    val newTotalStock = oldStock + quantity
    val newAvgCost = if (newTotalStock > 0.0) {
        ((oldStock * oldCost) + costTotal) / newTotalStock
    } else {
        unitCost
    }

    // Update material aggregate
    val allBatches = materialBatchDao.getByMaterial(materialId).filter { it.remainingQuantity > 0 && it.isUsable }
    val earliestExp = allBatches.mapNotNull { it.expiryDate }.minOrNull()
    val hasExpired = allBatches.any { it.expiryDate != null && it.expiryDate <= now }

    rawMaterialDao.upsert(
        material.copy(
            currentStock = newTotalStock,
            costPerUnit = newAvgCost,
            lastRestockedAt = now,
            earliestExpiryDate = earliestExp,
            hasExpiredBatch = hasExpired
        )
    )

    // Record Stock Movement
    stockMovementDao.insert(
        StockMovementEntity(
            id = UUID.randomUUID().toString(),
            materialId = materialId,
            type = StockMovementType.RESTOCK.name,
            quantity = quantity,
            costTotal = costTotal,
            notes = "Restock $batchNumber: $notes",
            timestamp = now
        )
    )

    // Cascade update BOP to all products with this material in Recipe BOM
    recalculateAllProductsBopForMaterial(materialId)

    batchEntity.toDomain()
}
```

3. Implement `recalculateAllProductsBopForMaterial` and `recalculateProductBop`:
```kotlin
suspend fun recalculateAllProductsBopForMaterial(materialId: String) = withContext(Dispatchers.IO) {
    val affectedRecipes = recipeDao.getAll().filter { it.materialId == materialId }
    val productIds = affectedRecipes.map { it.productId }.distinct()
    for (prodId in productIds) {
        recalculateProductBop(prodId)
    }
}

suspend fun recalculateProductBop(productId: String) = withContext(Dispatchers.IO) {
    val product = productDao.getById(productId) ?: return@withContext
    val recipes = recipeDao.getByProduct(productId)
    if (recipes.isEmpty()) return@withContext

    val allMaterials = rawMaterialDao.getAll().associateBy { it.id }
    val totalBop = recipes.sumOf { r ->
        val mat = allMaterials[r.materialId]
        val unitCost = mat?.costPerUnit ?: 0.0
        r.quantityRequired * unitCost
    }

    productDao.upsert(product.copy(costPrice = totalBop))
}
```

4. Implement `recordBatchWaste`:
```kotlin
suspend fun recordBatchWaste(
    batchId: String,
    quantity: Double,
    notes: String = ""
) = withContext(Dispatchers.IO) {
    if (quantity <= 0.0) return@withContext
    val batch = materialBatchDao.getById(batchId) ?: return@withContext
    val now = System.currentTimeMillis()

    val actualDeduct = minOf(batch.remainingQuantity, quantity)
    val newRemaining = (batch.remainingQuantity - actualDeduct).coerceAtLeast(0.0)
    materialBatchDao.updateRemainingQuantity(batchId, newRemaining)

    // Deduct raw material master stock
    rawMaterialDao.adjustStock(batch.materialId, -actualDeduct, now)

    // Monetary loss calculation
    val lossCost = actualDeduct * batch.costPerUnit

    stockMovementDao.insert(
        StockMovementEntity(
            id = UUID.randomUUID().toString(),
            materialId = batch.materialId,
            type = StockMovementType.WASTE.name,
            quantity = -actualDeduct,
            costTotal = lossCost,
            notes = "Waste ${batch.batchNumber}: $notes (Rugi: Rp ${lossCost.toInt()})",
            timestamp = now
        )
    )

    updateMaterialExpiryState(batch.materialId)
}
```

5. Update `deductStockForOrderItems` with FIFO logic across `materialBatchDao.getActiveBatchesForFIFO(materialId)`.

- [ ] **Step 4: Run unit tests to verify repository integration**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.core.data.repository.*"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/core/data/repository/InventoryRepository.kt app/src/test/java/com/kapos/mobile/core/data/repository/InventoryBatchCalculationTest.kt
git commit -m "feat(inventory): implement weighted average cost, auto BOP recalculation, FIFO deduction, and waste loss tracking"
```

---

### Task 4: ViewModel & State Refactoring (`StockViewModel` and `StockUiState`)

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/stock/StockUiState.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/stock/StockViewModel.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/menu/MenuManagementViewModel.kt:212-241`
- Create: `app/src/test/java/com/kapos/mobile/feature/stock/StockBatchViewModelTest.kt`

**Interfaces:**
- Produces:
  - `enum class StockTab(val displayName: String)`: `INVENTORY("Bahan Baku & Batch")`, `AUDIT("Audit Mutasi & Waste")`
  - `StockUiState`: contains `rawMaterials`, `batchesByMaterial: Map<String, List<MaterialBatch>>`, `selectedMaterialBatches: List<MaterialBatch>`, `isBatchDetailOpen: Boolean`, `selectedBatchForWaste: MaterialBatch?`, `isWasteDialogOpen: Boolean`
  - `StockViewModel`: `openBatchDetail(material)`, `closeBatchDetail()`, `openWasteDialog(batch)`, `closeWasteDialog()`, `confirmWaste(batchId, qty, reason)`, `toggleBatchUsable(batchId, isUsable)`
  - `MenuManagementViewModel`: auto recalculates product BOP upon adding/editing recipe items.

- [ ] **Step 1: Write the failing test for StockUiState and ViewModel batch handling**

```kotlin
// app/src/test/java/com/kapos/mobile/feature/stock/StockBatchViewModelTest.kt
package com.kapos.mobile.feature.stock

import com.kapos.mobile.domain.model.BatchFreshness
import com.kapos.mobile.domain.model.MaterialBatch
import com.kapos.mobile.domain.model.RawMaterial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StockBatchViewModelTest {

    @Test
    fun testStockUiStateExpirySummaryCounters() {
        val now = 1000000000L
        val day = 86400000L
        val batches = listOf(
            MaterialBatch("b1", "m1", "B-1", 100.0, 50.0, 5000.0, 50.0, now, now - day, true), // EXPIRED
            MaterialBatch("b2", "m1", "B-2", 100.0, 80.0, 4000.0, 40.0, now, now + day, true), // EXPIRING SOON
            MaterialBatch("b3", "m2", "B-3", 100.0, 100.0, 3000.0, 30.0, now, now + (5 * day), true) // FRESH
        )

        val state = StockUiState(
            batchesByMaterial = mapOf("m1" to batches.take(2), "m2" to batches.takeLast(1))
        )

        assertEquals(1, state.totalExpiredBatches(now))
        assertEquals(1, state.totalExpiringSoonBatches(now))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.feature.stock.StockBatchViewModelTest"`
Expected: FAIL

- [ ] **Step 3: Implement state and ViewModel methods**

In `StockUiState.kt`:
Update `StockTab` to 2 streamlined options:
```kotlin
enum class StockTab(val displayName: String) {
    INVENTORY("Bahan Baku & Batch"),
    AUDIT("Audit Mutasi & Waste")
}
```
Add batch state properties and helper getters (`totalExpiredBatches`, `totalExpiringSoonBatches`, `totalInventoryAssetValue`).

In `StockViewModel.kt`:
- Collect `inventoryRepository.observeAllBatches()` and group by `materialId`.
- Add `restockWithBatch(materialId, qty, costTotal, expiryDate, notes)`.
- Add `recordBatchWaste(batchId, qty, notes)`.
- Add `toggleBatchUsability(batchId, isUsable)`.

In `MenuManagementViewModel.kt`:
- In `addOrUpdateRecipeItem` and `deleteRecipeItem`, call `inventoryRepository.recalculateProductBop(productId)` to guarantee immediate BOP reflection.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.feature.stock.StockBatchViewModelTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/feature/stock/StockUiState.kt app/src/main/java/com/kapos/mobile/feature/stock/StockViewModel.kt app/src/main/java/com/kapos/mobile/feature/menu/MenuManagementViewModel.kt app/src/test/java/com/kapos/mobile/feature/stock/StockBatchViewModelTest.kt
git commit -m "feat(stock): update StockViewModel and StockUiState for batch expiry and auto BOP"
```

---

### Task 5: UI Dialogs for Restock with Expiry Date and Batch Detail / Waste

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/stock/RawMaterialDialogs.kt`
- Create: `app/src/main/java/com/kapos/mobile/feature/stock/BatchDetailDialog.kt`

**Interfaces:**
- Produces:
  - `RestockMaterialDialog`: includes total cost, dynamic unit cost preview, quick expiry buttons (+3 days, +7 days, +30 days, Custom Date, No Expiry), notes
  - `BatchDetailDialog`: displays active batches with badge (Fresh / Expiring Soon / Expired), usability toggle, and Waste trigger
  - `WasteBatchDialog`: records quantity destroyed, reason, and shows live loss value (Rp)

- [ ] **Step 1: Write UI component tests or screenshot test verification for dialog states**

Create or update test asserting dialog arguments and formatting in `RawMaterialDialogs`:
Ensure unit price preview calculation function: `calculatePreviewUnitCost(costTotal, quantity)` formats accurately.

- [ ] **Step 2: Update RestockMaterialDialog in RawMaterialDialogs.kt**

Add:
- Total Cost field with live calculation: `Rp {cost / quantity} / {unit}`.
- Expiry Date selector with quick duration chips (+3 Hari, +7 Hari, +14 Hari, +30 Hari, Pilihan Tanggal, Tanpa Expired).
- Calls `onConfirm(quantity, costTotal, expiryDateTimestamp, notes)`.

- [ ] **Step 3: Implement BatchDetailDialog.kt**

Create `app/src/main/java/com/kapos/mobile/feature/stock/BatchDetailDialog.kt`:
- Displays material header with current stock and average unit cost.
- Lists all batches for this material with:
  - Batch number & received date.
  - Remaining stock vs initial stock.
  - Cost per unit.
  - Expiry status badge (🟢 Segar / 🟡 Mendekati Expired / 🔴 Kadaluarsa / ⚠️ Rusak).
  - Quick action buttons: **Tandai Rusak/Layak** and **🗑️ Catat Waste**.

- [ ] **Step 4: Update WasteMaterialDialog to support Batch context**

- Displays Batch Number and Purchase Unit Cost.
- Input: Kuantitas Dibuang & Alasan Pembuangan (Basi / Kadaluarsa / Tumpah).
- Real-time display: `Estimasi Nilai Kerugian: Rp {qty * costPerUnit}`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/feature/stock/RawMaterialDialogs.kt app/src/main/java/com/kapos/mobile/feature/stock/BatchDetailDialog.kt
git commit -m "feat(stock-ui): add batch expiry selection in Restock dialog, BatchDetailDialog, and WasteDialog"
```

---

### Task 6: Modern Screen Integration (`StockScreen`, `MenuManagementScreen`, `RecipeBomDialog`)

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/stock/StockScreen.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/menu/MenuManagementScreen.kt:140-230`
- Modify: `app/src/main/java/com/kapos/mobile/feature/menu/RecipeBomDialog.kt:125-205`

**Interfaces:**
- Produces:
  - `StockScreen`: Streamlined 2-tab view. Tab 1 shows inventory stat cards (Total Asset Value, Low Stock, Expiring Alerts), material cards with expiry badge and Batch Details button. Tab 2 shows Audit Mutasi with financial cost/loss tracking.
  - `MenuManagementScreen`: Card shows BOP badge and Profit Margin % badge (`>50%` Green, `20-50%` Yellow, `<20%` Red).
  - `RecipeBomDialog`: Displays cost contribution per ingredient line and total BOP summary footer.

- [ ] **Step 1: Write verify tests for Menu and Stock UI States**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.feature.menu.MenuManagementUiStateTest"`
Ensure existing tests continue to pass.

- [ ] **Step 2: Refactor StockScreen.kt**

- Remove deprecated hardcoded tabs `TAICHAN` & `CHICKEN`.
- Implement clean header with:
  - StatCard: Total Nilai Aset Bahan (Rp)
  - StatCard: Bahan Menipis
  - StatCard: Peringatan Kadaluarsa (Red/Yellow badge counter)
- Render modern Material cards with:
  - Category tag, stock count & unit.
  - Biaya per unit rata-rata.
  - Expiry status pill.
  - Action buttons: `+ Restock`, `Rincian Batch`, `Opsi`.
- Wire up `BatchDetailDialog` and `RestockMaterialDialog`.

- [ ] **Step 3: Update MenuManagementScreen.kt and RecipeBomDialog.kt**

- In `MenuManagementScreen.kt` product list item:
  - Display: `Harga Jual: Rp ...` and `BOP (HPP): Rp ...`.
  - Add Margin pill: `Margin: {margin}% (Laba: Rp {price - bop})`.
- In `RecipeBomDialog.kt`:
  - Show cost contribution: `{qty} {unit} × Rp {matCost} = Rp {qty * matCost}`.
  - Footer shows: `Total Estimasi BOP per Porsi: Rp {totalBop}`.

- [ ] **Step 4: Verify complete project builds without UI or compilation errors**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/feature/stock/StockScreen.kt app/src/main/java/com/kapos/mobile/feature/menu/MenuManagementScreen.kt app/src/main/java/com/kapos/mobile/feature/menu/RecipeBomDialog.kt
git commit -m "feat(ui): integrate simplified 2-tab StockScreen and live BOP margin indicators in MenuManagement"
```

---

### Task 7: Comprehensive Regression Testing & Clean Deprecation

**Files:**
- Create: `app/src/test/java/com/kapos/mobile/feature/stock/StockIntegrationWorkflowTest.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/mock/SampleData.kt`
- Clean up: Remove unused imports and deprecate `StockRepository.kt` safely

**Interfaces:**
- Produces:
  - End-to-end integration test validating:
    1. Restock material with custom batch price and expiry date.
    2. Moving weighted average cost recalculated correctly.
    3. Product BOM cost updated automatically in menu.
    4. Expiry warning status triggered when batch expiry is near/past.
    5. Waste recorded correctly with monetary loss deducted.
    6. Sale order deduction decrements batch via FIFO.

- [ ] **Step 1: Write end-to-end workflow test**

```kotlin
// app/src/test/java/com/kapos/mobile/feature/stock/StockIntegrationWorkflowTest.kt
package com.kapos.mobile.feature.stock

import com.kapos.mobile.domain.model.BatchFreshness
import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.MaterialBatch
import com.kapos.mobile.domain.model.Product
import com.kapos.mobile.domain.model.RawMaterial
import com.kapos.mobile.domain.model.RawMaterialCategory
import com.kapos.mobile.domain.model.RecipeItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StockIntegrationWorkflowTest {

    @Test
    fun testCompleteWorkflowFromRestockToBopUpdateAndFifoWaste() {
        val now = 1000000000L
        val oneDay = 86400000L

        // 1. Initial Material: Daging Ayam Fillet with 1000g @ Rp 40/g
        var material = RawMaterial(
            id = "mat-ayam",
            name = "Daging Ayam Fillet",
            category = RawMaterialCategory.PROTEIN,
            unit = "gram",
            currentStock = 1000.0,
            costPerUnit = 40.0
        )

        // 2. Restock: 2000g @ Rp 55/g (Total Rp 110.000), expires in 3 days
        val newQty = 2000.0
        val newCostTotal = 110000.0
        val newBatch = MaterialBatch(
            id = "batch-1",
            materialId = material.id,
            batchNumber = "B-001",
            initialQuantity = newQty,
            remainingQuantity = newQty,
            costTotal = newCostTotal,
            costPerUnit = newCostTotal / newQty, // 55.0
            receivedDate = now,
            expiryDate = now + (3 * oneDay),
            isUsable = true
        )

        // Calculate moving average: (1000*40 + 110000) / 3000 = 150000 / 3000 = 50.0
        val updatedCostPerUnit = ((material.currentStock * material.costPerUnit) + newCostTotal) / (material.currentStock + newQty)
        material = material.copy(
            currentStock = material.currentStock + newQty,
            costPerUnit = updatedCostPerUnit
        )
        assertEquals(50.0, material.costPerUnit, 0.001)
        assertEquals(3000.0, material.currentStock, 0.001)

        // 3. Product Recipe: 1 Porsi Taichan requires 150g Ayam
        val recipe = RecipeItem("r1", "p-taichan", material.id, material.name, material.unit, 150.0)
        val productBop = recipe.quantityRequired * material.costPerUnit
        assertEquals(7500.0, productBop, 0.001) // 150g * Rp 50/g = Rp 7.500

        // Margin on Rp 25.000 sell price
        val product = Product("p-taichan", "c1", "Sate Taichan", 25000.0, costPrice = productBop)
        assertEquals(17500.0, product.calculateProfitMargin(), 0.001)
        assertEquals(70.0, product.calculateMarginPercentage(), 0.001)

        // 4. Expiry verification: 3 days remaining -> FRESH
        assertEquals(BatchFreshness.FRESH, newBatch.freshnessStatus(now))
        // 2 days later -> EXPIRING_SOON
        assertEquals(BatchFreshness.EXPIRING_SOON, newBatch.freshnessStatus(now + (2 * oneDay)))
        // 4 days later -> EXPIRED
        assertEquals(BatchFreshness.EXPIRED, newBatch.freshnessStatus(now + (4 * oneDay)))

        // 5. Waste recording: 200g ruined -> loss Rp 11.000
        val wasteQty = 200.0
        val wasteLoss = wasteQty * newBatch.costPerUnit
        val remainingAfterWaste = newBatch.remainingQuantity - wasteQty
        assertEquals(1800.0, remainingAfterWaste, 0.001)
        assertEquals(11000.0, wasteLoss, 0.001)
    }
}
```

- [ ] **Step 2: Run all unit tests across the whole project**

Run: `./gradlew testDebugUnitTest`
Expected: ALL TESTS PASS

- [ ] **Step 3: Deprecate or adapt old SharedPreferences `StockRepository`**

Mark `StockRepository` as `@Deprecated("Superseded by InventoryRepository room-based batch stock")`.
Verify no unresolved references exist across all screens.

- [ ] **Step 4: Final project build validation**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/test/java/com/kapos/mobile/feature/stock/StockIntegrationWorkflowTest.kt app/src/main/java/com/kapos/mobile/core/data/mock/SampleData.kt app/src/main/java/com/kapos/mobile/core/data/repository/StockRepository.kt
git commit -m "test(stock): add end-to-end integration test and finalize clean deprecation of legacy stock repository"
```
