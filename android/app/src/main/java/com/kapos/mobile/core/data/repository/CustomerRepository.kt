package com.kapos.mobile.core.data.repository

import com.kapos.mobile.core.data.local.AppDatabase
import com.kapos.mobile.core.data.local.entity.CustomerEntity
import com.kapos.mobile.domain.model.Customer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

import com.kapos.mobile.core.data.remote.SupabaseClient
import com.kapos.mobile.core.data.remote.dto.SupabaseCustomerDto

class CustomerRepository(
    private val database: AppDatabase,
    private val supabaseClient: SupabaseClient? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val customerDao = database.customerDao()

    fun observeCustomers(): Flow<List<Customer>> {
        return customerDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun searchCustomers(query: String): Flow<List<Customer>> {
        return if (query.isBlank()) {
            observeCustomers()
        } else {
            customerDao.search(query.trim()).map { list ->
                list.map { it.toDomain() }
            }
        }
    }

    suspend fun getCustomerById(id: String): Customer? = withContext(Dispatchers.IO) {
        customerDao.getById(id)?.toDomain()
    }

    suspend fun getCustomerByPhone(phone: String): Customer? = withContext(Dispatchers.IO) {
        customerDao.getByPhone(phone.trim())?.toDomain()
    }

    suspend fun saveCustomer(customer: Customer): Customer = withContext(Dispatchers.IO) {
        val id = if (customer.id.isBlank()) UUID.randomUUID().toString() else customer.id
        val entity = CustomerEntity(
            id = id,
            name = customer.name.trim(),
            phone = customer.phone.trim(),
            points = customer.points.coerceAtLeast(0),
            totalSpend = customer.totalSpend.coerceAtLeast(0.0),
            orderCount = customer.orderCount.coerceAtLeast(0),
            createdAt = customer.createdAt
        )
        customerDao.upsert(entity)

        if (supabaseClient?.isConfigured() == true) {
            scope.launch {
                supabaseClient.upsertCustomers(listOf(
                    SupabaseCustomerDto(
                        id = entity.id,
                        name = entity.name,
                        phone = entity.phone,
                        points = entity.points,
                        totalSpent = entity.totalSpend,
                        visitCount = entity.orderCount
                    )
                ))
            }
        }

        entity.toDomain()
    }

    suspend fun deleteCustomer(id: String) = withContext(Dispatchers.IO) {
        customerDao.deleteById(id)
        if (supabaseClient?.isConfigured() == true) {
            scope.launch {
                supabaseClient.deleteCustomer(id)
            }
        }
    }

    suspend fun recordCustomerTransaction(
        customerId: String,
        totalSpent: Double,
        pointsRedeemed: Int,
        pointsEarned: Int
    ) = withContext(Dispatchers.IO) {
        val pointsDelta = pointsEarned - pointsRedeemed
        customerDao.updateStats(
            id = customerId,
            pointsDelta = pointsDelta,
            spendDelta = totalSpent,
            orderDelta = 1
        )
        syncUpdatedCustomer(customerId)
    }

    suspend fun reverseCustomerTransaction(
        customerId: String,
        totalSpent: Double,
        pointsRedeemed: Int,
        pointsEarned: Int
    ) = withContext(Dispatchers.IO) {
        val pointsDelta = pointsRedeemed - pointsEarned // restore redeemed, revoke earned
        customerDao.updateStats(
            id = customerId,
            pointsDelta = pointsDelta,
            spendDelta = -totalSpent,
            orderDelta = -1
        )
        syncUpdatedCustomer(customerId)
    }

    private suspend fun syncUpdatedCustomer(customerId: String) {
        if (supabaseClient?.isConfigured() == true) {
            val updated = customerDao.getById(customerId) ?: return
            scope.launch {
                supabaseClient.upsertCustomers(listOf(
                    SupabaseCustomerDto(
                        id = updated.id,
                        name = updated.name,
                        phone = updated.phone,
                        points = updated.points,
                        totalSpent = updated.totalSpend,
                        visitCount = updated.orderCount
                    )
                ))
            }
        }
    }

    /**
     * Sinkronisasi 2 arah Pelanggan dengan Supabase.
     * Dipanggil saat startup atau Sync Now.
     */
    suspend fun syncCustomers(): Boolean = withContext(Dispatchers.IO) {
        val client = supabaseClient ?: return@withContext false
        if (!client.isConfigured()) return@withContext false
        try {
            // Push local customers yang belum ada di cloud
            val localCustomers = customerDao.getAll()
            if (localCustomers.isNotEmpty()) {
                client.upsertCustomers(localCustomers.map {
                    SupabaseCustomerDto(
                        id = it.id,
                        name = it.name,
                        phone = it.phone,
                        points = it.points,
                        totalSpent = it.totalSpend,
                        visitCount = it.orderCount
                    )
                })
            }

            // Pull dari remote
            val remoteCustomers = client.fetchCustomers()
            if (remoteCustomers.isNotEmpty()) {
                customerDao.upsertAll(remoteCustomers.map {
                    CustomerEntity(
                        id = it.id,
                        name = it.name,
                        phone = it.phone,
                        points = it.points ?: 0,
                        totalSpend = it.totalSpent ?: 0.0,
                        orderCount = it.visitCount ?: 0
                    )
                })
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun CustomerEntity.toDomain() = Customer(
        id = id,
        name = name,
        phone = phone,
        points = points,
        totalSpend = totalSpend,
        orderCount = orderCount,
        createdAt = createdAt
    )
}
