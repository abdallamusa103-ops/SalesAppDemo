package com.abdallamusa.salesapp.features.auth.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onServerUrlChanged(value: String) {
        _uiState.update { it.copy(serverUrl = value, errorMessage = null) }
    }

    fun onDbNameChanged(value: String) {
        _uiState.update { it.copy(dbName = value, errorMessage = null) }
    }

    fun onUsernameChanged(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun login() {
        val currentState = _uiState.value

        if (currentState.serverUrl.isBlank() ||
            currentState.dbName.isBlank() ||
            currentState.username.isBlank() ||
            currentState.password.isBlank()
        ) {
            _uiState.update { it.copy(errorMessage = "Please enter all connection details") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.login(
                serverUrl = currentState.serverUrl,
                db = currentState.dbName,
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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Authentication failed"
                        )
                    }
                }
            )
        }
    }
}