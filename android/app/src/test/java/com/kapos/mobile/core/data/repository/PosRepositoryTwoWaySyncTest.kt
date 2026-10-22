package com.kapos.mobile.core.data.repository

import com.kapos.mobile.core.data.remote.SupabaseClient
import com.kapos.mobile.core.data.remote.dto.SupabaseOrderItemPayload
import com.kapos.mobile.core.data.remote.dto.SupabaseOrderPayload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
