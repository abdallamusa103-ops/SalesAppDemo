package com.abdallamusa.salesapp.features.sales.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesOrderDao {

    @Query("SELECT * FROM sales_orders ORDER BY id DESC")
    fun getAllSalesOrdersFlow(): Flow<List<SalesOrderEntity>>

    @Query("SELECT * FROM sales_orders WHERE id = :id LIMIT 1")
    fun getSalesOrderByIdFlow(id: Int): Flow<SalesOrderEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSalesOrders(orders: List<SalesOrderEntity>)

    @Query("UPDATE sales_orders SET state = :newState WHERE id = :id")
    suspend fun updateOrderState(id: Int, newState: String)

    @Query("DELETE FROM sales_orders")
    suspend fun clearSalesOrders()
}