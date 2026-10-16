package com.abdallamusa.salesapp.features.auth.domain

import com.abdallamusa.salesapp.core.datastore.UserSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val sessionFlow: Flow<UserSession>

    suspend fun login(
        serverUrl: String,
        db: String,
        username: String,
        password: String
    ): Result<UserSession>

    suspend fun logout()
}