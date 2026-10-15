package com.kapos.mobile.core.data.repository

import com.kapos.mobile.core.data.local.AppDatabase
import com.kapos.mobile.core.data.local.entity.CategoryEntity
import com.kapos.mobile.core.data.local.entity.OrderEntity
import com.kapos.mobile.core.data.local.entity.OrderItemEntity
import com.kapos.mobile.core.data.local.entity.ProductEntity
import com.kapos.mobile.core.data.mock.SampleData
import com.kapos.mobile.core.data.remote.SupabaseClient
import com.kapos.mobile.core.data.remote.dto.SupabaseOrderItemPayload
import com.kapos.mobile.core.data.remote.dto.SupabaseOrderPayload
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class PosRepository(
    private val database: AppDatabase,
    private val supabaseClient: SupabaseClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val categoryDao = database.categoryDao()
    private val productDao = database.productDao()
    private val orderDao = database.orderDao()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        // Seed default sample data if local database is empty
        scope.launch {
            seedInitialDataIfEmpty()
            refreshFromSupabase()
        }
    }

    private suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingCats = categoryDao.getAll()
        if (existingCats.isEmpty()) {
            val catEntities = SampleData.categories.map {
                CategoryEntity(
                    id = it.id,
                    name = it.name,
                    emoji = it.emoji,
                    color = it.color,
                    sortOrder = it.sortOrder,
                    isActive = it.isActive
                )
            }
            categoryDao.upsertAll(catEntities)
        }

        val existingProds = productDao.getAll()
        if (existingProds.isEmpty()) {
            val prodEntities = SampleData.products.map {
                ProductEntity(
                    id = it.id,
                    categoryId = it.categoryId,
                    name = it.name,
                    price = it.price,
                    costPrice = it.costPrice,
                    emoji = it.emoji,
                    spiceLevel = it.spiceLevel,
                    description = it.description,
                    isActive = it.isActive
                )
            }
            productDao.upsertAll(prodEntities)
        }
    }

    fun observeCategories(): Flow<List<Category>> {
        return categoryDao.observeAll().map { list ->
            list.map {
                Category(
                    id = it.id,
                    name = it.name,
                    emoji = it.emoji,
                    color = it.color,
                    sortOrder = it.sortOrder,
                    isActive = it.isActive
                )
            }
        }
    }

    fun observeProducts(): Flow<List<Product>> {
        return productDao.observeAll().map { list ->
            list.map {
                Product(
                    id = it.id,
                    categoryId = it.categoryId,
                    name = it.name,
                    price = it.price,
                    costPrice = it.costPrice,
                    emoji = it.emoji,
                    spiceLevel = it.spiceLevel,
                    description = it.description,
                    isActive = it.isActive
                )
            }
        }
    }

    fun observeOrders(): Flow<List<Order>> {
        return orderDao.observeOrders().map { list ->
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
                    syncStatus = try { SyncStatus.valueOf(entity.syncStatus) } catch (e: Exception) { SyncStatus.PENDING_SYNC }
                )
            }
        }
    }

    suspend fun getOrderItems(orderId: String): List<OrderItem> = withContext(Dispatchers.IO) {
        orderDao.getItemsForOrder(orderId).map {
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
        database.orderDao().insertCompleteOrder(orderEntity, itemEntities)

        // 2. Trigger asynchronous background push to Supabase
        scope.launch {
            syncPendingOrders()
        }

        order
    }

    suspend fun refreshFromSupabase() = withContext(Dispatchers.IO) {
        if (!supabaseClient.isConfigured()) return@withContext
        _isSyncing.value = true
        try {
            val isConnected = supabaseClient.testConnection()
            _isOnline.value = isConnected
            if (isConnected) {
                val remoteCats = supabaseClient.fetchCategories()
                if (remoteCats.isNotEmpty()) {
                    categoryDao.upsertAll(remoteCats.map {
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

                val remoteProds = supabaseClient.fetchProducts()
                if (remoteProds.isNotEmpty()) {
                    productDao.upsertAll(remoteProds.map {
                        ProductEntity(
                            id = it.id,
                            categoryId = it.categoryId,
                            name = it.name,
                            price = it.price,
                            costPrice = it.costPrice ?: 0.0,
                            emoji = it.emoji ?: "🍢",
                            spiceLevel = it.spiceLevel ?: "",
                            description = it.description ?: "",
                            isActive = it.isActive ?: true
                        )
                    })
                }

                // Push any pending orders
                syncPendingOrders()
            }
        } catch (e: Exception) {
            _isOnline.value = false
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun syncPendingOrders() = withContext(Dispatchers.IO) {
        if (!supabaseClient.isConfigured()) return@withContext
        val pending = orderDao.getPendingOrders()
        if (pending.isEmpty()) return@withContext

        _isSyncing.value = true
        try {
            for (order in pending) {
                val items = orderDao.getItemsForOrder(order.id)
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
                    status = "completed",
                    cashierId = order.cashierName,
                    notes = order.notes
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
}
