package com.abdallamusa.salesapp.core.network

import com.abdallamusa.salesapp.core.datastore.SessionPreference
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionInterceptor @Inject constructor(
    private val sessionPreference: SessionPreference
) : Interceptor {

    @Volatile
    private var activeSessionId: String? = null

    fun setActiveSessionId(sessionId: String?) {
        activeSessionId = sessionId
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        val sessionId = activeSessionId?.takeIf { it.isNotBlank() } ?: runBlocking {
            sessionPreference.sessionFlow.firstOrNull()?.sessionId
        }

        if (!sessionId.isNullOrBlank()) {
            builder.header("Cookie", "session_id=$sessionId")
        }

        return chain.proceed(builder.build())
    }
}