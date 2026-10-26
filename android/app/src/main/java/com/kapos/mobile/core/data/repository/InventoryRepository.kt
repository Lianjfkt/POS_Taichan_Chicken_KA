package com.kapos.mobile.core.data.repository

import com.kapos.mobile.core.data.local.AppDatabase
import com.kapos.mobile.core.data.local.entity.RawMaterialEntity
import com.kapos.mobile.core.data.local.entity.RecipeItemEntity
import com.kapos.mobile.core.data.local.entity.StockMovementEntity
import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.RawMaterial
import com.kapos.mobile.domain.model.RawMaterialCategory
import com.kapos.mobile.domain.model.RecipeItem
import com.kapos.mobile.domain.model.StockMovement
import com.kapos.mobile.domain.model.StockMovementType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kapos.mobile.core.data.local.entity.MaterialBatchEntity
import com.kapos.mobile.domain.model.MaterialBatch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class InventoryRepository(
    private val database: AppDatabase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val rawMaterialDao = database.rawMaterialDao()
    private val recipeDao = database.recipeDao()
    private val stockMovementDao = database.stockMovementDao()
    private val materialBatchDao = database.materialBatchDao()
    private val productDao = database.productDao()

    fun observeRawMaterials(): Flow<List<RawMaterial>> {
        return rawMaterialDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Identifies products whose required recipe materials are out of stock.
     */
    fun observeSoldOutProductIds(): Flow<Set<String>> {
        return kotlinx.coroutines.flow.combine(
            rawMaterialDao.observeAll(),
            recipeDao.observeAll()
        ) { materials, recipes ->
            val stockMap = materials.associate { it.id to it.currentStock }
            val soldOut = mutableSetOf<String>()
            recipes.groupBy { it.productId }.forEach { (productId, ingredients) ->
                val canMake = ingredients.all { ing ->
                    (stockMap[ing.materialId] ?: 0.0) >= ing.quantityRequired
                }
                if (!canMake) {
                    soldOut.add(productId)
                }
            }
            soldOut
        }
    }

    fun observeRawMaterialsByCategory(category: RawMaterialCategory): Flow<List<RawMaterial>> {
        return rawMaterialDao.observeByCategory(category.name).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeLowStockMaterials(): Flow<List<RawMaterial>> {
        return rawMaterialDao.observeLowStock().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getRawMaterialById(id: String): RawMaterial? = withContext(Dispatchers.IO) {
        rawMaterialDao.getById(id)?.toDomain()
    }

    suspend fun saveRawMaterial(material: RawMaterial): RawMaterial = withContext(Dispatchers.IO) {
        val id = if (material.id.isBlank()) UUID.randomUUID().toString() else material.id
        val entity = RawMaterialEntity(
            id = id,
            name = material.name.trim(),
            category = material.category.name,
            unit = material.unit.trim().ifBlank { "gram" },
            currentStock = material.currentStock.coerceAtLeast(0.0),
            minStock = material.minStock.coerceAtLeast(0.0),
            costPerUnit = material.costPerUnit.coerceAtLeast(0.0),
            lastRestockedAt = material.lastRestockedAt
        )
        rawMaterialDao.upsert(entity)
        entity.toDomain()
    }

    suspend fun deleteRawMaterial(id: String) = withContext(Dispatchers.IO) {
        rawMaterialDao.deleteById(id)
    }

    suspend fun restockMaterial(
        materialId: String,
        quantity: Double,
        cost: Double = 0.0,
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
        restockMaterialWithBatch(
            materialId = materialId,
            quantity = quantity,
            costTotal = cost,
            expiryDate = null,
            notes = notes
        )
    }

    suspend fun restockMaterialWithBatch(
        materialId: String,
        quantity: Double,
        costTotal: Double,
        expiryDate: Long? = null,
        notes: String = ""
    ): MaterialBatch = withContext(Dispatchers.IO) {
        if (quantity <= 0.0) throw IllegalArgumentException("Jumlah masuk harus > 0")
        val now = System.currentTimeMillis()
        val material = rawMaterialDao.getById(materialId) ?: throw IllegalArgumentException("Bahan tidak ditemukan")

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

        // Moving Weighted Average Calculation:
        val oldStock = material.currentStock.coerceAtLeast(0.0)
        val oldCost = material.costPerUnit.coerceAtLeast(0.0)
        val newTotalStock = oldStock + quantity
        val newAvgCost = if (newTotalStock > 0.0) {
            ((oldStock * oldCost) + costTotal) / newTotalStock
        } else {
            unitCost
        }

        rawMaterialDao.upsert(
            material.copy(
                currentStock = newTotalStock,
                costPerUnit = newAvgCost,
                lastRestockedAt = now
            )
        )

        updateMaterialExpiryState(materialId)

        // Record Stock Movement
        stockMovementDao.insert(
            StockMovementEntity(
                id = UUID.randomUUID().toString(),
                materialId = materialId,
                type = StockMovementType.RESTOCK.name,
                quantity = quantity,
                costTotal = costTotal,
                notes = "Restock $batchNumber: $notes (Harga Beli: Rp ${unitCost.toInt()}/${material.unit})",
                timestamp = now
            )
        )

        // Cascade update BOP to all products with this material in Recipe BOM
        recalculateAllProductsBopForMaterial(materialId)

        batchEntity.toDomain()
    }

    suspend fun recordWaste(
        materialId: String,
        quantity: Double,
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
        // Find active batches for this material to deduct
        val batches = materialBatchDao.getActiveBatchesForFIFO(materialId)
        val targetBatch = batches.firstOrNull()
        if (targetBatch != null) {
            recordBatchWaste(targetBatch.id, quantity, notes)
        } else {
            if (quantity <= 0) return@withContext
            val now = System.currentTimeMillis()
            rawMaterialDao.adjustStock(materialId, -quantity, now)
            stockMovementDao.insert(
                StockMovementEntity(
                    id = UUID.randomUUID().toString(),
                    materialId = materialId,
                    type = StockMovementType.WASTE.name,
                    quantity = -quantity,
                    costTotal = 0.0,
                    notes = notes,
                    timestamp = now
                )
            )
            updateMaterialExpiryState(materialId)
        }
    }

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
                notes = "Pemusnahan Batch ${batch.batchNumber}: $notes (Rugi: Rp ${lossCost.toInt()})",
                timestamp = now
            )
        )

        updateMaterialExpiryState(batch.materialId)
    }

    suspend fun toggleBatchUsability(batchId: String, isUsable: Boolean) = withContext(Dispatchers.IO) {
        val batch = materialBatchDao.getById(batchId) ?: return@withContext
        materialBatchDao.setUsable(batchId, isUsable)
        updateMaterialExpiryState(batch.materialId)
    }

    private suspend fun updateMaterialExpiryState(materialId: String) {
        val material = rawMaterialDao.getById(materialId) ?: return
        val now = System.currentTimeMillis()
        val allBatches = materialBatchDao.getByMaterial(materialId).filter { it.remainingQuantity > 0 && it.isUsable }
        val earliestExp = allBatches.mapNotNull { it.expiryDate }.minOrNull()
        val hasExpired = allBatches.any { it.expiryDate != null && it.expiryDate <= now }

        rawMaterialDao.upsert(
            material.copy(
                earliestExpiryDate = earliestExp,
                hasExpiredBatch = hasExpired
            )
        )
    }

    fun observeBatchesForMaterial(materialId: String): Flow<List<MaterialBatch>> {
        return materialBatchDao.observeByMaterial(materialId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeAllBatches(): Flow<List<MaterialBatch>> {
        return materialBatchDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun adjustStock(
        materialId: String,
        newStock: Double,
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
        val current = rawMaterialDao.getById(materialId)?.currentStock ?: 0.0
        val delta = newStock - current
        val now = System.currentTimeMillis()
        rawMaterialDao.setStock(materialId, newStock.coerceAtLeast(0.0), now)
        stockMovementDao.insert(
            StockMovementEntity(
                id = UUID.randomUUID().toString(),
                materialId = materialId,
                type = StockMovementType.ADJUSTMENT.name,
                quantity = delta,
                costTotal = 0.0,
                notes = notes,
                timestamp = now
            )
        )
    }

    fun observeMovements(limit: Int = 100): Flow<List<StockMovement>> {
        return stockMovementDao.observeRecent(limit).map { list ->
            val materials = rawMaterialDao.getAll().associateBy { it.id }
            list.map { entity ->
                val matName = materials[entity.materialId]?.name ?: "Bahan Baku"
                StockMovement(
                    id = entity.id,
                    materialId = entity.materialId,
                    materialName = matName,
                    type = try { StockMovementType.valueOf(entity.type) } catch (_: Exception) { StockMovementType.ADJUSTMENT },
                    quantity = entity.quantity,
                    costTotal = entity.costTotal,
                    notes = entity.notes,
                    timestamp = entity.timestamp
                )
            }
        }
    }

    // --- BOM Recipes ---
    fun observeRecipesForProduct(productId: String): Flow<List<RecipeItem>> {
        return recipeDao.observeByProduct(productId).map { list ->
            val materials = rawMaterialDao.getAll().associateBy { it.id }
            list.map {
                val mat = materials[it.materialId]
                RecipeItem(
                    id = it.id,
                    productId = it.productId,
                    materialId = it.materialId,
                    materialName = mat?.name ?: "Bahan",
                    unit = mat?.unit ?: "gram",
                    quantityRequired = it.quantityRequired
                )
            }
        }
    }

    suspend fun getRecipesForProduct(productId: String): List<RecipeItem> = withContext(Dispatchers.IO) {
        val materials = rawMaterialDao.getAll().associateBy { it.id }
        recipeDao.getByProduct(productId).map {
            val mat = materials[it.materialId]
            RecipeItem(
                id = it.id,
                productId = it.productId,
                materialId = it.materialId,
                materialName = mat?.name ?: "Bahan",
                unit = mat?.unit ?: "gram",
                quantityRequired = it.quantityRequired
            )
        }
    }

    suspend fun saveRecipeItem(productId: String, materialId: String, quantityRequired: Double) = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        recipeDao.upsert(
            RecipeItemEntity(
                id = id,
                productId = productId,
                materialId = materialId,
                quantityRequired = quantityRequired.coerceAtLeast(0.01)
            )
        )
        recalculateProductBop(productId)
    }

    suspend fun deleteRecipeItem(id: String) = withContext(Dispatchers.IO) {
        val item = recipeDao.getAll().firstOrNull { it.id == id }
        recipeDao.deleteById(id)
        if (item != null) {
            recalculateProductBop(item.productId)
        }
    }

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

    /**
     * Automatic BOM stock deduction when checkout / sale is confirmed.
     * Deducts using FIFO across active batches.
     */
    suspend fun deductStockForOrderItems(cartItems: List<CartItem>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val allRecipes = recipeDao.getAll()
        val movementsToInsert = mutableListOf<StockMovementEntity>()

        for (cart in cartItems) {
            val recipesForProduct = allRecipes.filter { it.productId == cart.product.id }
            for (recipe in recipesForProduct) {
                val totalDeduction = recipe.quantityRequired * cart.quantity
                
                // 1. Deduct master raw material stock
                rawMaterialDao.adjustStock(recipe.materialId, -totalDeduction, now)
                
                // 2. FIFO deduction across active batches
                val activeBatches = materialBatchDao.getActiveBatchesForFIFO(recipe.materialId)
                var remainingToDeduct = totalDeduction
                for (b in activeBatches) {
                    if (remainingToDeduct <= 0.0) break
                    val deductFromBatch = minOf(b.remainingQuantity, remainingToDeduct)
                    val newRemaining = (b.remainingQuantity - deductFromBatch).coerceAtLeast(0.0)
                    materialBatchDao.updateRemainingQuantity(b.id, newRemaining)
                    remainingToDeduct -= deductFromBatch
                }

                movementsToInsert.add(
                    StockMovementEntity(
                        id = UUID.randomUUID().toString(),
                        materialId = recipe.materialId,
                        type = StockMovementType.ORDER_DEDUCTION.name,
                        quantity = -totalDeduction,
                        costTotal = 0.0,
                        notes = "Auto BOM: ${cart.quantity}x ${cart.product.name}",
                        timestamp = now
                    )
                )
                
                updateMaterialExpiryState(recipe.materialId)
            }
        }

        if (movementsToInsert.isNotEmpty()) {
            stockMovementDao.insertAll(movementsToInsert)
        }
    }

    /**
     * Automatic BOM stock restoration when an order is voided.
     */
    suspend fun restoreStockForOrderItems(orderItems: List<com.kapos.mobile.domain.model.OrderItem>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val allRecipes = recipeDao.getAll()
        val movementsToInsert = mutableListOf<StockMovementEntity>()

        for (item in orderItems) {
            val recipesForProduct = allRecipes.filter { it.productId == item.productId }
            for (recipe in recipesForProduct) {
                val totalRestoration = recipe.quantityRequired * item.quantity
                rawMaterialDao.adjustStock(recipe.materialId, totalRestoration, now)
                movementsToInsert.add(
                    StockMovementEntity(
                        id = UUID.randomUUID().toString(),
                        materialId = recipe.materialId,
                        type = StockMovementType.VOID_RESTORATION.name,
                        quantity = totalRestoration,
                        costTotal = 0.0,
                        notes = "Void Order Restoration: ${item.quantity}x ${item.productName}",
                        timestamp = now
                    )
                )
                updateMaterialExpiryState(recipe.materialId)
            }
        }

        if (movementsToInsert.isNotEmpty()) {
            stockMovementDao.insertAll(movementsToInsert)
        }
    }

    private fun RawMaterialEntity.toDomain() = RawMaterial(
        id = id,
        name = name,
        category = try { RawMaterialCategory.valueOf(category) } catch (_: Exception) { RawMaterialCategory.BUMBU_SAMBAL },
        unit = unit,
        currentStock = currentStock,
        minStock = minStock,
        costPerUnit = costPerUnit,
        lastRestockedAt = lastRestockedAt,
        earliestExpiryDate = earliestExpiryDate,
        hasExpiredBatch = hasExpiredBatch
    )
}
