package com.kapos.mobile.core.data.repository

import com.kapos.mobile.core.data.local.AppDatabase
import com.kapos.mobile.core.data.local.entity.RawMaterialEntity
import com.kapos.mobile.core.data.local.entity.RecipeItemEntity
import com.kapos.mobile.core.data.local.entity.StockMovementEntity
import com.kapos.mobile.core.data.mock.SampleData
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
import java.util.UUID

class InventoryRepository(
    private val database: AppDatabase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val rawMaterialDao = database.rawMaterialDao()
    private val recipeDao = database.recipeDao()
    private val stockMovementDao = database.stockMovementDao()

    init {
        scope.launch {
            seedInitialDataIfEmpty()
        }
    }

    private suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingMaterials = rawMaterialDao.getAll()
        if (existingMaterials.isEmpty()) {
            val entities = SampleData.rawMaterials.map {
                RawMaterialEntity(
                    id = it.id,
                    name = it.name,
                    category = it.category.name,
                    unit = it.unit,
                    currentStock = it.currentStock,
                    minStock = it.minStock,
                    costPerUnit = it.costPerUnit,
                    lastRestockedAt = it.lastRestockedAt
                )
            }
            rawMaterialDao.upsertAll(entities)
        }

        val existingRecipes = recipeDao.getAll()
        if (existingRecipes.isEmpty()) {
            recipeDao.upsertAll(SampleData.recipeItems)
        }
    }

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
        if (quantity <= 0) return@withContext
        val now = System.currentTimeMillis()
        rawMaterialDao.adjustStock(materialId, quantity, now)
        stockMovementDao.insert(
            StockMovementEntity(
                id = UUID.randomUUID().toString(),
                materialId = materialId,
                type = StockMovementType.RESTOCK.name,
                quantity = quantity,
                costTotal = cost,
                notes = notes,
                timestamp = now
            )
        )
    }

    suspend fun recordWaste(
        materialId: String,
        quantity: Double,
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
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
    }

    suspend fun deleteRecipeItem(id: String) = withContext(Dispatchers.IO) {
        recipeDao.deleteById(id)
    }

    /**
     * Automatic BOM stock deduction when checkout / sale is confirmed.
     */
    suspend fun deductStockForOrderItems(cartItems: List<CartItem>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val allRecipes = recipeDao.getAll()
        val movementsToInsert = mutableListOf<StockMovementEntity>()

        for (cart in cartItems) {
            val recipesForProduct = allRecipes.filter { it.productId == cart.product.id }
            for (recipe in recipesForProduct) {
                val totalDeduction = recipe.quantityRequired * cart.quantity
                rawMaterialDao.adjustStock(recipe.materialId, -totalDeduction, now)
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
                        type = StockMovementType.ADJUSTMENT.name,
                        quantity = totalRestoration,
                        costTotal = 0.0,
                        notes = "Void Order Restoration: ${item.quantity}x ${item.productName}",
                        timestamp = now
                    )
                )
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
        lastRestockedAt = lastRestockedAt
    )
}
