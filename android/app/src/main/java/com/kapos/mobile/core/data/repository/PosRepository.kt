package com.kapos.mobile.core.data.repository

import com.kapos.mobile.core.data.local.AppDatabase
import com.kapos.mobile.core.data.local.entity.CategoryEntity
import com.kapos.mobile.core.data.local.entity.OrderEntity
import com.kapos.mobile.core.data.local.entity.OrderItemEntity
import com.kapos.mobile.core.data.local.entity.ProductEntity
import com.kapos.mobile.core.data.remote.SupabaseClient
import com.kapos.mobile.core.data.remote.dto.SupabaseCategoryDto
import com.kapos.mobile.core.data.remote.dto.SupabaseOrderItemPayload
import com.kapos.mobile.core.data.remote.dto.SupabaseOrderPayload
import com.kapos.mobile.core.data.remote.dto.SupabaseProductDto
import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.Category
import com.kapos.mobile.domain.model.Order
import com.kapos.mobile.domain.model.OrderItem
import com.kapos.mobile.domain.model.OrderType
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.PaymentStatus
import com.kapos.mobile.domain.model.Product
import com.kapos.mobile.domain.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kapos.mobile.core.data.local.entity.ModifierEntity
import com.kapos.mobile.domain.model.ModifierOption
import com.kapos.mobile.core.data.repository.CustomerRepository
import java.util.UUID

