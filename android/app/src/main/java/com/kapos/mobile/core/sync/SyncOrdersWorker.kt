package com.kapos.mobile.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kapos.mobile.KaPosApplication
import com.kapos.mobile.core.data.local.AppDatabase
import com.kapos.mobile.core.data.remote.SupabaseClient
import com.kapos.mobile.core.data.remote.dto.SupabaseOrderItemPayload
import com.kapos.mobile.core.data.remote.dto.SupabaseOrderPayload
import com.kapos.mobile.core.data.repository.SettingsRepository
import com.kapos.mobile.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SyncOrdersWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val app = runCatching { KaPosApplication.instance }.getOrNull()
                ?: (applicationContext as? KaPosApplication)

            val supabase = app?.supabaseClient ?: run {
                val settingsRepo = SettingsRepository(applicationContext)
                val settings = settingsRepo.settingsFlow.value
                SupabaseClient(settings.supabaseUrl, settings.supabaseAnonKey)
            }

            if (!supabase.isConfigured()) {
                return@withContext Result.success()
            }

            val success = if (app != null) {
                app.posRepository.syncPendingOrders()
            } else {
                val database = AppDatabase.getInstance(applicationContext)
                syncPendingOrdersInternal(database, supabase)
            }

            if (success) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: IOException) {
            Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    internal suspend fun syncPendingOrdersInternal(
        database: AppDatabase,
        supabaseClient: SupabaseClient
    ): Boolean {
        val orderDao = database.orderDao()
        val pending = orderDao.getPendingOrders()
        if (pending.isEmpty()) return true

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        var allSuccess = true
        for (order in pending) {
            val items = orderDao.getItemsForOrder(order.id)
            val createdAtIso = isoFormat.format(Date(order.createdAt))
            val paidAtIso = order.paidAt?.let { isoFormat.format(Date(it)) } ?: createdAtIso

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
            } else {
                allSuccess = false
            }
        }

        return allSuccess
    }

    companion object {
        const val WORK_NAME = "SyncOrdersWorker"
    }
}
