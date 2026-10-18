package com.abdallamusa.salesapp.core.network

import com.abdallamusa.salesapp.BuildConfig
import com.abdallamusa.salesapp.core.datastore.SessionPreference
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicUrlInterceptor @Inject constructor(
    private val sessionPreference: SessionPreference
) : Interceptor {

    @Volatile
    private var cachedBaseUrl: String? = null

    fun updateBaseUrl(url: String) {
        cachedBaseUrl = url.trim().trimEnd('/')
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val baseUrl = cachedBaseUrl ?: runBlocking {
            sessionPreference.sessionFlow.firstOrNull()?.serverUrl?.trim()?.trimEnd('/')
        } ?: BuildConfig.ODOO_BASE_URL.trim().trimEnd('/')

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