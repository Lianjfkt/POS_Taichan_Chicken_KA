package com.kapos.mobile.core.data.repository

import com.kapos.mobile.core.data.local.AppDatabase
import com.kapos.mobile.core.data.local.entity.CashMovementEntity
import com.kapos.mobile.core.data.local.entity.ShiftEntity
import com.kapos.mobile.core.data.remote.SupabaseClient
import com.kapos.mobile.core.data.remote.dto.SupabaseCashMovementPayload
import com.kapos.mobile.core.data.remote.dto.SupabaseShiftPayload
import com.kapos.mobile.domain.model.CashMovement
import com.kapos.mobile.domain.model.CashMovementType
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.Shift
import com.kapos.mobile.domain.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class ShiftRepository(
    private val database: AppDatabase,
    private val supabaseClient: SupabaseClient? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val shiftDao = database.shiftDao()
    private val orderDao = database.orderDao()
    private val cashMovementDao = database.cashMovementDao()

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

    fun observeCashMovements(shiftId: String): Flow<List<CashMovement>> {
        return cashMovementDao.observeByShift(shiftId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeAllCashMovements(): Flow<List<CashMovement>> {
        return cashMovementDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getCashMovements(shiftId: String): List<CashMovement> = withContext(Dispatchers.IO) {
        cashMovementDao.getByShift(shiftId).map { it.toDomain() }
    }

    suspend fun addCashMovement(
        shiftId: String,
        type: CashMovementType,
        category: String,
        amount: Double,
        notes: String = ""
    ): CashMovement = withContext(Dispatchers.IO) {
        val movement = CashMovement(
            id = UUID.randomUUID().toString(),
            shiftId = shiftId,
            type = type,
            category = category,
            amount = amount,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )
        cashMovementDao.insert(movement.toEntity())
        shiftDao.updateSyncStatus(shiftId, SyncStatus.PENDING_SYNC.name)
        scope.launch { syncPendingShifts() }
        movement
    }

    suspend fun openShift(cashierName: String, startCash: Double): Shift = withContext(Dispatchers.IO) {
        val shift = Shift(
            id = UUID.randomUUID().toString(),
            cashierName = cashierName,
            startCash = startCash,
            isOpen = true,
            openedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_SYNC
        )
        shiftDao.insertShift(shift.toEntity())
        scope.launch { syncPendingShifts() }
        shift
    }

    suspend fun closeShift(shift: Shift, endCash: Double): Shift = withContext(Dispatchers.IO) {
        // Calculate system sales for this shift period (excluding VOID)
        val allOrders = orderDao.getAllOrdersAfter(shift.openedAt)
        val systemSales = allOrders.sumOf { it.total }

        val closed = shift.copy(
            endCash = endCash,
            systemSales = systemSales,
            isOpen = false,
            closedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_SYNC
        )
        shiftDao.updateShift(closed.toEntity())
        scope.launch { syncPendingShifts() }
        closed
    }

    suspend fun syncPendingShifts(): Boolean = withContext(Dispatchers.IO) {
        val client = supabaseClient ?: return@withContext false
        if (!client.isConfigured()) return@withContext false

        try {
            val pendingShifts = shiftDao.getPendingShifts()
            if (pendingShifts.isEmpty()) return@withContext true

            val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            for (shiftEntity in pendingShifts) {
                val movements = cashMovementDao.getByShift(shiftEntity.id)
                val movementPayloads = movements.map { m ->
                    SupabaseCashMovementPayload(
                        id = m.id,
                        shiftId = m.shiftId,
                        type = m.type,
                        category = m.category,
                        amount = m.amount,
                        notes = m.notes,
                        createdAt = isoDateFormat.format(Date(m.createdAt))
                    )
                }

                val shiftOrders = orderDao.getAllOrdersAfter(shiftEntity.openedAt)
                val shiftPayload = SupabaseShiftPayload(
                    id = shiftEntity.id,
                    cashierName = shiftEntity.cashierName,
                    startTime = isoDateFormat.format(Date(shiftEntity.openedAt)),
                    endTime = shiftEntity.closedAt?.let { isoDateFormat.format(Date(it)) },
                    startingCash = shiftEntity.startCash,
                    expectedCash = shiftEntity.systemSales,
                    actualCash = shiftEntity.endCash,
                    cashDifference = if (!shiftEntity.isOpen) shiftEntity.endCash - (shiftEntity.startCash + shiftEntity.systemSales) else null,
                    totalTransactions = shiftOrders.size,
                    notes = null,
                    isOpen = shiftEntity.isOpen,
                    createdAt = isoDateFormat.format(Date(shiftEntity.openedAt))
                )

                val successShift = client.postShifts(listOf(shiftPayload))
                if (movementPayloads.isNotEmpty()) {
                    client.postCashMovements(movementPayloads)
                }

                if (successShift) {
                    shiftDao.updateSyncStatus(shiftEntity.id, SyncStatus.SYNCED.name)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun CashMovementEntity.toDomain(): CashMovement = CashMovement(
        id = id,
        shiftId = shiftId,
        type = try { CashMovementType.valueOf(type) } catch (_: Exception) { CashMovementType.CASH_OUT },
        category = category,
        amount = amount,
        notes = notes,
        createdAt = createdAt
    )

    private fun CashMovement.toEntity(): CashMovementEntity = CashMovementEntity(
        id = id,
        shiftId = shiftId,
        type = type.name,
        category = category,
        amount = amount,
        notes = notes,
        createdAt = createdAt
    )

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
