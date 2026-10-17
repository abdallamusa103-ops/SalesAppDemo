package com.abdallamusa.salesapp.features.sales.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales_orders",
    indices = [Index("customerId")]
)
data class SalesOrderEntity(
    @PrimaryKey
    val id: Int, // Odoo sale.order ID
    val name: String, // Order Number (e.g. "SO001")
    val customerId: Int, // partner_id[0]
    val customerName: String, // partner_id[1]
    val dateOrder: String?,
    val amountTotal: Double,
    val currency: String = "", // Dynamic currency (e.g. "LE")
    val state: String // "draft", "sent", "sale", "cancel"
)