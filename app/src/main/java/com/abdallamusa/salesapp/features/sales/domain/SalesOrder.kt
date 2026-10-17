package com.abdallamusa.salesapp.features.sales.domain

import java.util.Locale

data class SalesOrder(
    val id: Int,
    val name: String,
    val customerId: Int,
    val customerName: String,
    val dateOrder: String?,
    val amountTotal: Double,
    val currency: String,
    val state: String, // "draft", "sent", "sale", "cancel"
    val products: List<OrderLine> = emptyList()
) {
    val isConfirmable: Boolean
        get() = state == "draft" || state == "sent"

    val isConfirmed: Boolean
        get() = state == "sale"

    val formattedTotal: String
        get() {
            val formattedAmount = String.format(Locale.US, "%,.2f", amountTotal)
            return if (currency.isNotBlank()) "$formattedAmount $currency" else formattedAmount
        }
}
