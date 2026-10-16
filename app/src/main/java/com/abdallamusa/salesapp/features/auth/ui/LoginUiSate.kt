package com.abdallamusa.salesapp.features.auth.ui

data class LoginUiState(
    val serverUrl: String = "",
    val dbName: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isInternalUser: Boolean = false
)