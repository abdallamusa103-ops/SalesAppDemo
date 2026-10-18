package com.abdallamusa.salesapp.features.customers.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abdallamusa.salesapp.core.ui.ErrorBannerManager
import com.abdallamusa.salesapp.features.customers.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerDetailsViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val errorBannerManager: ErrorBannerManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val customerId: Int = checkNotNull(savedStateHandle["customerId"])

    private val _uiState = MutableStateFlow(CustomerDetailsUiState(isLoading = true))
    val uiState: StateFlow<CustomerDetailsUiState> = _uiState.asStateFlow()

    init {
        loadCustomer()
    }

    private fun loadCustomer() {
        viewModelScope.launch {
            customerRepository.getCustomerByIdFlow(customerId).collect { customer ->
                _uiState.update { current ->
                    current.copy(
                        customer = customer,
                        phoneInput = if (current.phoneInput.isEmpty()) customer?.phone.orEmpty() else current.phoneInput,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onPhoneChanged(newPhone: String) {
        _uiState.update { it.copy(phoneInput = newPhone, saveSuccess = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
        errorBannerManager.dismissError()
    }

    fun savePhone() {
        val current = _uiState.value
        val customer = current.customer ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            customerRepository.updatePhoneOffline(customer.id, current.phoneInput.trim())
                .fold(
                    onSuccess = {
                        _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                saveSuccess = true,
                                errorMessage = error.message
                            )
                        }
                        errorBannerManager.showError(error.message)
                    }
                )
        }
    }
}
