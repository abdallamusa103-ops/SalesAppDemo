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
    val id: Int, // Permanent Odoo sale.order ID
    val name: String, // Order reference (e.g. "SO001")
    val customerId: Int, // ID from Odoo's partner_id[0]
    val customerName: String, // Name from Odoo's partner_id[1]
    val dateOrder: String?,
    val amountTotal: Double,
    val state: String // "draft", "sent", "sale", "cancel"
)