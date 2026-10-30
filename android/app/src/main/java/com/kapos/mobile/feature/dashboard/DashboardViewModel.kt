package com.kapos.mobile.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapos.mobile.core.data.repository.InventoryRepository
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val posRepository: PosRepository,
    private val settingsRepository: SettingsRepository,
    private val inventoryRepository: InventoryRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Load settings
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        storeName = settings.storeName,
                        dailyRevenueTarget = settings.dailyRevenueTarget,
                        dailyFixedCost = settings.dailyFixedCost,
                        ownerName = "Owner Gerai",
                        isLoading = false
                    )
                }
            }
        }

        // Observe orders (today + yesterday for trend)
        viewModelScope.launch {
            posRepository.observeOrders().collect { orders ->
                _uiState.update { it.copy(orders = orders, isLoading = false) }
            }
        }

        // Observe order items
        viewModelScope.launch {
            posRepository.observeAllOrderItems().collect { items ->
                _uiState.update { it.copy(orderItems = items) }
            }
        }

        // Observe products (for cost price / emoji)
        viewModelScope.launch {
            posRepository.observeAllProducts(includeInactive = true).collect { prods ->
                _uiState.update { it.copy(products = prods) }
            }
        }

        // Observe raw materials for stock alerts
        inventoryRepository?.let { repo ->
            viewModelScope.launch {
                repo.observeRawMaterials().collect { materials ->
                    _uiState.update { it.copy(rawMaterials = materials) }
                }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
