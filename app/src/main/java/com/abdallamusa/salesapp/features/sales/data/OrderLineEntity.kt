package com.abdallamusa.salesapp.features.sales.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales_order_lines",
    indices = [Index("orderId")]
)
data class OrderLineEntity(
    @PrimaryKey
    val id: Int, // Odoo sale.order.line ID
    val orderId: Int,
    val productName: String,
    val quantity: Double
)
