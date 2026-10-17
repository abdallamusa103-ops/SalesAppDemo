package com.abdallamusa.salesapp.features.customers.domain.model

data class Customer(
    val id: Int,
    val name: String,
    val phone: String?,
    val email: String?,
    val street: String?,
    val city: String?,
    val isSynced: Boolean,
    val lastModified: Long
)