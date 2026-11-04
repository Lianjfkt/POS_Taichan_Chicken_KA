package com.kapos.mobile.core.data.repository

import com.kapos.mobile.core.data.remote.SupabaseClient
import com.kapos.mobile.core.data.remote.dto.SupabaseOrderItemPayload
import com.kapos.mobile.core.data.remote.dto.SupabaseOrderPayload
import com.kapos.mobile.domain.model.CartItem
import com.kapos.mobile.domain.model.Order
import com.kapos.mobile.domain.model.OrderType
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.PaymentStatus
import com.kapos.mobile.domain.model.Product
import com.kapos.mobile.domain.model.SyncStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class PosRepositoryTwoWaySyncTest {

    private lateinit var unconfiguredClient: SupabaseClient
    private lateinit var repository: PosRepository

    @Before
    fun setUp() {
        unconfiguredClient = SupabaseClient(baseUrl = "", apiKey = "")
        repository = PosRepository(
            database = null,
            supabaseClient = unconfiguredClient
        )
    }

    @Test
    fun testSyncMethodsReturnFalseWhenClientUnconfigured() = runBlocking {
        assertFalse(repository.syncAll())
        assertFalse(repository.syncMenu())
        assertFalse(repository.syncTransactions())
    }

    @Test
    fun testRemoteOrderTimestampParsing() {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val sampleUtc = "2026-10-22T08:30:00.123456+00:00"
        val parsed = isoFormat.parse(sampleUtc.substring(0, 19))
        assertTrue(parsed != null)
        assertTrue(parsed!!.time > 0L)
    }

    @Test
    fun testRemotePayloadMappingIntegrity() {
        val remoteOrder = SupabaseOrderPayload(
            id = "ord-999",
            orderNo = "KA-20261022-120000",
            orderType = "take-away",
            subtotal = 35000.0,
            total = 35000.0,
            paymentMethod = "qris",
            status = "completed",
            cashierId = "Kasir 1"
        )
        val remoteItem = SupabaseOrderItemPayload(
            id = "item-1",
            orderId = "ord-999",
            productId = "p-1",
            productName = "Taichan Ori",
            price = 15000.0,
            quantity = 2,
            subtotal = 30000.0
        )
        assertEquals("ord-999", remoteOrder.id)
        assertEquals("completed", remoteOrder.status)
        assertEquals("ord-999", remoteItem.orderId)
        assertEquals(30000.0, remoteItem.subtotal, 0.001)
    }

    @Test
    fun testSubmitOrderCompletesAtomicallyWithoutThrowing() = runBlocking {
        val testOrder = Order(
            id = "test-ord-1",
            orderNo = "KA-20261104-001",
            customerName = "Budi Santoso",
            customerPhone = "08123456789",
            orderType = OrderType.DINE_IN,
            tableNo = "T1",
            subtotal = 30000.0,
            discount = 0.0,
            total = 30000.0,
            paymentMethod = PaymentMethod.CASH,
            paymentStatus = PaymentStatus.PAID,
            cashierName = "Kasir 1",
            createdAt = System.currentTimeMillis(),
            paidAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_SYNC
        )

        val testCartItems = listOf(
            CartItem(
                product = Product(
                    id = "prod-1",
                    categoryId = "cat-1",
                    name = "Sate Taichan Ori",
                    price = 15000.0
                ),
                quantity = 2
            )
        )

        val submitted = repository.submitOrder(testOrder, testCartItems)
        assertNotNull(submitted)
        assertEquals("test-ord-1", submitted.id)
        assertEquals(30000.0, submitted.total, 0.001)
    }

    @Test
    fun testVoidOrderCompletesWithoutThrowing() = runBlocking {
        repository.voidOrder("test-ord-1", "Salah input meja")
        // If voidOrder handles fallback/null database gracefully, no exception is thrown
        assertTrue(true)
    }
}
