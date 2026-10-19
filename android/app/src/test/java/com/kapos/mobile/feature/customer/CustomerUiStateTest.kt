package com.kapos.mobile.feature.customer

import com.kapos.mobile.domain.model.Customer
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomerUiStateTest {

    private val sampleCustomers = listOf(
        Customer(
            id = "c1",
            name = "Budi Santoso",
            phone = "081234567890",
            points = 120,
            totalSpend = 450000.0,
            orderCount = 6
        ),
        Customer(
            id = "c2",
            name = "Siti Aminah",
            phone = "081987654321",
            points = 250,
            totalSpend = 920000.0,
            orderCount = 12
        ),
        Customer(
            id = "c3",
            name = "Ahmad Fauzi",
            phone = "085678901234",
            points = 60,
            totalSpend = 210000.0,
            orderCount = 3
        )
    )

    @Test
    fun testCustomerStatsAggregations() {
        val state = CustomerUiState(customers = sampleCustomers)

        assertEquals(3, state.totalMembers)
        assertEquals(430, state.totalPointsCirculating) // 120 + 250 + 60
        assertEquals(1580000.0, state.totalMemberSpend, 0.001) // 450k + 920k + 210k
    }

    @Test
    fun testCustomerSearchByName() {
        val state = CustomerUiState(
            customers = sampleCustomers,
            searchQuery = "budi"
        )

        assertEquals(1, state.filteredCustomers.size)
        assertEquals("Budi Santoso", state.filteredCustomers.first().name)
    }

    @Test
    fun testCustomerSearchByPhone() {
        val state = CustomerUiState(
            customers = sampleCustomers,
            searchQuery = "98765"
        )

        assertEquals(1, state.filteredCustomers.size)
        assertEquals("Siti Aminah", state.filteredCustomers.first().name)
    }
}
