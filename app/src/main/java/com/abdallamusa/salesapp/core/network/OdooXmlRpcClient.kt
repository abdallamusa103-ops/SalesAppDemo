package com.abdallamusa.salesapp.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OdooXmlRpcClient @Inject constructor() {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val xmlMediaType = "text/xml; charset=utf-8".toMediaType()

    suspend fun authenticate(
        serverUrl: String,
        db: String,
        login: String,
        password: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        val cleanUrl = serverUrl.trim().trimEnd('/')
        val endpoint = "$cleanUrl/xmlrpc/2/common"

        val xmlPayload = """
            <?xml version="1.0"?>
            <methodCall>
                <methodName>authenticate</methodName>
                <params>
                    <param><value><string>$db</string></value></param>
                    <param><value><string>$login</string></value></param>
                    <param><value><string>$password</string></value></param>
                    <param><value><struct></struct></value></param>
                </params>
            </methodCall>
        """.trimIndent()

        val request = Request.Builder()
            .url(endpoint)
            .post(xmlPayload.toRequestBody(xmlMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("XML-RPC HTTP Error: ${response.code}")
                )
            }

            // Odoo returns <value><int>UID</int></value> or <value><i4>UID</i4></value>
            val intRegex = Regex("<(?:int|i4)>(\\d+)</(?:int|i4)>")
            val match = intRegex.find(body)
            val uid = match?.groupValues?.getOrNull(1)?.toIntOrNull()

            if (uid != null && uid > 0) {
                Result.success(uid)
            } else {
                Result.failure(Exception("XML-RPC Authentication failed: Invalid credentials or database."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}