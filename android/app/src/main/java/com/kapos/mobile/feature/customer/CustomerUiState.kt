package com.kapos.mobile.feature.customer

import com.kapos.mobile.domain.model.Customer

data class CustomerUiState(
    val customers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isAddEditCustomerOpen: Boolean = false,
    val customerToEdit: Customer? = null,
    val customerToDelete: Customer? = null
) {
    val filteredCustomers: List<Customer>
        get() {
            if (searchQuery.isBlank()) return customers
            val q = searchQuery.lowercase().trim()
            return customers.filter {
                it.name.lowercase().contains(q) || it.phone.contains(q)
            }
        }

    val totalMembers: Int
        get() = customers.size

    val totalPointsCirculating: Int
        get() = customers.sumOf { it.points }

    val totalMemberSpend: Double
        get() = customers.sumOf { it.totalSpend }
}
