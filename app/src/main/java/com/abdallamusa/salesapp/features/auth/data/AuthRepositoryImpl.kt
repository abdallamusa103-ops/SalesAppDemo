package com.abdallamusa.salesapp.features.auth.data

import com.abdallamusa.salesapp.core.datastore.SessionPreference
import com.abdallamusa.salesapp.core.datastore.UserSession
import com.abdallamusa.salesapp.core.network.DynamicUrlInterceptor
import com.abdallamusa.salesapp.core.network.OdooApiService
import com.abdallamusa.salesapp.core.network.OdooXmlRpcClient
import com.abdallamusa.salesapp.core.network.model.OdooRpcRequest
import com.abdallamusa.salesapp.features.auth.domain.AuthRepository
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val odooApiService: OdooApiService,
    private val odooXmlRpcClient: OdooXmlRpcClient,
    private val sessionPreference: SessionPreference,
    private val dynamicUrlInterceptor: DynamicUrlInterceptor
) : AuthRepository {

    override val sessionFlow: Flow<UserSession> = sessionPreference.sessionFlow

    override suspend fun login(
        serverUrl: String,
        db: String,
        username: String,
        password: String
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        val cleanServerUrl = serverUrl.trim().trimEnd('/')
        dynamicUrlInterceptor.updateBaseUrl(cleanServerUrl)

        // 1. Primary Strategy: Try Odoo REST JSON-RPC
        val restResult = tryRestAuthentication(cleanServerUrl, db, username, password)
        if (restResult.isSuccess) {
            return@withContext restResult
        }

        // 2. Fallback Strategy: If REST failed, try Odoo XML-RPC
        val xmlRpcResult = odooXmlRpcClient.authenticate(cleanServerUrl, db, username, password)
        if (xmlRpcResult.isSuccess) {
            val uid = xmlRpcResult.getOrThrow()
            val session = UserSession(
                userId = uid,
                sessionId = "xmlrpc_session_$uid",
                isInternalUser = true, // Default for authenticated XML-RPC users
                serverUrl = cleanServerUrl,
                dbName = db
            )
            sessionPreference.saveSession(
                userId = uid,
                sessionId = session.sessionId.orEmpty(),
                isInternalUser = session.isInternalUser,
                serverUrl = cleanServerUrl,
                dbName = db
            )
            return@withContext Result.success(session)
        }

        // 3. Both failed
        Result.failure(Exception("Authentication failed on both REST and XML-RPC. Please check credentials, database, and server URL."))
    }

    private suspend fun tryRestAuthentication(
        serverUrl: String,
        db: String,
        username: String,
        password: String
    ): Result<UserSession> {
        return try {
            val authParams = mapOf(
                "db" to db,
                "login" to username,
                "password" to password
            )
            val response = odooApiService.authenticate(OdooRpcRequest(params = authParams))

            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP Error ${response.code()}"))
            }

            val rpcResponse = response.body()
            if (rpcResponse?.error != null) {
                return Result.failure(Exception(rpcResponse.error.message ?: "Odoo Auth Error"))
            }

            val resultObj = rpcResponse?.result?.asJsonObject
            val uid = resultObj?.get("uid")?.takeIf { !it.isJsonNull }?.asInt

            if (uid == null || uid <= 0) {
                return Result.failure(Exception("Invalid credentials"))
            }

            // Extract session_id from Set-Cookie header
            var sessionId: String? = null
            val cookieHeaders = response.headers().values("Set-Cookie")
            for (cookie in cookieHeaders) {
                val match = Regex("session_id=([^;]+)").find(cookie)
                if (match != null) {
                    sessionId = match.groupValues[1]
                    break
                }
            }

            // Check if user has base.group_user (Internal User role)
            val isInternalUser = checkIsInternalUser(uid)

            val session = UserSession(
                userId = uid,
                sessionId = sessionId ?: "session_$uid",
                isInternalUser = isInternalUser,
                serverUrl = serverUrl,
                dbName = db
            )

            sessionPreference.saveSession(
                userId = uid,
                sessionId = session.sessionId.orEmpty(),
                isInternalUser = isInternalUser,
                serverUrl = serverUrl,
                dbName = db
            )

            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun checkIsInternalUser(uid: Int): Boolean {
        return try {
            val groupCheckParams = mapOf(
                "model" to "res.users",
                "method" to "has_group",
                "args" to listOf("base.group_user"),
                "kwargs" to emptyMap<String, Any>()
            )
            val response = odooApiService.callKw("res.users", "has_group", OdooRpcRequest(params = groupCheckParams))
            val result = response.body()?.result
            result?.asBoolean ?: true
        } catch (e: Exception) {
            true // Fallback gracefully to allow sales exploration if role query fails
        }
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        sessionPreference.clearSession()
    }
}