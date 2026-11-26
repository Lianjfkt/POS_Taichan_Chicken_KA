package com.kapos.mobile.core.sync

import com.kapos.mobile.core.data.remote.SupabaseClient
import com.kapos.mobile.core.data.repository.PosRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncOrdersWorkerTest {

    @Test
    fun testSyncOrdersWorkerWorkNameConstant() {
        assertEquals("SyncOrdersWorker", SyncOrdersWorker.WORK_NAME)
    }

    @Test
    fun testPosRepositorySyncPendingOrdersReturnsFalseWhenUnconfigured() = runBlocking {
        val client = SupabaseClient(baseUrl = "", apiKey = "")
        val repo = PosRepository(database = null, supabaseClient = client)
        assertFalse(repo.isSupabaseConfigured())
        assertFalse(repo.syncPendingOrders())
    }

    @Test
    fun testPosRepositorySyncPendingOrdersReturnsTrueWhenNoOrders() = runBlocking {
        val client = SupabaseClient(baseUrl = "https://example.supabase.co", apiKey = "test-key")
        val repo = PosRepository(database = null, supabaseClient = client)
        assertTrue(repo.isSupabaseConfigured())
        assertTrue(repo.syncPendingOrders())
    }
}
