package com.abdallamusa.salesapp.features.sales.data

import com.abdallamusa.salesapp.core.network.OdooApiService
import com.abdallamusa.salesapp.core.network.model.OdooRpcRequest
import com.abdallamusa.salesapp.features.sales.domain.OrderLine
import com.abdallamusa.salesapp.features.sales.domain.SalesOrder
import com.abdallamusa.salesapp.features.sales.domain.SalesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SalesRepositoryImpl @Inject constructor(
    private val salesOrderDao: SalesOrderDao,
    private val odooApiService: OdooApiService
) : SalesRepository {

    override val salesOrdersFlow: Flow<List<SalesOrder>> =
        salesOrderDao.getAllSalesOrdersFlow().map { list -> list.map { it.toDomain() } }

    override fun getOrderWithProductsFlow(orderId: Int): Flow<SalesOrder?> =
        combine(
            salesOrderDao.getSalesOrderByIdFlow(orderId),
            salesOrderDao.getOrderLinesByOrderIdFlow(orderId)
        ) { orderEntity, lineEntities ->
            orderEntity?.toDomain(lineEntities.map { it.toDomain() })
        }

    override suspend fun refreshSalesOrders(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val orderParams = mapOf(
                "model" to "sale.order",
                "method" to "search_read",
                "args" to listOf(
                    listOf(listOf("state", "in", listOf("draft", "sent", "sale")))
                ),
                "kwargs" to mapOf(
                    "fields" to listOf("id", "name", "partner_id", "date_order", "amount_total", "currency_id", "state", "order_line"),
                    "limit" to 100
                )
            )

            val orderResponse = odooApiService.callKw("sale.order", "search_read", OdooRpcRequest(params = orderParams))

            if (!orderResponse.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP Error ${orderResponse.code()}"))
            }

            val orderRpc = orderResponse.body()
            if (orderRpc?.error != null) {
                return@withContext Result.failure(Exception(orderRpc.error.message ?: "Odoo Error"))
            }

            val ordersJson = orderRpc?.result?.asJsonArray ?: return@withContext Result.success(Unit)

            val allLineIds = mutableListOf<Int>()
            val orderEntities = ordersJson.mapNotNull { element ->
                val obj = element.asJsonObject
                val id = obj.get("id")?.asInt ?: return@mapNotNull null

                var partnerId = 0
                var partnerName = "Customer"
                val partnerElem = obj.get("partner_id")
                if (partnerElem != null && partnerElem.isJsonArray) {
                    val arr = partnerElem.asJsonArray
                    if (arr.size() >= 2) {
                        partnerId = arr[0].asInt
                        partnerName = arr[1].asString
                    }
                }

                // Dynamic Currency from Odoo's currency_id: [14, "LE"]
                var currencyName = ""
                val currencyElem = obj.get("currency_id")
                if (currencyElem != null && currencyElem.isJsonArray) {
                    val arr = currencyElem.asJsonArray
                    if (arr.size() >= 2) {
                        currencyName = arr[1].asString
                    }
                }

                // Collect order line IDs
                val lineIdsElem = obj.get("order_line")
                if (lineIdsElem != null && lineIdsElem.isJsonArray) {
                    lineIdsElem.asJsonArray.forEach { lineIdElem ->
                        if (!lineIdElem.isJsonNull) {
                            allLineIds.add(lineIdElem.asInt)
                        }
                    }
                }

                SalesOrderEntity(
                    id = id,
                    name = obj.get("name")?.takeIf { !it.isJsonNull }?.asString.orEmpty(),
                    customerId = partnerId,
                    customerName = partnerName,
                    dateOrder = obj.get("date_order")?.takeIf { !it.isJsonNull }?.asString,
                    amountTotal = obj.get("amount_total")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0,
                    currency = currencyName,
                    state = obj.get("state")?.takeIf { !it.isJsonNull }?.asString ?: "draft"
                )
            }

            salesOrderDao.upsertSalesOrders(orderEntities)

            // Fetch product lines if any exist
            if (allLineIds.isNotEmpty()) {
                fetchAndCacheOrderLines(allLineIds)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchAndCacheOrderLines(lineIds: List<Int>) {
        try {
            val lineParams = mapOf(
                "model" to "sale.order.line",
                "method" to "search_read",
                "args" to listOf(
                    listOf(listOf("id", "in", lineIds))
                ),
                "kwargs" to mapOf(
                    "fields" to listOf("id", "order_id", "name", "product_uom_qty")
                )
            )

            val response = odooApiService.callKw("sale.order.line", "search_read", OdooRpcRequest(params = lineParams))
            val linesJson = response.body()?.result?.asJsonArray ?: return

            val lineEntities = linesJson.mapNotNull { element ->
                val obj = element.asJsonObject
                val id = obj.get("id")?.asInt ?: return@mapNotNull null

                var orderId = 0
                val orderElem = obj.get("order_id")
                if (orderElem != null && orderElem.isJsonArray) {
                    val arr = orderElem.asJsonArray
                    if (arr.size() >= 1) {
                        orderId = arr[0].asInt
                    }
                }

                val productName = obj.get("name")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
                // Clean Odoo multiline descriptions to just first line for clean UI
                val cleanProductName = productName.lines().firstOrNull()?.trim() ?: productName

                OrderLineEntity(
                    id = id,
                    orderId = orderId,
                    productName = cleanProductName,
                    quantity = obj.get("product_uom_qty")?.takeIf { !it.isJsonNull }?.asDouble ?: 1.0
                )
            }

            salesOrderDao.upsertOrderLines(lineEntities)
        } catch (_: Exception) {
            // Fail silently on lines; orders are already persisted
        }
    }

    override suspend fun confirmOrder(orderId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf(
                "model" to "sale.order",
                "method" to "action_confirm",
                "args" to listOf(listOf(orderId)),
                "kwargs" to emptyMap<String, Any>()
            )

            val response = odooApiService.callKw("sale.order", "action_confirm", OdooRpcRequest(params = params))

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP Error ${response.code()}"))
            }

            val rpcResponse = response.body()
            if (rpcResponse?.error != null) {
                return@withContext Result.failure(Exception(rpcResponse.error.message ?: "Confirm Failed"))
            }

            // Room update
            salesOrderDao.updateOrderState(orderId, "sale")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun SalesOrderEntity.toDomain(products: List<OrderLine> = emptyList()) = SalesOrder(
        id = id,
        name = name,
        customerId = customerId,
        customerName = customerName,
        dateOrder = dateOrder,
        amountTotal = amountTotal,
        currency = currency,
        state = state,
        products = products
    )

    private fun OrderLineEntity.toDomain() = OrderLine(
        id = id,
        orderId = orderId,
        productName = productName,
        quantity = quantity
    )
}
