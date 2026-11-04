package com.kapos.mobile.feature.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapos.mobile.core.data.repository.InventoryRepository
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.domain.model.MaterialBatch
import com.kapos.mobile.domain.model.RawMaterial
import com.kapos.mobile.domain.model.RawMaterialCategory
import com.kapos.mobile.domain.model.StockMovement
import com.kapos.mobile.domain.model.StockMovementType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class StockViewModel(
    private val posRepository: PosRepository? = null,
    private val inventoryRepository: InventoryRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : ViewModel() {

    private val _activeTab = MutableStateFlow(StockTab.INVENTORY)

    // Raw Material & Batch states
    private val _rawMaterials = MutableStateFlow<List<RawMaterial>>(emptyList())
    private val _batchesByMaterial = MutableStateFlow<Map<String, List<MaterialBatch>>>(emptyMap())
    private val _selectedMaterialCategory = MutableStateFlow<RawMaterialCategory?>(null)
    private val _materialSearchQuery = MutableStateFlow("")
    private val _selectedMaterialForAction = MutableStateFlow<RawMaterial?>(null)
    private val _selectedMaterialForBatches = MutableStateFlow<RawMaterial?>(null)
    private val _isBatchDetailOpen = MutableStateFlow(false)
    private val _selectedBatchForWaste = MutableStateFlow<MaterialBatch?>(null)
    private val _isWasteDialogOpen = MutableStateFlow(false)
    private val _isAddEditMaterialOpen = MutableStateFlow(false)
    private val _materialToEdit = MutableStateFlow<RawMaterial?>(null)
    private val _isRestockMaterialOpen = MutableStateFlow(false)
    private val _isWasteMaterialOpen = MutableStateFlow(false)
    private val _isOpnameMaterialOpen = MutableStateFlow(false)

    // Stock Movements Audit Log
    private val _stockMovements = MutableStateFlow<List<StockMovement>>(emptyList())
    private val _movementFilterType = MutableStateFlow<StockMovementType?>(null)

    init {
        inventoryRepository?.let { inv ->
            scope.launch {
                inv.observeRawMaterials().collect { list ->
                    _rawMaterials.value = list
                }
            }

            scope.launch {
                inv.observeAllBatches().collect { list ->
                    _batchesByMaterial.value = list.groupBy { it.materialId }
                }
            }

            scope.launch {
                inv.observeMovements(100).collect { list ->
                    _stockMovements.value = list
                }
            }
        }
    }

    val uiState: StateFlow<StockUiState> = combine(
        _activeTab,
        combine(
            _rawMaterials,
            _batchesByMaterial,
            _selectedMaterialCategory,
            _materialSearchQuery,
            _selectedMaterialForAction
        ) { mats, batchesMap, cat, query, selectedMat ->
            listOf(mats, batchesMap, cat, query, selectedMat)
        },
        combine(
            _isAddEditMaterialOpen,
            _materialToEdit,
            _isRestockMaterialOpen,
            _isWasteMaterialOpen,
            _isOpnameMaterialOpen
        ) { addEdit, toEdit, restock, waste, opname ->
            listOf(addEdit, toEdit, restock, waste, opname)
        },
        combine(
            _stockMovements,
            _movementFilterType,
            _selectedMaterialForBatches,
            _isBatchDetailOpen,
            _selectedBatchForWaste
        ) { movements, filterType, matBatches, isBatchOpen, wasteBatch ->
            listOf(movements, filterType, matBatches, isBatchOpen, wasteBatch)
        },
        _isWasteDialogOpen
    ) { tab, matList, dialogList, batchDialogList, isWasteDialog ->
        @Suppress("UNCHECKED_CAST")
        val mats = matList[0] as List<RawMaterial>
        @Suppress("UNCHECKED_CAST")
        val batchesMap = matList[1] as Map<String, List<MaterialBatch>>
        val cat = matList[2] as RawMaterialCategory?
        val query = matList[3] as String
        val selectedMat = matList[4] as RawMaterial?

        val addEdit = dialogList[0] as Boolean
        val toEdit = dialogList[1] as RawMaterial?
        val restock = dialogList[2] as Boolean
        val waste = dialogList[3] as Boolean
        val opname = dialogList[4] as Boolean

        @Suppress("UNCHECKED_CAST")
        val movements = batchDialogList[0] as List<StockMovement>
        val movementFilter = batchDialogList[1] as StockMovementType?
        val matBatches = batchDialogList[2] as RawMaterial?
        val isBatchOpen = batchDialogList[3] as Boolean
        val wasteBatch = batchDialogList[4] as MaterialBatch?

        StockUiState(
            activeTab = tab,
            rawMaterials = mats,
            batchesByMaterial = batchesMap,
            selectedMaterialCategory = cat,
            materialSearchQuery = query,
            selectedMaterialForAction = selectedMat,
            selectedMaterialForBatches = matBatches,
            isBatchDetailOpen = isBatchOpen,
            selectedBatchForWaste = wasteBatch,
            isWasteDialogOpen = isWasteDialog,
            isAddEditMaterialOpen = addEdit,
            materialToEdit = toEdit,
            isRestockMaterialOpen = restock,
            isWasteMaterialOpen = waste,
            isOpnameMaterialOpen = opname,
            stockMovements = movements,
            movementFilterType = movementFilter
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = StockUiState()
    )

    fun onTabSelect(tab: StockTab) {
        _activeTab.value = tab
    }

    fun onSelectMovementFilter(type: StockMovementType?) {
        _movementFilterType.value = type
    }

    // --- Raw Materials & BOM Actions ---
    fun onMaterialCategorySelect(category: RawMaterialCategory?) {
        _selectedMaterialCategory.value = category
    }

    fun onMaterialSearchQueryChange(query: String) {
        _materialSearchQuery.value = query
    }

    fun openAddMaterial() {
        _materialToEdit.value = null
        _isAddEditMaterialOpen.value = true
    }

    fun openEditMaterial(material: RawMaterial) {
        _materialToEdit.value = material
        _isAddEditMaterialOpen.value = true
    }

    fun closeAddEditMaterial() {
        _isAddEditMaterialOpen.value = false
        _materialToEdit.value = null
    }

    fun saveMaterial(
        name: String,
        category: RawMaterialCategory,
        unit: String,
        currentStock: Double,
        minStock: Double,
        costPerUnit: Double
    ) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val current = _materialToEdit.value
        viewModelScope.launch {
            val material = if (current != null) {
                current.copy(
                    name = trimmed,
                    category = category,
                    unit = unit.trim().ifBlank { "gram" },
                    currentStock = currentStock.coerceAtLeast(0.0),
                    minStock = minStock.coerceAtLeast(0.0),
                    costPerUnit = costPerUnit.coerceAtLeast(0.0)
                )
            } else {
                RawMaterial(
                    id = UUID.randomUUID().toString(),
                    name = trimmed,
                    category = category,
                    unit = unit.trim().ifBlank { "gram" },
                    currentStock = 0.0,
                    minStock = minStock.coerceAtLeast(0.0),
                    costPerUnit = costPerUnit.coerceAtLeast(0.0),
                    lastRestockedAt = System.currentTimeMillis()
                )
            }
            inventoryRepository?.saveRawMaterial(material)
            if (current == null && currentStock > 0.0) {
                inventoryRepository?.restockMaterialWithBatch(
                    materialId = material.id,
                    quantity = currentStock,
                    costTotal = currentStock * costPerUnit.coerceAtLeast(0.0),
                    expiryDate = null,
                    notes = "Stok awal pendaftaran bahan baku",
                    receivedDate = System.currentTimeMillis()
                )
            }
            closeAddEditMaterial()
        }
    }

    fun deleteMaterial(id: String) {
        viewModelScope.launch {
            inventoryRepository?.deleteRawMaterial(id)
        }
    }

    fun openRestockMaterial(material: RawMaterial) {
        _selectedMaterialForAction.value = material
        _isRestockMaterialOpen.value = true
    }

    fun closeRestockMaterial() {
        _isRestockMaterialOpen.value = false
        _selectedMaterialForAction.value = null
    }

    fun confirmRestockMaterial(materialId: String, quantity: Double, cost: Double, notes: String) {
        confirmRestockWithBatch(materialId, quantity, cost, null, notes)
    }

    fun confirmRestockWithBatch(
        materialId: String,
        quantity: Double,
        costTotal: Double,
        expiryDate: Long?,
        notes: String,
        receivedDate: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            inventoryRepository?.restockMaterialWithBatch(
                materialId = materialId,
                quantity = quantity,
                costTotal = costTotal,
                expiryDate = expiryDate,
                notes = notes,
                receivedDate = receivedDate
            )
            closeRestockMaterial()
        }
    }

    // --- Batch Detail and Waste Actions ---
    fun openBatchDetail(material: RawMaterial) {
        _selectedMaterialForBatches.value = material
        _isBatchDetailOpen.value = true
    }

    fun closeBatchDetail() {
        _isBatchDetailOpen.value = false
        _selectedMaterialForBatches.value = null
    }

    fun openWasteForBatch(batch: MaterialBatch) {
        _selectedBatchForWaste.value = batch
        _isWasteDialogOpen.value = true
    }

    fun closeWasteForBatch() {
        _isWasteDialogOpen.value = false
        _selectedBatchForWaste.value = null
    }

    fun confirmBatchWaste(batchId: String, quantity: Double, notes: String) {
        viewModelScope.launch {
            inventoryRepository?.recordBatchWaste(batchId, quantity, notes)
            closeWasteForBatch()
        }
    }

    fun toggleBatchUsability(batchId: String, isUsable: Boolean) {
        viewModelScope.launch {
            inventoryRepository?.toggleBatchUsability(batchId, isUsable)
        }
    }

    fun openWasteMaterial(material: RawMaterial) {
        _selectedMaterialForAction.value = material
        _isWasteMaterialOpen.value = true
    }

    fun closeWasteMaterial() {
        _isWasteMaterialOpen.value = false
        _selectedMaterialForAction.value = null
    }

    fun confirmWasteMaterial(materialId: String, quantity: Double, notes: String) {
        viewModelScope.launch {
            inventoryRepository?.recordWaste(materialId, quantity, notes)
            closeWasteMaterial()
        }
    }

    fun openOpnameMaterial(material: RawMaterial) {
        _selectedMaterialForAction.value = material
        _isOpnameMaterialOpen.value = true
    }

    fun closeOpnameMaterial() {
        _isOpnameMaterialOpen.value = false
        _selectedMaterialForAction.value = null
    }

    fun confirmOpnameMaterial(materialId: String, newStock: Double, notes: String) {
        viewModelScope.launch {
            inventoryRepository?.adjustStock(materialId, newStock, notes)
            closeOpnameMaterial()
        }
    }
}
