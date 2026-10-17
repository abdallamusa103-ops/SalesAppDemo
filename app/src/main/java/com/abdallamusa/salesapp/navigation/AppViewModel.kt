package com.abdallamusa.salesapp.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abdallamusa.salesapp.core.datastore.SessionPreference
import com.abdallamusa.salesapp.core.datastore.UserSession
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
    private val authRepository: AuthRepository
) : ViewModel() {

    val sessionState: StateFlow<UserSession?> = sessionPreference.sessionFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
