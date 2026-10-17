package com.abdallamusa.salesapp.features.sales.domain

data class OrderLine(
    val id: Int,
    val orderId: Int,
    val productName: String,
    val quantity: Double
)
