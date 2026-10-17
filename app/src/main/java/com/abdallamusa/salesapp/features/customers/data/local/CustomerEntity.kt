package com.abdallamusa.salesapp.features.customers.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey
    val id: Int, // Permanent Odoo res.partner ID
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val street: String? = null,
    val city: String? = null,
    val isSynced: Boolean = true, // true = matches Odoo; false = modified locally offline
    val lastModified: Long = System.currentTimeMillis()
)