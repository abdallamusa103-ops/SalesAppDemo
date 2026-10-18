package com.abdallamusa.salesapp.features.customers.data.repository

import com.abdallamusa.salesapp.core.datastore.SessionPreference
import com.abdallamusa.salesapp.core.network.OdooApiService
import com.abdallamusa.salesapp.core.network.OdooXmlRpcClient
import com.abdallamusa.salesapp.core.network.model.OdooRpcRequest
import com.abdallamusa.salesapp.core.utils.ErrorConstants
import com.abdallamusa.salesapp.features.customers.data.local.CustomerDao
import com.abdallamusa.salesapp.features.customers.data.local.CustomerEntity
import com.abdallamusa.salesapp.features.customers.domain.model.Customer
import com.abdallamusa.salesapp.features.customers.domain.repository.CustomerRepository
import com.abdallamusa.salesapp.sync.SyncManager
import com.google.gson.JsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao,
    private val odooApiService: OdooApiService,
    private val odooXmlRpcClient: OdooXmlRpcClient,
    private val sessionPreference: SessionPreference,
    private val syncManager: SyncManager
) : CustomerRepository {

    override val customersFlow: Flow<List<Customer>> =
        customerDao.getAllCustomersFlow().map { list -> list.map { it.toDomain() } }

    override fun searchCustomersFlow(query: String): Flow<List<Customer>> =
        customerDao.searchCustomersFlow(query).map { list -> list.map { it.toDomain() } }

    override fun getCustomerByIdFlow(id: Int): Flow<Customer?> =
        customerDao.getCustomerByIdFlow(id).map { it?.toDomain() }

    override suspend fun refreshCustomers(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Push any pending offline phone edits to Odoo first
            val unsyncedCustomers = customerDao.getUnsyncedCustomers()
            for (unsynced in unsyncedCustomers) {
                val writeOutcome = executePartnerPhoneWrite(unsynced.id, unsynced.phone.orEmpty())
                if (writeOutcome.isSuccess) {
                    customerDao.markAsSynced(unsynced.id)
                } else if (ErrorConstants.isAccessError(writeOutcome.exceptionOrNull())) {
                    // Clear dirty flag so permission-rejected edits do not block refresh or loop forever
                    customerDao.markAsSynced(unsynced.id)
                }
            }

            // 2. Fetch customers from Odoo (primary: customer_rank > 0; fallback: active partners if none ranked yet)
            val rankedResult = fetchPartnersByDomain(listOf(listOf("customer_rank", ">", 0)))
            if (rankedResult.isFailure) {
                return@withContext Result.failure(rankedResult.exceptionOrNull()!!)
            }

            var jsonArray = rankedResult.getOrNull()
            if (jsonArray == null || jsonArray.size() == 0) {
                val activeResult = fetchPartnersByDomain(listOf(listOf("active", "=", true)))
                if (activeResult.isSuccess) {
                    jsonArray = activeResult.getOrNull()
                }
            }

            if (jsonArray == null) {
                return@withContext Result.success(Unit)
            }

            val remainingUnsyncedIds = customerDao.getUnsyncedCustomers().map { it.id }.toSet()

            val entities = jsonArray.mapNotNull { element ->
                val obj = element.asJsonObject
                val id = obj.get("id")?.asInt ?: return@mapNotNull null

                if (id in remainingUnsyncedIds) return@mapNotNull null

                CustomerEntity(
                    id = id,
                    name = obj.get("name")?.takeIf { !it.isJsonNull && it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString.orEmpty(),
                    phone = obj.get("phone")?.takeIf { !it.isJsonNull && it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString,
                    email = obj.get("email")?.takeIf { !it.isJsonNull && it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString,
                    street = obj.get("street")?.takeIf { !it.isJsonNull && it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString,
                    city = obj.get("city")?.takeIf { !it.isJsonNull && it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString,
                    isSynced = true,
                    lastModified = System.currentTimeMillis()
                )
            }

            customerDao.upsertCustomers(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(ErrorConstants.mapThrowableToMessage(e)))
        }
    }

    private suspend fun fetchPartnersByDomain(domain: List<Any>): Result<JsonArray> {
        val params = mapOf(
            "model" to "res.partner",
            "method" to "search_read",
            "args" to listOf(domain),
            "kwargs" to mapOf(
                "fields" to listOf("id", "name", "phone", "email", "street", "city"),
                "limit" to 100
            )
        )

        val response = odooApiService.callKw("res.partner", "search_read", OdooRpcRequest(params = params))

        if (!response.isSuccessful) {
            return Result.failure(
                Exception(ErrorConstants.httpError(response.code(), response.message()))
            )
        }

        val rpcResponse = response.body()
        if (rpcResponse?.error != null) {
            return Result.failure(Exception(rpcResponse.error.readableMessage))
        }

        val jsonArray = rpcResponse?.result?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
        return Result.success(jsonArray)
    }

    override suspend fun updatePhoneOffline(id: Int, newPhone: String): Result<Unit> = withContext(Dispatchers.IO) {
        val previousPhone = customerDao.getCustomerById(id)?.phone
        customerDao.updatePhoneOffline(id, newPhone)

        val writeResult = executePartnerPhoneWrite(id, newPhone)
        writeResult.fold(
            onSuccess = {
                customerDao.markAsSynced(id)
                Result.success(Unit)
            },
            onFailure = { error ->
                if (ErrorConstants.isAccessError(error)) {
                    customerDao.revertPhoneUpdate(id, previousPhone)
                    Result.failure(Exception(ErrorConstants.ERROR_PERMISSION_DENIED_WRITE_CUSTOMER))
                } else {
                    syncManager.triggerImmediateSync()
                    Result.failure(Exception(ErrorConstants.mapThrowableToMessage(error)))
                }
            }
        )
    }

    private suspend fun executePartnerPhoneWrite(id: Int, newPhone: String): Result<Unit> {
        // 1. Primary Strategy: Odoo JSON-RPC call_kw
        val restOutcome = tryRestWritePartnerPhone(id, newPhone)
        if (restOutcome.isSuccess) {
            return Result.success(Unit)
        }

        val restError = restOutcome.exceptionOrNull()
        if (ErrorConstants.isAccessError(restError)) {
            return Result.failure(Exception(ErrorConstants.ERROR_PERMISSION_DENIED_WRITE_CUSTOMER))
        }

        // 2. Fallback Strategy: Odoo XML-RPC execute_kw if session has credentials
        val session = sessionPreference.sessionFlow.firstOrNull()
        val uid = session?.userId
        val password = session?.password
        val serverUrl = session?.serverUrl
        val dbName = session?.dbName

        if (uid != null && uid > 0 && !password.isNullOrEmpty() && !serverUrl.isNullOrBlank() && !dbName.isNullOrBlank()) {
            val xmlOutcome = odooXmlRpcClient.writePartnerPhone(
                serverUrl = serverUrl,
                db = dbName,
                uid = uid,
                password = password,
                partnerId = id,
                newPhone = newPhone
            )
            if (xmlOutcome.isSuccess && xmlOutcome.getOrDefault(false)) {
                return Result.success(Unit)
            }
            val xmlError = xmlOutcome.exceptionOrNull()
            if (ErrorConstants.isAccessError(xmlError)) {
                return Result.failure(Exception(ErrorConstants.ERROR_PERMISSION_DENIED_WRITE_CUSTOMER))
            }
            if (xmlError != null && session.sessionId?.startsWith("xmlrpc_session_") == true) {
                return Result.failure(xmlError)
            }
        }

        return Result.failure(restError ?: Exception(ErrorConstants.ERROR_ODOO_SERVER))
    }

    private suspend fun tryRestWritePartnerPhone(id: Int, newPhone: String): Result<Unit> {
        return try {
            val writeParams = mapOf(
                "model" to "res.partner",
                "method" to "write",
                "args" to listOf(
                    listOf(id),
                    mapOf("phone" to newPhone)
                ),
                "kwargs" to emptyMap<String, Any>()
            )

            val response = odooApiService.callKw("res.partner", "write", OdooRpcRequest(params = writeParams))

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string().orEmpty()
                if (ErrorConstants.isAccessError(
                        httpCode = response.code(),
                        errorMessage = "${response.message()} $errorBody"
                    )
                ) {
                    return Result.failure(Exception(ErrorConstants.ERROR_PERMISSION_DENIED_WRITE_CUSTOMER))
                }
                return Result.failure(
                    Exception(ErrorConstants.httpError(response.code(), response.message()))
                )
            }

            val rpcResponse = response.body()
            val rpcError = rpcResponse?.error
            if (rpcError != null) {
                val combinedMsg = "${rpcError.message.orEmpty()} ${rpcError.data?.message.orEmpty()}"
                if (ErrorConstants.isAccessError(
                        errorName = rpcError.data?.name,
                        errorMessage = combinedMsg
                    )
                ) {
                    return Result.failure(Exception(ErrorConstants.ERROR_PERMISSION_DENIED_WRITE_CUSTOMER))
                }
                return Result.failure(Exception(rpcError.readableMessage))
            }

            val isSuccess = rpcResponse?.result?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
            if (isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(ErrorConstants.ERROR_ODOO_SERVER))
            }
        } catch (e: Exception) {
            if (ErrorConstants.isAccessError(e)) {
                Result.failure(Exception(ErrorConstants.ERROR_PERMISSION_DENIED_WRITE_CUSTOMER))
            } else {
                Result.failure(e)
            }
        }
    }

    private fun CustomerEntity.toDomain() = Customer(
        id = id,
        name = name,
        phone = phone,
        email = email,
        street = street,
        city = city,
        isSynced = isSynced,
        lastModified = lastModified
    )
}