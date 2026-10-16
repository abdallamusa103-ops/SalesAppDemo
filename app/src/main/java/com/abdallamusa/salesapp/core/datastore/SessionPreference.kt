package com.abdallamusa.salesapp.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "odoo_session_prefs")

@Singleton
class SessionPreference @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionEngine: TinkAeadEngine
) {
    private val dataStore = context.dataStore

    val sessionFlow: Flow<UserSession> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val encryptedSessionId = prefs[KEY_SESSION_ID].orEmpty()
            val decryptedSessionId = encryptionEngine.decrypt(encryptedSessionId)

            UserSession(
                userId = prefs[KEY_USER_ID],
                sessionId = decryptedSessionId.ifEmpty { null },
                isInternalUser = prefs[KEY_IS_INTERNAL] ?: false,
                serverUrl = prefs[KEY_SERVER_URL],
                dbName = prefs[KEY_DB_NAME]
            )
        }

    suspend fun saveSession(
        userId: Int,
        sessionId: String,
        isInternalUser: Boolean,
        serverUrl: String,
        dbName: String
    ) {
        val encryptedSessionId = encryptionEngine.encrypt(sessionId)
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_SESSION_ID] = encryptedSessionId
            prefs[KEY_IS_INTERNAL] = isInternalUser
            prefs[KEY_SERVER_URL] = serverUrl
            prefs[KEY_DB_NAME] = dbName
        }
    }

    suspend fun clearSession() {
        dataStore.edit { it.clear() }
    }

    companion object {
        private val KEY_USER_ID = intPreferencesKey("user_id")
        private val KEY_SESSION_ID = stringPreferencesKey("session_id")
        private val KEY_IS_INTERNAL = booleanPreferencesKey("is_internal_user")
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_DB_NAME = stringPreferencesKey("db_name")
    }
}