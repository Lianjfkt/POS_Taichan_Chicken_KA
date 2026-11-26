package com.kapos.mobile.core.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkMonitorTest {

    @Test
    fun testNetworkMonitorInitialStateWithoutContext() {
        val monitor = NetworkMonitor(context = null)
        assertFalse(monitor.isOnlineFlow.value)
        assertFalse(monitor.isCurrentlyConnected())
    }

    @Test
    fun testNetworkMonitorTestingStateTransition() {
        val monitor = NetworkMonitor(context = null)
        assertFalse(monitor.isOnlineFlow.value)

        monitor.setOnlineForTesting(true)
        assertTrue(monitor.isOnlineFlow.value)

        monitor.setOnlineForTesting(false)
        assertFalse(monitor.isOnlineFlow.value)
    }

    @Test
    fun testNetworkMonitorUnregisterSafety() {
        val monitor = NetworkMonitor(context = null)
        monitor.unregister()
        monitor.unregister()
    }
}
