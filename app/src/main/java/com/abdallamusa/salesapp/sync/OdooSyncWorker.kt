package com.abdallamusa.salesapp.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abdallamusa.salesapp.core.network.OdooApiService
import com.abdallamusa.salesapp.core.network.model.OdooRpcRequest
import com.abdallamusa.salesapp.features.customers.data.local.CustomerDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class OdooSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val customerDao: CustomerDao,
    private val odooApiService: OdooApiService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val unsyncedCustomers = customerDao.getUnsyncedCustomers()

            if (unsyncedCustomers.isEmpty()) {
                return@withContext Result.success()
            }

            for (customer in unsyncedCustomers) {
                val writeParams = mapOf(
                    "model" to "res.partner",
                    "method" to "write",
                    "args" to listOf(
                        listOf(customer.id),
                        mapOf("phone" to (customer.phone ?: ""))
                    ),
                    "kwargs" to emptyMap<String, Any>()
                )

                val response = odooApiService.callKw("res.partner", "write", OdooRpcRequest(params = writeParams))

                if (response.isSuccessful && response.body()?.error == null) {
                    customerDao.markAsSynced(customer.id)
                } else {
                    return@withContext Result.retry()
                }
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
