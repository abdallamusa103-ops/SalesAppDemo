package com.abdallamusa.salesapp.features.sales.domain

import kotlinx.coroutines.flow.Flow

interface SalesRepository {
    val salesOrdersFlow: Flow<List<SalesOrder>>

    fun getOrderWithProductsFlow(orderId: Int): Flow<SalesOrder?>

    suspend fun refreshSalesOrders(): Result<Unit>

    suspend fun confirmOrder(orderId: Int): Result<Unit>
}
