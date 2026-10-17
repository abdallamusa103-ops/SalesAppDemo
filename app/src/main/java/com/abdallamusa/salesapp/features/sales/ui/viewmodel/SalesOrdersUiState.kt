package com.abdallamusa.salesapp.features.sales.ui.viewmodel

import com.abdallamusa.salesapp.features.sales.domain.SalesOrder

data class SalesOrdersUiState(
    val orders: List<SalesOrder> = emptyList(),
    val isLoading: Boolean = false,
    val selectedOrder: SalesOrder? = null,
    val isConfirming: Boolean = false,
    val errorMessage: String? = null
)
