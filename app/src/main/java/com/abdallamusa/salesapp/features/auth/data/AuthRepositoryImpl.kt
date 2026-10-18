package com.abdallamusa.salesapp.features.auth.data

import com.abdallamusa.salesapp.core.datastore.SessionPreference
import com.abdallamusa.salesapp.core.datastore.UserSession
import com.abdallamusa.salesapp.core.network.DynamicUrlInterceptor
import com.abdallamusa.salesapp.core.network.OdooApiService
import com.abdallamusa.salesapp.core.network.OdooServerConfig
import com.abdallamusa.salesapp.core.network.OdooXmlRpcClient
import com.abdallamusa.salesapp.core.network.model.OdooRpcRequest
import com.abdallamusa.salesapp.core.utils.ErrorConstants
import com.abdallamusa.salesapp.features.auth.domain.AuthRepository
import com.abdallamusa.salesapp.features.sales.data.SalesOrderDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val odooApiService: OdooApiService,
    private val odooXmlRpcClient: OdooXmlRpcClient,
    private val odooServerConfig: OdooServerConfig,
    private val sessionPreference: SessionPreference,
    private val dynamicUrlInterceptor: DynamicUrlInterceptor,
    private val salesOrderDao: SalesOrderDao
) : AuthRepository {

    override val sessionFlow: Flow<UserSession> = sessionPreference.sessionFlow

    override suspend fun login(
        username: String,
        password: String
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        val cleanServerUrl = odooServerConfig.baseUrl
        dynamicUrlInterceptor.updateBaseUrl(cleanServerUrl)
        val db = odooServerConfig.resolveDatabaseName()

        // 1. Primary Strategy: Try Odoo REST JSON-RPC
        val restResult = tryRestAuthentication(cleanServerUrl, db, username, password)
        if (restResult.isSuccess) {
            return@withContext restResult
        }

        // If REST returned an explicit Odoo credential error (like Access Denied), return it immediately
        val restError = restResult.exceptionOrNull()
        if (restError != null && restError.message?.contains("Access Denied", ignoreCase = true) == true) {
            return@withContext Result.failure(Exception(ErrorConstants.mapThrowableToMessage(restError)))
        }

        // 2. Fallback Strategy: If REST endpoint was unreachable/unsupported, try Odoo XML-RPC
        val xmlRpcResult = odooXmlRpcClient.authenticate(cleanServerUrl, db, username, password)
        if (xmlRpcResult.isSuccess) {
            val uid = xmlRpcResult.getOrThrow()
            val hasBaseGroupUser = odooXmlRpcClient
                .checkHasGroup(cleanServerUrl, db, uid, password, "base.group_user")
                .getOrDefault(false)
            val canAccessSaleOrders = if (hasBaseGroupUser) {
                odooXmlRpcClient.checkSaleOrderAccess(cleanServerUrl, db, uid, password)
                    .getOrDefault(false)
            } else {
                false
            }
            val isInternalUser = hasBaseGroupUser && canAccessSaleOrders

            if (!isInternalUser) {
                salesOrderDao.clearAllOrderLines()
                salesOrderDao.clearSalesOrders()
            }

            val session = UserSession(
                userId = uid,
                sessionId = "xmlrpc_session_$uid",
                isInternalUser = isInternalUser,
                serverUrl = cleanServerUrl,
                dbName = db,
                password = password
            )
            sessionPreference.saveSession(
                userId = uid,
                sessionId = session.sessionId.orEmpty(),
                isInternalUser = session.isInternalUser,
                serverUrl = cleanServerUrl,
                dbName = db,
                password = password
            )
            return@withContext Result.success(session)
        }

        // 3. Both failed: map network or server exception to clean user-facing message
        val primaryThrowable = restError ?: xmlRpcResult.exceptionOrNull()
        val cleanMessage = if (primaryThrowable != null) {
            ErrorConstants.mapThrowableToMessage(primaryThrowable)
        } else {
            ErrorConstants.ERROR_AUTH_FAILED
        }
        Result.failure(Exception(cleanMessage))
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
                "login" to username.trim(),
                "password" to password
            )
            val response = odooApiService.authenticate(OdooRpcRequest(params = authParams))

            if (!response.isSuccessful) {
                return Result.failure(
                    Exception(ErrorConstants.httpError(response.code(), response.message()))
                )
            }

            val rpcResponse = response.body()
            if (rpcResponse?.error != null) {
                return Result.failure(Exception(rpcResponse.error.readableMessage))
            }

            val resultObj = rpcResponse?.result?.takeIf { it.isJsonObject }?.asJsonObject
            val uidElement = resultObj?.get("uid")
            val uid = if (uidElement != null && !uidElement.isJsonNull && uidElement.isJsonPrimitive && uidElement.asJsonPrimitive.isNumber) {
                uidElement.asInt
            } else {
                null
            }

            if (uid == null || uid <= 0) {
                return Result.failure(Exception(ErrorConstants.ERROR_ACCESS_DENIED))
            }

            var sessionId: String? = null
            val cookieHeaders = response.headers().values("Set-Cookie")
            for (cookie in cookieHeaders) {
                val match = Regex("session_id=([^;]+)").find(cookie)
                if (match != null) {
                    sessionId = match.groupValues[1]
                    break
                }
            }
            val resolvedSessionId = sessionId ?: resultObj?.get("session_id")?.takeIf { !it.isJsonNull }?.asString ?: "session_$uid"

            // Save session cookie first so subsequent call_kw requests carry the authenticated Cookie
            sessionPreference.saveSession(
                userId = uid,
                sessionId = resolvedSessionId,
                isInternalUser = false,
                serverUrl = serverUrl,
                dbName = db,
                password = password
            )

            // Always verify both base.group_user AND sale.order read access so Light/Portal users are gated
            val isInternalUser = checkIsInternalUser(
                uid = uid,
                serverUrl = serverUrl,
                db = db,
                password = password
            )

            if (!isInternalUser) {
                salesOrderDao.clearAllOrderLines()
                salesOrderDao.clearSalesOrders()
            }

            val session = UserSession(
                userId = uid,
                sessionId = resolvedSessionId,
                isInternalUser = isInternalUser,
                serverUrl = serverUrl,
                dbName = db,
                password = password
            )

            sessionPreference.saveSession(
                userId = uid,
                sessionId = resolvedSessionId,
                isInternalUser = isInternalUser,
                serverUrl = serverUrl,
                dbName = db,
                password = password
            )

            Result.success(session)
        } catch (e: Exception) {
            Result.failure(Exception(ErrorConstants.mapThrowableToMessage(e)))
        }
    }

    private suspend fun checkIsInternalUser(
        uid: Int,
        serverUrl: String,
        db: String,
        password: String
    ): Boolean {
        val hasGroupUser = checkHasGroupBaseUser(uid, serverUrl, db, password)
        if (!hasGroupUser) return false
        return checkCanReadSaleOrders(uid, serverUrl, db, password)
    }

    private suspend fun checkHasGroupBaseUser(
        uid: Int,
        serverUrl: String,
        db: String,
        password: String
    ): Boolean {
        // 1. Try Odoo 17+ @api.model signature: args = ["base.group_user"]
        val argVariations = listOf(
            listOf("base.group_user"),
            listOf(listOf(uid), "base.group_user")
        )
        for (args in argVariations) {
            try {
                val params = mapOf(
                    "model" to "res.users",
                    "method" to "has_group",
                    "args" to args,
                    "kwargs" to emptyMap<String, Any>()
                )
                val response = odooApiService.callKw("res.users", "has_group", OdooRpcRequest(params = params))
                val rpcBody = response.body()
                if (response.isSuccessful && rpcBody?.error == null) {
                    val result = rpcBody?.result
                    if (result != null && result.isJsonPrimitive && result.asJsonPrimitive.isBoolean) {
                        return result.asJsonPrimitive.asBoolean
                    }
                }
            } catch (_: Exception) {
                // Try next variation
            }
        }

        return odooXmlRpcClient.checkHasGroup(serverUrl, db, uid, password, "base.group_user")
            .getOrDefault(false)
    }

    private suspend fun checkCanReadSaleOrders(
        uid: Int,
        serverUrl: String,
        db: String,
        password: String
    ): Boolean {
        try {
            val accessParams = mapOf(
                "model" to "sale.order",
                "method" to "check_access_rights",
                "args" to listOf("read"),
                "kwargs" to mapOf("raise_exception" to false)
            )
            val response = odooApiService.callKw("sale.order", "check_access_rights", OdooRpcRequest(params = accessParams))
            val rpcBody = response.body()
            if (response.isSuccessful && rpcBody?.error == null) {
                val result = rpcBody?.result
                if (result != null && result.isJsonPrimitive && result.asJsonPrimitive.isBoolean) {
                    return result.asJsonPrimitive.asBoolean
                }
            } else if (rpcBody?.error != null) {
                return false
            }
        } catch (_: Exception) {
            // Fall through to XML-RPC check
        }

        return odooXmlRpcClient.checkSaleOrderAccess(serverUrl, db, uid, password)
            .getOrDefault(false)
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        salesOrderDao.clearAllOrderLines()
        salesOrderDao.clearSalesOrders()
        sessionPreference.clearSession()
    }
}