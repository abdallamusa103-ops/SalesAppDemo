package com.abdallamusa.salesapp.features.customers.ui.viewmodel

import com.abdallamusa.salesapp.features.customers.domain.model.Customer

data class CustomerDetailsUiState(
    val customer: Customer? = null,
    val phoneInput: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)
