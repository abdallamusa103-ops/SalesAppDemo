package com.abdallamusa.salesapp.features.auth.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abdallamusa.salesapp.core.ui.ErrorBannerManager
import com.abdallamusa.salesapp.core.utils.CredentialValidator
import com.abdallamusa.salesapp.core.utils.ErrorConstants
import com.abdallamusa.salesapp.features.auth.domain.AuthRepository
import com.abdallamusa.salesapp.features.auth.ui.LoginUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val errorBannerManager: ErrorBannerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChanged(value: String) {
        val emailError = if (value.isNotEmpty()) CredentialValidator.validateEmail(value) else null
        _uiState.update {
            it.copy(
                username = value,
                emailErrorRes = emailError
            )
        }
    }

    fun onPasswordChanged(value: String) {
        val passwordError = if (value.isNotEmpty()) CredentialValidator.validatePassword(value) else null
        _uiState.update {
            it.copy(
                password = value,
                passwordErrorRes = passwordError
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
        errorBannerManager.dismissError()
    }

    fun login() {
        val currentState = _uiState.value
        val emailError = CredentialValidator.validateEmail(currentState.username)
        val passwordError = CredentialValidator.validatePassword(currentState.password)

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    emailErrorRes = emailError,
                    passwordErrorRes = passwordError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    emailErrorRes = null,
                    passwordErrorRes = null,
                    errorMessage = null
                )
            }
            errorBannerManager.dismissError()

            val result = authRepository.login(
                username = currentState.username,
                password = currentState.password
            )

            result.fold(
                onSuccess = { session ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            isInternalUser = session.isInternalUser
                        )
                    }
                },
                onFailure = { error ->
                    val message = error.message ?: ErrorConstants.ERROR_AUTH_FAILED
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = message
                        )
                    }
                    errorBannerManager.showError(message)
                }
            )
        }
    }
}