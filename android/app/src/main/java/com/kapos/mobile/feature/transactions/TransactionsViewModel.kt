package com.kapos.mobile.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapos.mobile.core.data.repository.PosRepository
import com.kapos.mobile.domain.model.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TransactionsViewModel(
    private val repository: PosRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeOrders().collect { list ->
                _uiState.update { it.copy(orders = list) }
            }
        }
        viewModelScope.launch {
            repository.isSyncing.collect { syncing ->
                _uiState.update { it.copy(isSyncing = syncing) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onFilterChange(filter: TransactionFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun onSelectOrder(order: Order) {
        _uiState.update { it.copy(selectedOrder = order, isLoadingDetail = true) }
        viewModelScope.launch {
            val items = repository.getOrderItems(order.id)
            _uiState.update { it.copy(selectedOrderItems = items, isLoadingDetail = false) }
        }
    }

    fun onDismissDetail() {
        _uiState.update { it.copy(selectedOrder = null, selectedOrderItems = emptyList()) }
    }

    fun syncPending() {
        viewModelScope.launch {
            repository.syncPendingOrders()
        }
    }
}
