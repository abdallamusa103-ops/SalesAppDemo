package com.abdallamusa.salesapp.features.customers.domain.repository

import com.abdallamusa.salesapp.features.customers.domain.model.Customer
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    val customersFlow: Flow<List<Customer>>

    fun searchCustomersFlow(query: String): Flow<List<Customer>>

    fun getCustomerByIdFlow(id: Int): Flow<Customer?>

    suspend fun refreshCustomers(): Result<Unit>

    suspend fun updatePhoneOffline(id: Int, newPhone: String): Result<Unit>
}