package com.abdallamusa.salesapp.features.customers.data.repository

import com.abdallamusa.salesapp.core.network.OdooApiService
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao,
    private val odooApiService: OdooApiService,
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
                val writeParams = mapOf(
                    "model" to "res.partner",
                    "method" to "write",
                    "args" to listOf(
                        listOf(unsynced.id),
                        mapOf("phone" to (unsynced.phone ?: ""))
                    ),
                    "kwargs" to emptyMap<String, Any>()
                )
                val writeResponse = odooApiService.callKw("res.partner", "write", OdooRpcRequest(params = writeParams))
                if (writeResponse.isSuccessful && writeResponse.body()?.error == null) {
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
        customerDao.updatePhoneOffline(id, newPhone)

        try {
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
                syncManager.triggerImmediateSync()
                return@withContext Result.failure(
                    Exception(ErrorConstants.httpError(response.code(), response.message()))
                )
            }

            val rpcResponse = response.body()
            if (rpcResponse?.error != null) {
                syncManager.triggerImmediateSync()
                return@withContext Result.failure(Exception(rpcResponse.error.readableMessage))
            }

            val isSuccess = rpcResponse?.result?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
            if (isSuccess) {
                customerDao.markAsSynced(id)
            } else {
                syncManager.triggerImmediateSync()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            syncManager.triggerImmediateSync()
            Result.failure(Exception(ErrorConstants.mapThrowableToMessage(e)))
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