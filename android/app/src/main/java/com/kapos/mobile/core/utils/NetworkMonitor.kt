package com.kapos.mobile.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(
    context: Context? = null,
    private val connectivityManager: ConnectivityManager? = context?.let {
        try {
            (it.applicationContext ?: it).getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        } catch (e: Exception) {
            null
        }
    }
) {
    private val _isOnlineFlow = MutableStateFlow(isCurrentlyConnected())
    val isOnlineFlow: StateFlow<Boolean> = _isOnlineFlow.asStateFlow()

    private var isRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val hasInternet = try {
                connectivityManager?.getNetworkCapabilities(network)
                    ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            } catch (e: Exception) {
                false
            }
            if (hasInternet) {
                _isOnlineFlow.value = true
            } else {
                updateConnectivity()
            }
        }

        override fun onLost(network: Network) {
            updateConnectivity()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            if (hasInternet) {
                _isOnlineFlow.value = true
            } else {
                updateConnectivity()
            }
        }
    }

    init {
        register()
    }

    @Synchronized
    fun register() {
        if (!isRegistered && connectivityManager != null) {
            try {
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
                isRegistered = true
                updateConnectivity()
            } catch (e: Exception) {
                updateConnectivity()
            }
        }
    }

    @Synchronized
    fun unregister() {
        if (isRegistered && connectivityManager != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            } catch (e: Exception) {
                // Ignore if already unregistered
            } finally {
                isRegistered = false
            }
        }
    }

    fun isCurrentlyConnected(): Boolean {
        val cm = connectivityManager ?: return false
        return try {
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    private fun updateConnectivity() {
        _isOnlineFlow.value = isCurrentlyConnected()
    }

    // Visible for testing / mock support
    internal fun setOnlineForTesting(isOnline: Boolean) {
        _isOnlineFlow.value = isOnline
    }
}