class PosRepository(
    private val database: AppDatabase? = null,
    private val supabaseClient: SupabaseClient,
    private val inventoryRepository: InventoryRepository? = null,
    private val customerRepository: CustomerRepository? = null,
    private val settingsRepository: com.kapos.mobile.core.data.repository.SettingsRepository? = null,
    private val shiftRepository: ShiftRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val categoryDao = database?.categoryDao()
    private val productDao = database?.productDao()
    private val orderDao = database?.orderDao()
    private val modifierDao = database?.modifierDao()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    init {
        scope.launch {
            syncAll()
        }
        // Auto-sync periodik selama app aktif
        scope.launch {
            startPeriodicSync()
        }
    }

    /**
     * Loop sinkronisasi otomatis di background.
     * Membaca interval dari settings setiap iterasi agar langsung berlaku saat setting diubah.
     */
    private suspend fun startPeriodicSync() {
        while (true) {
            val intervalMinutes = settingsRepository?.settingsFlow?.value?.autoSyncIntervalMinutes ?: 5
            // Interval 0 = nonaktif
            if (intervalMinutes > 0) {
                delay(intervalMinutes * 60 * 1000L)
                if (supabaseClient.isConfigured()) {
                    syncAll()
                }
            } else {
                // Jika nonaktif, cukup periksa setiap 1 menit apakah diaktifkan kembali
                delay(60 * 1000L)
            }
        }
    }

    fun observeCategories(): Flow<List<Category>> {
        return categoryDao?.observeAll()?.map { list ->
            list.map { it.toDomain() }
        } ?: flowOf(emptyList())
    }

    fun observeAllCategories(includeInactive: Boolean = true): Flow<List<Category>> {
        val flow = if (includeInactive) categoryDao?.observeAllIncludeInactive() else categoryDao?.observeAll()
        return flow?.map { list -> list.map { it.toDomain() } } ?: flowOf(emptyList())
    }

    suspend fun saveCategory(category: Category) = withContext(Dispatchers.IO) {
        val id = if (category.id.isBlank()) UUID.randomUUID().toString() else category.id
        categoryDao?.upsert(
            CategoryEntity(
                id = id,
                name = category.name.trim(),
                emoji = category.emoji.ifBlank { "🍽️" },
                color = category.color.ifBlank { "#0B5C73" },
                sortOrder = category.sortOrder,
                isActive = category.isActive
            )
        )
        scope.launch { syncMenu() }
    }

    suspend fun deleteCategory(id: String) = withContext(Dispatchers.IO) {
        categoryDao?.deleteById(id)
        if (supabaseClient.isConfigured()) {
            supabaseClient.deleteCategory(id)
        }
    }

    suspend fun toggleCategoryActive(id: String, isActive: Boolean) = withContext(Dispatchers.IO) {
        categoryDao?.toggleActive(id, isActive)
        scope.launch { syncMenu() }
    }

    fun observeProducts(): Flow<List<Product>> {
        return productDao?.observeAll()?.map { list ->
            list.map { it.toDomain() }
        } ?: flowOf(emptyList())
    }

    fun observeAllProducts(includeInactive: Boolean = true): Flow<List<Product>> {
        val flow = if (includeInactive) productDao?.observeAllIncludeInactive() else productDao?.observeAll()
        return flow?.map { list -> list.map { it.toDomain() } } ?: flowOf(emptyList())
    }

    suspend fun saveProduct(product: Product) = withContext(Dispatchers.IO) {
        val id = if (product.id.isBlank()) UUID.randomUUID().toString() else product.id
        productDao?.upsert(
            ProductEntity(
                id = id,
                categoryId = product.categoryId,
                name = product.name.trim(),
                price = product.price.coerceAtLeast(0.0),
                costPrice = product.costPrice.coerceAtLeast(0.0),
                emoji = product.emoji.ifBlank { "🍢" },
                spiceLevel = product.spiceLevel,
                description = product.description.trim(),
                isActive = product.isActive,
                allowSpiceLevel = product.allowSpiceLevel,
                allowSambal = product.allowSambal,
                allowComplimentary = product.allowComplimentary,
                allowToppings = product.allowToppings
            )
        )
        scope.launch { syncMenu() }
    }

    suspend fun deleteProduct(id: String) = withContext(Dispatchers.IO) {
        productDao?.deleteById(id)
        if (supabaseClient.isConfigured()) {
            supabaseClient.deleteProduct(id)
        }
    }

    suspend fun toggleProductActive(id: String, isActive: Boolean) = withContext(Dispatchers.IO) {
        productDao?.toggleActive(id, isActive)
        scope.launch { syncMenu() }
    }

    fun observeAllModifiers(): Flow<List<ModifierOption>> {
        return modifierDao?.observeAll()?.map { list -> list.map { it.toDomain() } } ?: flowOf(emptyList())
    }

    fun observeActiveModifiers(): Flow<List<ModifierOption>> {
        return modifierDao?.observeActive()?.map { list -> list.map { it.toDomain() } } ?: flowOf(emptyList())
    }

    suspend fun saveModifier(modifier: ModifierOption) = withContext(Dispatchers.IO) {
        val id = if (modifier.id.isBlank()) UUID.randomUUID().toString() else modifier.id
        modifierDao?.upsert(ModifierEntity.fromDomain(modifier.copy(id = id)))
        scope.launch { syncMenu() }
    }

    suspend fun deleteModifier(id: String) = withContext(Dispatchers.IO) {
        modifierDao?.deleteById(id)
        if (supabaseClient.isConfigured()) {
            supabaseClient.deleteModifier(id)
        }
    }

    suspend fun toggleModifierActive(id: String, isActive: Boolean) = withContext(Dispatchers.IO) {
        modifierDao?.toggleActive(id, isActive)
        scope.launch { syncMenu() }
    }


    private fun CategoryEntity.toDomain() = Category(
        id = id,
        name = name,
        emoji = emoji,
        color = color,
        sortOrder = sortOrder,
        isActive = isActive
    )

    private fun ProductEntity.toDomain() = Product(
        id = id,
        categoryId = categoryId,
        name = name,
        price = price,
        costPrice = costPrice,
        emoji = emoji,
        spiceLevel = spiceLevel,
        description = description,
        isActive = isActive,
        allowSpiceLevel = allowSpiceLevel,
        allowSambal = allowSambal,
        allowComplimentary = allowComplimentary,
        allowToppings = allowToppings
    )

    fun observeOrders(): Flow<List<Order>> {
        return orderDao?.observeOrders()?.map { list ->
            list.map { entity ->
                Order(
                    id = entity.id,
                    orderNo = entity.orderNo,
                    customerName = entity.customerName,
                    customerPhone = entity.customerPhone,
                    orderType = try { OrderType.valueOf(entity.orderType) } catch (e: Exception) { OrderType.DINE_IN },
                    tableNo = entity.tableNo,
                    subtotal = entity.subtotal,
                    discount = entity.discount,
                    total = entity.total,
                    paymentMethod = try { PaymentMethod.valueOf(entity.paymentMethod) } catch (e: Exception) { PaymentMethod.CASH },
                    paymentStatus = try { PaymentStatus.valueOf(entity.paymentStatus) } catch (e: Exception) { PaymentStatus.PAID },
                    cashierName = entity.cashierName,
                    notes = entity.notes,
                    createdAt = entity.createdAt,
                    paidAt = entity.paidAt,
                    syncStatus = try { SyncStatus.valueOf(entity.syncStatus) } catch (e: Exception) { SyncStatus.PENDING_SYNC },
                    voidReason = entity.voidReason,
                    voidedAt = entity.voidedAt
                )
            }
        } ?: flowOf(emptyList())
    }

    suspend fun getOrderItems(orderId: String): List<OrderItem> = withContext(Dispatchers.IO) {
        orderDao?.getItemsForOrder(orderId)?.map {
            OrderItem(
                id = it.id,
                orderId = it.orderId,
                productId = it.productId,
                productName = it.productName,
                price = it.price,
                quantity = it.quantity,
                spiceLevel = it.spiceLevel,
                notes = it.notes,
                subtotal = it.subtotal
            )
        } ?: emptyList()
    }

    fun observeAllOrderItems(): Flow<List<OrderItem>> {
        return orderDao?.observeAllOrderItems()?.map { list ->
            list.map {
                OrderItem(
                    id = it.id,
                    orderId = it.orderId,
                    productId = it.productId,
                    productName = it.productName,
                    price = it.price,
                    quantity = it.quantity,
                    spiceLevel = it.spiceLevel,
                    notes = it.notes,
                    subtotal = it.subtotal
                )
            }
        } ?: flowOf(emptyList())
    }

    suspend fun submitOrder(
        order: Order,
        items: List<CartItem>
    ): Order = withContext(Dispatchers.IO) {
        val orderEntity = OrderEntity(
            id = order.id,
            orderNo = order.orderNo,
            customerName = order.customerName,
            customerPhone = order.customerPhone,
            orderType = order.orderType.name,
            tableNo = order.tableNo,
            subtotal = order.subtotal,
            discount = order.discount,
            total = order.total,
            paymentMethod = order.paymentMethod.name,
            paymentStatus = order.paymentStatus.name,
            cashierName = order.cashierName,
            notes = order.notes,
            createdAt = order.createdAt,
            paidAt = order.paidAt,
            syncStatus = SyncStatus.PENDING_SYNC.name
        )

        val itemEntities = items.map { cart ->
            OrderItemEntity(
                id = UUID.randomUUID().toString(),
                orderId = order.id,
                productId = cart.product.id,
                productName = cart.product.name,
                price = cart.product.price,
                quantity = cart.quantity,
                spiceLevel = cart.spiceLevel,
                notes = cart.notes,
                subtotal = cart.subtotal
            )
        }

        // 1. Instantly write to Room (SSOT)
        database?.orderDao()?.insertCompleteOrder(orderEntity, itemEntities)

        // 2. Automatically deduct raw material stock via BOM
        inventoryRepository?.deductStockForOrderItems(items)

        // 3. Trigger asynchronous background push to Supabase
        scope.launch {
            syncPendingOrders()
        }

        order
    }

    // Legacy alias for backward compatibility
    suspend fun refreshFromSupabase() = syncAll()

    /**
     * Sinkronisasi menyeluruh 2 arah (Katalog Menu + Transaksi).
     * Dipanggil saat startup, periodik background, dan tombol Sync Now.
     */
    suspend fun syncAll(): Boolean = withContext(Dispatchers.IO) {
        if (!supabaseClient.isConfigured()) return@withContext false
        _isSyncing.value = true
        var success = false
        try {
            val isConnected = supabaseClient.testConnection()
            _isOnline.value = isConnected
            if (isConnected) {
                val menuOk = syncMenuInternal()
                val transOk = syncTransactionsInternal()
                shiftRepository?.syncPendingShifts()
                success = menuOk && transOk
                if (success) _lastSyncTime.value = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            _isOnline.value = false
        } finally {
            _isSyncing.value = false
        }
        success
    }

    /** Sinkronisasi 2 arah Katalog Menu (Kategori & Produk). Bisa dipanggil mandiri. */
    suspend fun syncMenu(): Boolean = withContext(Dispatchers.IO) {
        if (!supabaseClient.isConfigured()) return@withContext false
        _isSyncing.value = true
        try {
            syncMenuInternal()
        } catch (e: Exception) {
            false
        } finally {
            _isSyncing.value = false
        }
    }

    /** Sinkronisasi 2 arah Transaksi (Orders & Order Items). Bisa dipanggil mandiri. */
    suspend fun syncTransactions(): Boolean = withContext(Dispatchers.IO) {
        if (!supabaseClient.isConfigured()) return@withContext false
        _isSyncing.value = true
        try {
            syncTransactionsInternal()
        } catch (e: Exception) {
            false
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun syncMenuInternal(): Boolean {
        // --- PULL categories ---
        val remoteCats = supabaseClient.fetchCategories()
        if (remoteCats.isNotEmpty()) {
            categoryDao?.upsertAll(remoteCats.map {
                CategoryEntity(
                    id = it.id,
                    name = it.name,
                    emoji = it.emoji ?: "🍽️",
                    color = it.color ?: "#0B5C73",
                    sortOrder = it.sortOrder ?: 0,
                    isActive = it.isActive ?: true
                )
            })
        }
        // --- PUSH local categories ---
        val localCats = categoryDao?.getAllIncludeInactive() ?: emptyList()
        if (localCats.isNotEmpty()) {
            supabaseClient.upsertCategories(localCats.map {
                SupabaseCategoryDto(
                    id = it.id,
                    name = it.name,
                    emoji = it.emoji,
                    color = it.color,
                    sortOrder = it.sortOrder,
                    isActive = it.isActive
                )
            })
        }
        // --- PULL products ---
        val remoteProds = supabaseClient.fetchProducts()
        if (remoteProds.isNotEmpty()) {
            productDao?.upsertAll(remoteProds.map {
                ProductEntity(
                    id = it.id,
                    categoryId = it.categoryId,
                    name = it.name,
                    price = it.price,
                    costPrice = it.costPrice ?: 0.0,
                    emoji = it.emoji ?: "🍢",
                    spiceLevel = it.spiceLevel ?: "",
                    description = it.description ?: "",
                    isActive = it.isActive ?: true,
                    allowSpiceLevel = it.allowSpiceLevel ?: false,
                    allowSambal = it.allowSambal ?: false,
                    allowComplimentary = it.allowComplimentary ?: false,
                    allowToppings = it.allowToppings ?: false
                )
            })
        }
        // --- PUSH local products ---
        val localProds = productDao?.getAllIncludeInactive() ?: emptyList()
        if (localProds.isNotEmpty()) {
            supabaseClient.upsertProducts(localProds.map {
                SupabaseProductDto(
                    id = it.id,
                    categoryId = it.categoryId,
                    name = it.name,
                    price = it.price,
                    costPrice = it.costPrice,
                    emoji = it.emoji,
                    spiceLevel = it.spiceLevel,
                    description = it.description,
                    isActive = it.isActive,
                    allowSpiceLevel = it.allowSpiceLevel,
                    allowSambal = it.allowSambal,
                    allowComplimentary = it.allowComplimentary,
                    allowToppings = it.allowToppings
                )
            })
        }
        // --- PULL modifiers ---
        val remoteMods = supabaseClient.fetchModifiers()
        if (remoteMods.isNotEmpty()) {
            modifierDao?.upsertAll(remoteMods.map {
                ModifierEntity(
                    id = it.id,
                    name = it.name,
                    price = it.price,
                    category = it.category,
                    isDefault = it.isDefault ?: false,
                    isActive = it.isActive ?: true,
                    sortOrder = it.sortOrder ?: 0
                )
            })
        }
        // --- PUSH local modifiers ---
        val localMods = modifierDao?.getAll() ?: emptyList()
        if (localMods.isNotEmpty()) {
            supabaseClient.upsertModifiers(localMods.map {
                com.kapos.mobile.core.data.remote.dto.SupabaseModifierDto(
                    id = it.id,
                    name = it.name,
                    price = it.price,
                    category = it.category,
                    isDefault = it.isDefault,
                    isActive = it.isActive,
                    sortOrder = it.sortOrder
                )
            })
        }
        return true
    }


    private suspend fun syncTransactionsInternal(): Boolean {
        // --- PUSH pending local orders ---
        syncPendingOrders()
        // --- PULL remote orders not yet in local DB ---
        val remoteOrders = supabaseClient.fetchOrders()
        if (remoteOrders.isNotEmpty()) {
            val localIds = orderDao?.getAll()?.map { it.id }?.toSet() ?: emptySet()
            val newOrders = remoteOrders.filter { it.id !in localIds }
            if (newOrders.isNotEmpty()) {
                val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                for (remote in newOrders) {
                    val createdAt = remote.createdAt?.let {
                        try { isoFormat.parse(it.take(19))?.time } catch (e: Exception) { System.currentTimeMillis() }
                    } ?: System.currentTimeMillis()
                    val paidAt = remote.paidAt?.let {
                        try { isoFormat.parse(it.take(19))?.time } catch (e: Exception) { null }
                    }
                    val orderEntity = OrderEntity(
                        id = remote.id,
                        orderNo = remote.orderNo,
                        customerName = remote.customerName,
                        customerPhone = remote.customerPhone,
                        orderType = remote.orderType.uppercase().replace('-', '_'),
                        tableNo = remote.tableNo,
                        subtotal = remote.subtotal,
                        discount = remote.discount,
                        total = remote.total,
                        paymentMethod = remote.paymentMethod.uppercase(),
                        paymentStatus = if (remote.status == "void") "VOID" else "PAID",
                        cashierName = remote.cashierId ?: "Kasir",
                        notes = remote.notes,
                        voidReason = remote.voidReason,
                        createdAt = createdAt,
                        paidAt = paidAt,
                        syncStatus = SyncStatus.SYNCED.name
                    )
                    orderDao?.insertOrder(orderEntity)
                    // Pull items for this order
                    val remoteItems = supabaseClient.fetchOrderItems(listOf(remote.id))
                    if (remoteItems.isNotEmpty()) {
                        orderDao?.insertOrderItems(remoteItems.map {
                            OrderItemEntity(
                                id = it.id,
                                orderId = it.orderId,
                                productId = it.productId,
                                productName = it.productName,
                                price = it.price,
                                quantity = it.quantity,
                                spiceLevel = it.spiceLevel ?: "",
                                notes = it.notes ?: "",
                                subtotal = it.subtotal
                            )
                        })
                    }
                }
            }
        }
        return true
    }

    suspend fun syncPendingOrders() = withContext(Dispatchers.IO) {
        if (!supabaseClient.isConfigured()) return@withContext
        val pending = orderDao?.getPendingOrders() ?: return@withContext
        if (pending.isEmpty()) return@withContext

        _isSyncing.value = true
        try {
            val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            for (order in pending) {
                val items = orderDao.getItemsForOrder(order.id)
                val createdAtIso = isoFormat.format(java.util.Date(order.createdAt))
                val paidAtIso = order.paidAt?.let { isoFormat.format(java.util.Date(it)) } ?: createdAtIso

                val orderPayload = SupabaseOrderPayload(
                    id = order.id,
                    orderNo = order.orderNo,
                    customerName = order.customerName,
                    customerPhone = order.customerPhone,
                    orderType = order.orderType.lowercase().replace('_', '-'),
                    tableNo = order.tableNo,
                    subtotal = order.subtotal,
                    discount = order.discount,
                    total = order.total,
                    paymentMethod = order.paymentMethod.lowercase(),
                    status = if (order.paymentStatus == "VOID") "void" else "completed",
                    cashierId = order.cashierName,
                    notes = order.notes,
                    voidReason = order.voidReason,
                    createdAt = createdAtIso,
                    paidAt = paidAtIso
                )
                val itemsPayload = items.map {
                    SupabaseOrderItemPayload(
                        id = it.id,
                        orderId = it.orderId,
                        productId = it.productId,
                        productName = it.productName,
                        price = it.price,
                        quantity = it.quantity,
                        spiceLevel = it.spiceLevel,
                        notes = it.notes,
                        subtotal = it.subtotal
                    )
                }

                val orderSuccess = supabaseClient.postOrder(orderPayload)
                val itemsSuccess = supabaseClient.postOrderItems(itemsPayload)

                if (orderSuccess && itemsSuccess) {
                    orderDao.updateSyncStatus(order.id, SyncStatus.SYNCED.name)
                }
            }
            _isOnline.value = true
        } catch (e: Exception) {
            _isOnline.value = false
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun voidOrder(orderId: String, reason: String = "Dibatalkan Kasir") = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val orderEntity = orderDao?.getOrderById(orderId)

        // 1. Restore BOM stock for order items
        val itemEntities = orderDao?.getItemsForOrder(orderId) ?: emptyList()
        val domainItems = itemEntities.map {
            OrderItem(
                id = it.id,
                orderId = it.orderId,
                productId = it.productId,
                productName = it.productName,
                price = it.price,
                quantity = it.quantity,
                spiceLevel = it.spiceLevel,
                notes = it.notes,
                subtotal = it.subtotal
            )
        }
        inventoryRepository?.restoreStockForOrderItems(domainItems)

        // 2. Reverse customer loyalty points and spend if member order
        if (orderEntity != null) {
            val phone = orderEntity.customerPhone
            if (!phone.isNullOrBlank()) {
                val customer = customerRepository?.getCustomerByPhone(phone)
                if (customer != null) {
                    val pointsEarned = (orderEntity.total / 10000.0).toInt().coerceAtLeast(0)
                    customerRepository?.reverseCustomerTransaction(
                        customerId = customer.id,
                        totalSpent = orderEntity.total,
                        pointsRedeemed = 0,
                        pointsEarned = pointsEarned
                    )
                }
            }
        }

        // 3. Mark order as VOID in local DB
        orderDao?.voidOrderWithReason(orderId, reason, now)

        // 4. Trigger cloud push to sync VOID status to Supabase
        scope.launch { syncPendingOrders() }
    }
}

