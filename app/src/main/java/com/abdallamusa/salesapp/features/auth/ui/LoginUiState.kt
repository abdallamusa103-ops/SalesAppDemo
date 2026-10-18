package com.abdallamusa.salesapp.features.auth.ui

import androidx.annotation.StringRes

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    @param:StringRes val emailErrorRes: Int? = null,
    @param:StringRes val passwordErrorRes: Int? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isInternalUser: Boolean = false
)