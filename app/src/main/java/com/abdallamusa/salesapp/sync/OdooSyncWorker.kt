package com.abdallamusa.salesapp.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abdallamusa.salesapp.core.datastore.SessionPreference
import com.abdallamusa.salesapp.core.network.OdooApiService
import com.abdallamusa.salesapp.core.network.OdooXmlRpcClient
import com.abdallamusa.salesapp.core.network.model.OdooRpcRequest
import com.abdallamusa.salesapp.core.utils.ErrorConstants
import com.abdallamusa.salesapp.features.customers.data.local.CustomerDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

@HiltWorker
class OdooSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val customerDao: CustomerDao,
    private val odooApiService: OdooApiService,
    private val odooXmlRpcClient: OdooXmlRpcClient,
    private val sessionPreference: SessionPreference
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val unsyncedCustomers = customerDao.getUnsyncedCustomers()

            if (unsyncedCustomers.isEmpty()) {
                return@withContext Result.success()
            }

            val session = sessionPreference.sessionFlow.firstOrNull()

            for (customer in unsyncedCustomers) {
                val phoneValue = customer.phone.orEmpty()
                val writeParams = mapOf(
                    "model" to "res.partner",
                    "method" to "write",
                    "args" to listOf(
                        listOf(customer.id),
                        mapOf("phone" to phoneValue)
                    ),
                    "kwargs" to emptyMap<String, Any>()
                )

                val response = try {
                    odooApiService.callKw("res.partner", "write", OdooRpcRequest(params = writeParams))
                } catch (_: Exception) {
                    null
                }

                val rpcBody = response?.body()
                val rpcError = rpcBody?.error

                if (response != null && response.isSuccessful && rpcError == null) {
                    customerDao.markAsSynced(customer.id)
                    continue
                }

                val errorBodyStr = response?.errorBody()?.string().orEmpty()
                val combinedErrorMsg = "${response?.message().orEmpty()} $errorBodyStr ${rpcError?.message.orEmpty()} ${rpcError?.data?.message.orEmpty()}"

                if (ErrorConstants.isAccessError(
                        httpCode = response?.code(),
                        errorName = rpcError?.data?.name,
                        errorMessage = combinedErrorMsg
                    )
                ) {
                    // Permission denied by ACL: clear dirty flag so worker does not loop infinitely
                    customerDao.markAsSynced(customer.id)
                    continue
                }

                // Fallback to XML-RPC if REST endpoint failed for non-ACL reason
                val uid = session?.userId
                val password = session?.password
                val serverUrl = session?.serverUrl
                val dbName = session?.dbName

                if (uid != null && uid > 0 && !password.isNullOrEmpty() && !serverUrl.isNullOrBlank() && !dbName.isNullOrBlank()) {
                    val xmlResult = odooXmlRpcClient.writePartnerPhone(
                        serverUrl = serverUrl,
                        db = dbName,
                        uid = uid,
                        password = password,
                        partnerId = customer.id,
                        newPhone = phoneValue
                    )
                    if (xmlResult.isSuccess && xmlResult.getOrDefault(false)) {
                        customerDao.markAsSynced(customer.id)
                        continue
                    }
                    if (ErrorConstants.isAccessError(xmlResult.exceptionOrNull())) {
                        customerDao.markAsSynced(customer.id)
                        continue
                    }
                }

                return@withContext Result.retry()
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "OdooSyncWorker"
    }
}
