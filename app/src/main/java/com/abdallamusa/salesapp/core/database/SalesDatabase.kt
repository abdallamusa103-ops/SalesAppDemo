package com.abdallamusa.salesapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.abdallamusa.salesapp.features.customers.data.CustomerDao
import com.abdallamusa.salesapp.features.customers.data.CustomerEntity
import com.abdallamusa.salesapp.features.sales.data.SalesOrderDao
import com.abdallamusa.salesapp.features.sales.data.SalesOrderEntity

@Database(
    entities = [
        CustomerEntity::class,
        SalesOrderEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SalesDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun salesOrderDao(): SalesOrderDao
}