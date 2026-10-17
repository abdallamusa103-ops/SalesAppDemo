package com.abdallamusa.salesapp.features.sales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abdallamusa.salesapp.features.sales.domain.SalesOrder
import com.abdallamusa.salesapp.features.sales.domain.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SalesOrderViewModel @Inject constructor(
    private val salesRepository: SalesRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _selectedOrder = MutableStateFlow<SalesOrder?>(null)
    private val _isConfirming = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SalesOrdersUiState> = combine(
        salesRepository.salesOrdersFlow,
        _isLoading,
        _selectedOrder,
        _isConfirming,
        _errorMessage
    ) { orders, loading, selected, confirming, error ->
        SalesOrdersUiState(
            orders = orders,
            isLoading = loading,
            selectedOrder = selected,
            isConfirming = confirming,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SalesOrdersUiState(isLoading = true)
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            salesRepository.refreshSalesOrders()
                .onFailure { error ->
                    _errorMessage.value = error.message
                }
            _isLoading.value = false
        }
    }

    fun onOrderClick(order: SalesOrder) {
        viewModelScope.launch {
            salesRepository.getOrderWithProductsFlow(order.id).collect { detailedOrder ->
                _selectedOrder.value = detailedOrder ?: order
            }
        }
    }

    fun onDismissDetails() {
        _selectedOrder.value = null
    }

    fun confirmOrder(orderId: Int) {
        viewModelScope.launch {
            _isConfirming.value = true
            salesRepository.confirmOrder(orderId)
                .onFailure { error ->
                    _errorMessage.value = error.message
                }
            _isConfirming.value = false
        }
    }
}
