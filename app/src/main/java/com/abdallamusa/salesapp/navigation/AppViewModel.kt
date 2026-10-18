package com.abdallamusa.salesapp.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abdallamusa.salesapp.core.datastore.SessionPreference
import com.abdallamusa.salesapp.core.datastore.UserSession
import com.abdallamusa.salesapp.core.ui.ErrorBannerManager
import com.abdallamusa.salesapp.features.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    sessionPreference: SessionPreference,
    private val authRepository: AuthRepository,
    private val errorBannerManager: ErrorBannerManager
) : ViewModel() {

    val sessionState: StateFlow<UserSession?> = sessionPreference.sessionFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val errorMessage: StateFlow<String?> = errorBannerManager.errorMessage

    fun dismissError() {
        errorBannerManager.dismissError()
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            errorBannerManager.dismissError()
            authRepository.logout()
            onLoggedOut()
        }
    }
}
