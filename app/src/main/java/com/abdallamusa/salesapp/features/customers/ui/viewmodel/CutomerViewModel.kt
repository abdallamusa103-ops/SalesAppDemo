package com.abdallamusa.salesapp.features.customers.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abdallamusa.salesapp.core.ui.ErrorBannerManager
import com.abdallamusa.salesapp.features.customers.domain.repository.CustomerRepository
import com.abdallamusa.salesapp.features.customers.ui.CustomerListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val errorBannerManager: ErrorBannerManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _customersFlow = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            customerRepository.customersFlow
        } else {
            customerRepository.searchCustomersFlow(query.trim())
        }
    }

    val uiState: StateFlow<CustomerListUiState> = combine(
        _customersFlow,
        _searchQuery,
        _isRefreshing,
        _errorMessage
    ) { customers, query, refreshing, error ->
        CustomerListUiState(
            customers = customers,
            searchQuery = query,
            isRefreshing = refreshing,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CustomerListUiState(isRefreshing = true)
    )

    init {
        refresh()
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun dismissError() {
        _errorMessage.value = null
        errorBannerManager.dismissError()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            errorBannerManager.dismissError()
            customerRepository.refreshCustomers()
                .onFailure { error ->
                    _errorMessage.value = error.message
                    errorBannerManager.showError(error.message)
                }
            _isRefreshing.value = false
        }
    }
}