package com.abdallamusa.salesapp.core.network

import com.abdallamusa.salesapp.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicUrlInterceptor @Inject constructor() : Interceptor {

    @Volatile
    private var cachedBaseUrl: String? = null

    fun updateBaseUrl(url: String) {
        cachedBaseUrl = url.trim().trimEnd('/')
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val baseUrl = cachedBaseUrl
            ?: BuildConfig.ODOO_BASE_URL.trim().trimEnd('/')

        if (baseUrl.isBlank()) {
            return chain.proceed(originalRequest)
        }

        val targetHttpUrl = baseUrl.toHttpUrlOrNull() ?: return chain.proceed(originalRequest)

        val newUrl = originalRequest.url.newBuilder()
            .scheme(targetHttpUrl.scheme)
            .host(targetHttpUrl.host)
            .port(targetHttpUrl.port)
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}