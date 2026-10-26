package com.kapos.mobile.core.data.remote

import com.kapos.mobile.core.data.remote.dto.SupabaseCategoryDto
import com.kapos.mobile.core.data.remote.dto.SupabaseOrderItemPayload
import com.kapos.mobile.core.data.remote.dto.SupabaseOrderPayload
import com.kapos.mobile.core.data.remote.dto.SupabaseProductDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SupabaseClientSyncTest {

    private lateinit var unconfiguredClient: SupabaseClient
    private lateinit var configuredClient: SupabaseClient

    @Before
    fun setUp() {
        unconfiguredClient = SupabaseClient(baseUrl = "", apiKey = "")
        configuredClient = SupabaseClient(
            baseUrl = "https://example.supabase.co",
            apiKey = "sample-key-12345"
        )
    }

    @Test
    fun testUnconfiguredClientReturnsGracefully() = runBlocking {
        assertFalse(unconfiguredClient.isConfigured())
        assertFalse(unconfiguredClient.testConnection())
        assertTrue(unconfiguredClient.fetchCategories().isEmpty())
        assertTrue(unconfiguredClient.fetchProducts().isEmpty())
        assertFalse(unconfiguredClient.upsertCategories(emptyList()))
        assertFalse(unconfiguredClient.upsertProducts(emptyList()))
        assertTrue(unconfiguredClient.fetchOrders().isEmpty())
        assertTrue(unconfiguredClient.fetchOrderItems(emptyList()).isEmpty())
    }

    @Test
    fun testConfiguredClientWithEmptyInputReturnsGracefully() = runBlocking {
        assertTrue(configuredClient.isConfigured())
        // Empty upsert list should return true/false without crashing
        assertFalse(configuredClient.upsertCategories(emptyList()))
        assertFalse(configuredClient.upsertProducts(emptyList()))
        assertTrue(configuredClient.fetchOrderItems(emptyList()).isEmpty())
    }
}
