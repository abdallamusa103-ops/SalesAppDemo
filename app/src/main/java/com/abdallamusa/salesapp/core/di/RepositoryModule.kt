package com.abdallamusa.salesapp.core.di

import com.abdallamusa.salesapp.features.auth.data.AuthRepositoryImpl
import com.abdallamusa.salesapp.features.auth.domain.AuthRepository
import com.abdallamusa.salesapp.features.customers.data.repository.CustomerRepositoryImpl
import com.abdallamusa.salesapp.features.customers.domain.repository.CustomerRepository
import com.abdallamusa.salesapp.features.sales.data.SalesRepositoryImpl
import com.abdallamusa.salesapp.features.sales.domain.SalesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCustomerRepository(
        impl: CustomerRepositoryImpl
    ): CustomerRepository

    @Binds
    @Singleton
    abstract fun bindSalesRepository(
        impl: SalesRepositoryImpl
    ): SalesRepository
}