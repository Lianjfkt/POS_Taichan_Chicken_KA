package com.kapos.mobile.core.data.repository

import com.kapos.mobile.core.data.remote.dto.SupabaseOrderPayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseSyncPayloadTest {

    @Test
    fun testOrderPayloadSerializationIncludesDatesAndVoidReason() {
        val payload = SupabaseOrderPayload(
            id = "test-1",
            orderNo = "KA-20261019-120000-A1",
            orderType = "dine-in",
            subtotal = 50000.0,
            total = 50000.0,
            paymentMethod = "cash",
            status = "void",
            voidReason = "Salah Input Menu",
            createdAt = "2026-10-19T05:00:00Z",
            paidAt = "2026-10-19T05:01:00Z"
        )
        val json = Json.encodeToString(payload)
        assertTrue(json.contains("\"created_at\""))
        assertTrue(json.contains("\"paid_at\""))
        assertTrue(json.contains("\"void_reason\""))
    }
}
