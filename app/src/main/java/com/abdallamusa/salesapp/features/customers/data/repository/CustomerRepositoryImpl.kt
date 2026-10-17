package com.abdallamusa.salesapp.features.customers.data.repository

import com.abdallamusa.salesapp.core.network.OdooApiService
import com.abdallamusa.salesapp.core.network.model.OdooRpcRequest
import com.abdallamusa.salesapp.features.customers.data.local.CustomerDao
import com.abdallamusa.salesapp.features.customers.data.local.CustomerEntity
import com.abdallamusa.salesapp.features.customers.domain.model.Customer
import com.abdallamusa.salesapp.features.customers.domain.repository.CustomerRepository
import com.abdallamusa.salesapp.sync.SyncManager
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
            val params = mapOf(
                "model" to "res.partner",
                "method" to "search_read",
                "args" to listOf(
                    listOf(listOf("customer_rank", ">", 0)) // Only actual customers
                ),
                "kwargs" to mapOf(
                    "fields" to listOf("id", "name", "phone", "email", "street", "city"),
                    "limit" to 100
                )
            )

            val response = odooApiService.callKw("res.partner", "search_read", OdooRpcRequest(params = params))

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP Error ${response.code()}"))
            }

            val rpcResponse = response.body()
            if (rpcResponse?.error != null) {
                return@withContext Result.failure(Exception(rpcResponse.error.message ?: "Odoo Query Error"))
            }

            val jsonArray = rpcResponse?.result?.asJsonArray ?: return@withContext Result.success(Unit)

            val existingUnsyncedIds = customerDao.getUnsyncedCustomers().map { it.id }.toSet()

            val entities = jsonArray.mapNotNull { element ->
                val obj = element.asJsonObject
                val id = obj.get("id")?.asInt ?: return@mapNotNull null

                // Never overwrite local un-synced edits with server data
                if (id in existingUnsyncedIds) return@mapNotNull null

                CustomerEntity(
                    id = id,
                    name = obj.get("name")?.takeIf { !it.isJsonNull }?.asString.orEmpty(),
                    phone = obj.get("phone")?.takeIf { !it.isJsonNull }?.asString,
                    email = obj.get("email")?.takeIf { !it.isJsonNull }?.asString,
                    street = obj.get("street")?.takeIf { !it.isJsonNull }?.asString,
                    city = obj.get("city")?.takeIf { !it.isJsonNull }?.asString,
                    isSynced = true,
                    lastModified = System.currentTimeMillis()
                )
            }

            customerDao.upsertCustomers(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePhoneOffline(id: Int, newPhone: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            customerDao.updatePhoneOffline(id, newPhone)
            syncManager.triggerImmediateSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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