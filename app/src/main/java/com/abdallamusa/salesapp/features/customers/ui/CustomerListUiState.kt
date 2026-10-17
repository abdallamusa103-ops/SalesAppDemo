package com.abdallamusa.salesapp.features.customers.ui

import com.abdallamusa.salesapp.features.customers.domain.model.Customer

data class CustomerListUiState(
    val customers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)