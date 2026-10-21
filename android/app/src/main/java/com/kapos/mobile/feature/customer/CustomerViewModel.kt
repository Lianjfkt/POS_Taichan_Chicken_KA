package com.kapos.mobile.feature.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapos.mobile.core.data.repository.CustomerRepository
import com.kapos.mobile.domain.model.Customer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class CustomerViewModel(
    private val customerRepository: CustomerRepository,
    private val settingsRepository: com.kapos.mobile.core.data.repository.SettingsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CustomerUiState(
            isLoading = true,
            isOwner = settingsRepository?.settingsFlow?.value?.currentSession?.role == com.kapos.mobile.domain.model.UserRole.OWNER
        )
    )
    val uiState: StateFlow<CustomerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository?.settingsFlow?.collect { settings ->
                val owner = settings.currentSession.role == com.kapos.mobile.domain.model.UserRole.OWNER
                _uiState.update { it.copy(isOwner = owner) }
            }
        }

        viewModelScope.launch {
            customerRepository.observeCustomers().collect { list ->
                _uiState.update {
                    it.copy(
                        customers = list,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openAddCustomer() {
        _uiState.update {
            it.copy(
                isAddEditCustomerOpen = true,
                customerToEdit = null
            )
        }
    }

    fun openEditCustomer(customer: Customer) {
        _uiState.update {
            it.copy(
                isAddEditCustomerOpen = true,
                customerToEdit = customer
            )
        }
    }

    fun closeAddEditDialog() {
        _uiState.update {
            it.copy(
                isAddEditCustomerOpen = false,
                customerToEdit = null
            )
        }
    }

    fun saveCustomer(name: String, phone: String, points: Int) {
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        if (trimmedName.isBlank()) return

        val isOwner = _uiState.value.isOwner
        val currentEdit = _uiState.value.customerToEdit

        // Non-owner cannot change existing points directly (preserve current points or 0 for new)
        val validPoints = if (isOwner) {
            points.coerceAtLeast(0)
        } else {
            currentEdit?.points ?: 0
        }

        viewModelScope.launch {
            val customer = if (currentEdit != null) {
                currentEdit.copy(
                    name = trimmedName,
                    phone = trimmedPhone,
                    points = validPoints
                )
            } else {
                Customer(
                    id = UUID.randomUUID().toString(),
                    name = trimmedName,
                    phone = trimmedPhone,
                    points = validPoints,
                    totalSpend = 0.0,
                    orderCount = 0,
                    createdAt = System.currentTimeMillis()
                )
            }
            customerRepository.saveCustomer(customer)
            closeAddEditDialog()
        }
    }

    fun openDeleteDialog(customer: Customer) {
        if (!_uiState.value.isOwner) {
            _uiState.update { it.copy(errorMessage = "Hanya Owner yang berhak menghapus data pelanggan.") }
            return
        }
        _uiState.update { it.copy(customerToDelete = customer, errorMessage = null) }
    }

    fun closeDeleteDialog() {
        _uiState.update { it.copy(customerToDelete = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun confirmDeleteCustomer() {
        if (!_uiState.value.isOwner) {
            _uiState.update { it.copy(customerToDelete = null, errorMessage = "Hanya Owner yang berhak menghapus data pelanggan.") }
            return
        }
        val toDelete = _uiState.value.customerToDelete ?: return
        viewModelScope.launch {
            customerRepository.deleteCustomer(toDelete.id)
            closeDeleteDialog()
        }
    }
}
