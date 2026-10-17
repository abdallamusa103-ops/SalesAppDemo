package com.abdallamusa.salesapp.core.di

import android.content.Context
import androidx.room.Room
import com.abdallamusa.salesapp.core.database.SalesDatabase
import com.abdallamusa.salesapp.features.customers.data.local.CustomerDao
import com.abdallamusa.salesapp.features.sales.data.SalesOrderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.jvm.java

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): SalesDatabase {
        return Room.databaseBuilder(
                context,
                SalesDatabase::class.java,
                "odoo_sales_app.db"
            ).fallbackToDestructiveMigration(false).build()
    }

    @Provides
    @Singleton
    fun provideCustomerDao(database: SalesDatabase): CustomerDao {
        return database.customerDao()
    }

    @Provides
    @Singleton
    fun provideSalesOrderDao(database: SalesDatabase): SalesOrderDao {
        return database.salesOrderDao()
    }
}