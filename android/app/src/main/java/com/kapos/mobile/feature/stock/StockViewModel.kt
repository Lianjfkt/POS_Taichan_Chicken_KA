package com.kapos.mobile.feature.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapos.mobile.core.data.repository.InventoryRepository
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.core.data.repository.StockRepository
import com.kapos.mobile.domain.model.ChickenPart
import com.kapos.mobile.domain.model.PaymentStatus
import com.kapos.mobile.domain.model.RawMaterial
import com.kapos.mobile.domain.model.RawMaterialCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class StockViewModel(
    private val stockRepository: StockRepository,
    private val posRepository: PosRepository? = null,
    private val inventoryRepository: InventoryRepository? = null,
    private val scope: kotlinx.coroutines.CoroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
) : ViewModel() {

    private val _activeTab = MutableStateFlow(StockTab.INVENTORY)
    private val _isRestockDialogOpen = MutableStateFlow(false)
    private val _restockChickenPart = MutableStateFlow(ChickenPart.SAYAP)
    private val _taichanSoldToday = MutableStateFlow(0)
    private val _chickenSoldToday = MutableStateFlow<Map<ChickenPart, Int>>(emptyMap())

    // Raw Material & Batch states
    private val _rawMaterials = MutableStateFlow<List<RawMaterial>>(emptyList())
    private val _batchesByMaterial = MutableStateFlow<Map<String, List<com.kapos.mobile.domain.model.MaterialBatch>>>(emptyMap())
    private val _selectedMaterialCategory = MutableStateFlow<RawMaterialCategory?>(null)
    private val _materialSearchQuery = MutableStateFlow("")
    private val _selectedMaterialForAction = MutableStateFlow<RawMaterial?>(null)
    private val _selectedMaterialForBatches = MutableStateFlow<RawMaterial?>(null)
    private val _isBatchDetailOpen = MutableStateFlow(false)
    private val _selectedBatchForWaste = MutableStateFlow<com.kapos.mobile.domain.model.MaterialBatch?>(null)
    private val _isWasteDialogOpen = MutableStateFlow(false)
    private val _isAddEditMaterialOpen = MutableStateFlow(false)
    private val _materialToEdit = MutableStateFlow<RawMaterial?>(null)
    private val _isRestockMaterialOpen = MutableStateFlow(false)
    private val _isWasteMaterialOpen = MutableStateFlow(false)
    private val _isOpnameMaterialOpen = MutableStateFlow(false)

    // Stock Movements Audit Log
    private val _stockMovements = MutableStateFlow<List<com.kapos.mobile.domain.model.StockMovement>>(emptyList())
    private val _movementFilterType = MutableStateFlow<com.kapos.mobile.domain.model.StockMovementType?>(null)

    init {
        posRepository?.let { repo ->
            scope.launch {
                repo.observeOrders().collect { orders ->
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val todayStart = cal.timeInMillis

                    val paidTodayOrders = orders.filter { it.createdAt >= todayStart && it.paymentStatus == PaymentStatus.PAID }
                    var totalTaichanTusuk = 0
                    val chickenSoldMap = mutableMapOf<ChickenPart, Int>()
                    ChickenPart.values().forEach { chickenSoldMap[it] = 0 }

                    for (order in paidTodayOrders) {
                        val items = repo.getOrderItems(order.id)
                        for (item in items) {
                            val nameLower = item.productName.lowercase()
                            val idLower = item.productId.lowercase()
                            val isTaichan = nameLower.contains("taichan") || idLower.contains("taichan")

                            if (isTaichan) {
                                totalTaichanTusuk += item.quantity * 10
                            }

                            // Check chicken part sales
                            when {
                                nameLower.contains("sayap") || idLower.contains("sayap") -> {
                                    chickenSoldMap[ChickenPart.SAYAP] = (chickenSoldMap[ChickenPart.SAYAP] ?: 0) + item.quantity
                                }
                                nameLower.contains("paha atas") || idLower.contains("paha_atas") -> {
                                    chickenSoldMap[ChickenPart.PAHA_ATAS] = (chickenSoldMap[ChickenPart.PAHA_ATAS] ?: 0) + item.quantity
                                }
                                nameLower.contains("paha bawah") || idLower.contains("paha_bawah") -> {
                                    chickenSoldMap[ChickenPart.PAHA_BAWAH] = (chickenSoldMap[ChickenPart.PAHA_BAWAH] ?: 0) + item.quantity
                                }
                                nameLower.contains("dada") || idLower.contains("dada") -> {
                                    chickenSoldMap[ChickenPart.DADA] = (chickenSoldMap[ChickenPart.DADA] ?: 0) + item.quantity
                                }
                                nameLower.contains("ayam") || idLower.contains("chicken") || idLower.contains("ay_") -> {
                                    if (!isTaichan) {
                                        val notesLower = (item.notes ?: "").lowercase()
                                        when {
                                            notesLower.contains("sayap") -> chickenSoldMap[ChickenPart.SAYAP] = (chickenSoldMap[ChickenPart.SAYAP] ?: 0) + item.quantity
                                            notesLower.contains("paha atas") -> chickenSoldMap[ChickenPart.PAHA_ATAS] = (chickenSoldMap[ChickenPart.PAHA_ATAS] ?: 0) + item.quantity
                                            notesLower.contains("paha") -> chickenSoldMap[ChickenPart.PAHA_BAWAH] = (chickenSoldMap[ChickenPart.PAHA_BAWAH] ?: 0) + item.quantity
                                            notesLower.contains("dada") -> chickenSoldMap[ChickenPart.DADA] = (chickenSoldMap[ChickenPart.DADA] ?: 0) + item.quantity
                                            else -> {
                                                chickenSoldMap[ChickenPart.SAYAP] = (chickenSoldMap[ChickenPart.SAYAP] ?: 0) + item.quantity
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    _taichanSoldToday.value = totalTaichanTusuk
                    _chickenSoldToday.value = chickenSoldMap
                }
            }
        }

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
        combine(
            _activeTab,
            _isRestockDialogOpen,
            _restockChickenPart,
            _taichanSoldToday,
            _chickenSoldToday
        ) { tab, dialog, part, soldToday, ckSold ->
            listOf(tab, dialog, part, soldToday, ckSold)
        },
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
        combine(
            stockRepository.taichanBatches,
            stockRepository.chickenBatches,
            _isWasteDialogOpen
        ) { tcBatches, ckBatches, isWasteDialog ->
            Triple(tcBatches, ckBatches, isWasteDialog)
        }
    ) { baseList, matList, dialogList, batchDialogList, repoData ->
        val tab = baseList[0] as StockTab
        val isDialogOpen = baseList[1] as Boolean
        val chickenPart = baseList[2] as ChickenPart
        val soldToday = baseList[3] as Int
        @Suppress("UNCHECKED_CAST")
        val chickenSoldMap = baseList[4] as Map<ChickenPart, Int>

        @Suppress("UNCHECKED_CAST")
        val mats = matList[0] as List<RawMaterial>
        @Suppress("UNCHECKED_CAST")
        val batchesMap = matList[1] as Map<String, List<com.kapos.mobile.domain.model.MaterialBatch>>
        val cat = matList[2] as RawMaterialCategory?
        val query = matList[3] as String
        val selectedMat = matList[4] as RawMaterial?

        val addEdit = dialogList[0] as Boolean
        val toEdit = dialogList[1] as RawMaterial?
        val restock = dialogList[2] as Boolean
        val waste = dialogList[3] as Boolean
        val opname = dialogList[4] as Boolean

        @Suppress("UNCHECKED_CAST")
        val movements = batchDialogList[0] as List<com.kapos.mobile.domain.model.StockMovement>
        val movementFilter = batchDialogList[1] as com.kapos.mobile.domain.model.StockMovementType?
        val matBatches = batchDialogList[2] as RawMaterial?
        val isBatchOpen = batchDialogList[3] as Boolean
        val wasteBatch = batchDialogList[4] as com.kapos.mobile.domain.model.MaterialBatch?

        val tcBatches = repoData.first
        val ckBatches = repoData.second
        val isWasteDialog = repoData.third

        StockUiState(
            activeTab = tab,
            taichanBatches = tcBatches,
            chickenBatches = ckBatches,
            taichanSoldToday = soldToday,
            chickenSoldToday = chickenSoldMap,
            isRestockDialogOpen = isDialogOpen,
            restockChickenPart = chickenPart,
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
        initialValue = StockUiState(
            taichanBatches = stockRepository.taichanBatches.value,
            chickenBatches = stockRepository.chickenBatches.value
        )
    )

    fun onTabSelect(tab: StockTab) {
        _activeTab.value = tab
    }

    // --- Taichan & Chicken Batches ---
    fun openRestockDialog(part: ChickenPart = ChickenPart.SAYAP) {
        _restockChickenPart.value = part
        _isRestockDialogOpen.value = true
    }

    fun closeRestockDialog() {
        _isRestockDialogOpen.value = false
    }

    fun addTaichanBatch(raw: Int, notes: String = "") {
        stockRepository.addTaichanBatch(raw, notes)
        closeRestockDialog()
    }

    fun updateTaichanFried(batchId: String, fried: Int) {
        stockRepository.updateTaichanFried(batchId, fried)
    }

    fun deleteTaichanBatch(batchId: String) {
        stockRepository.deleteTaichanBatch(batchId)
    }

    fun addChickenBatch(part: ChickenPart, raw: Int, notes: String = "") {
        stockRepository.addChickenBatch(part, raw, notes)
        closeRestockDialog()
    }

    fun updateChickenFried(batchId: String, fried: Int) {
        stockRepository.updateChickenFried(batchId, fried)
    }

    fun deleteChickenBatch(batchId: String) {
        stockRepository.deleteChickenBatch(batchId)
    }

    fun setTaichanSoldTodayForTest(sold: Int) {
        _taichanSoldToday.value = sold
    }

    fun setChickenSoldTodayForTest(map: Map<ChickenPart, Int>) {
        _chickenSoldToday.value = map
    }

    fun onSelectMovementFilter(type: com.kapos.mobile.domain.model.StockMovementType?) {
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
                    currentStock = currentStock.coerceAtLeast(0.0),
                    minStock = minStock.coerceAtLeast(0.0),
                    costPerUnit = costPerUnit.coerceAtLeast(0.0),
                    lastRestockedAt = System.currentTimeMillis()
                )
            }
            inventoryRepository?.saveRawMaterial(material)
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
        notes: String
    ) {
        viewModelScope.launch {
            inventoryRepository?.restockMaterialWithBatch(materialId, quantity, costTotal, expiryDate, notes)
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

    fun openWasteForBatch(batch: com.kapos.mobile.domain.model.MaterialBatch) {
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
