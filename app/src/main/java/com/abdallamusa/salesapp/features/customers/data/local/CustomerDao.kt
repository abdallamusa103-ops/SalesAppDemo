package com.abdallamusa.salesapp.features.customers.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomersFlow(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCustomersFlow(query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun getCustomerByIdFlow(id: Int): Flow<CustomerEntity?>

    // Used by WorkManager to find records modified offline
    @Query("SELECT * FROM customers WHERE isSynced = 0")
    suspend fun getUnsyncedCustomers(): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomers(customers: List<CustomerEntity>)

    @Query("UPDATE customers SET phone = :newPhone, isSynced = 0, lastModified = :timestamp WHERE id = :id")
    suspend fun updatePhoneOffline(id: Int, newPhone: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)
}