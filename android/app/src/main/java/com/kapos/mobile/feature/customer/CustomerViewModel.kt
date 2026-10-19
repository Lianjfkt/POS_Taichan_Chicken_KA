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
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerUiState(isLoading = true))
    val uiState: StateFlow<CustomerUiState> = _uiState.asStateFlow()

    init {
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

        val currentEdit = _uiState.value.customerToEdit
        viewModelScope.launch {
            val customer = if (currentEdit != null) {
                currentEdit.copy(
                    name = trimmedName,
                    phone = trimmedPhone,
                    points = points.coerceAtLeast(0)
                )
            } else {
                Customer(
                    id = UUID.randomUUID().toString(),
                    name = trimmedName,
                    phone = trimmedPhone,
                    points = points.coerceAtLeast(0),
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
        _uiState.update { it.copy(customerToDelete = customer) }
    }

    fun closeDeleteDialog() {
        _uiState.update { it.copy(customerToDelete = null) }
    }

    fun confirmDeleteCustomer() {
        val toDelete = _uiState.value.customerToDelete ?: return
        viewModelScope.launch {
            customerRepository.deleteCustomer(toDelete.id)
            closeDeleteDialog()
        }
    }
}
