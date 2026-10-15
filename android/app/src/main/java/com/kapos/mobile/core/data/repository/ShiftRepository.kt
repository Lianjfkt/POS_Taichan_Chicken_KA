package com.kapos.mobile.core.data.repository

import com.kapos.mobile.core.data.local.AppDatabase
import com.kapos.mobile.core.data.local.entity.ShiftEntity
import com.kapos.mobile.domain.model.Shift
import com.kapos.mobile.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class ShiftRepository(private val database: AppDatabase) {

    private val shiftDao = database.shiftDao()
    private val orderDao = database.orderDao()

    fun observeActiveShift(): Flow<Shift?> {
        return shiftDao.observeActiveShift().map { entity ->
            entity?.toDomain()
        }
    }

    fun observeAllShifts(): Flow<List<Shift>> {
        return shiftDao.observeAllShifts().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun openShift(cashierName: String, startCash: Double): Shift = withContext(Dispatchers.IO) {
        val shift = Shift(
            id = UUID.randomUUID().toString(),
            cashierName = cashierName,
            startCash = startCash,
            isOpen = true,
            openedAt = System.currentTimeMillis()
        )
        shiftDao.insertShift(shift.toEntity())
        shift
    }

    suspend fun closeShift(shift: Shift, endCash: Double): Shift = withContext(Dispatchers.IO) {
        // Calculate system sales for this shift period
        val allOrders = orderDao.getAllOrdersAfter(shift.openedAt)
        val systemSales = allOrders.sumOf { it.total }

        val closed = shift.copy(
            endCash = endCash,
            systemSales = systemSales,
            isOpen = false,
            closedAt = System.currentTimeMillis()
        )
        shiftDao.updateShift(closed.toEntity())
        closed
    }

    private fun ShiftEntity.toDomain(): Shift = Shift(
        id = id,
        cashierName = cashierName,
        startCash = startCash,
        endCash = endCash,
        systemSales = systemSales,
        isOpen = isOpen,
        openedAt = openedAt,
        closedAt = closedAt,
        syncStatus = try { SyncStatus.valueOf(syncStatus) } catch (_: Exception) { SyncStatus.PENDING_SYNC }
    )

    private fun Shift.toEntity(): ShiftEntity = ShiftEntity(
        id = id,
        cashierName = cashierName,
        startCash = startCash,
        endCash = endCash,
        systemSales = systemSales,
        isOpen = isOpen,
        openedAt = openedAt,
        closedAt = closedAt,
        syncStatus = syncStatus.name
    )
}
