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

class CustomerRepository(
    private val database: AppDatabase,
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
        entity.toDomain()
    }

    suspend fun deleteCustomer(id: String) = withContext(Dispatchers.IO) {
        customerDao.deleteById(id)
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
