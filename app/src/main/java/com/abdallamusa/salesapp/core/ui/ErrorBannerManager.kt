package com.abdallamusa.salesapp.core.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorBannerManager @Inject constructor() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun showError(message: String?) {
        if (!message.isNullOrBlank()) {
            _errorMessage.value = message
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
