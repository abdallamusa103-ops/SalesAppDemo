package com.abdallamusa.salesapp.core.network

import com.abdallamusa.salesapp.core.utils.ErrorConstants
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
                    <param><value><string>${escapeXml(db)}</string></value></param>
                    <param><value><string>${escapeXml(login)}</string></value></param>
                    <param><value><string>${escapeXml(password)}</string></value></param>
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

    suspend fun checkHasGroup(
        serverUrl: String,
        db: String,
        uid: Int,
        password: String,
        groupExtId: String = "base.group_user"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = serverUrl.trim().trimEnd('/')
        val endpoint = "$cleanUrl/xmlrpc/2/object"

        // 1. Try Odoo 17/18/19 @api.model signature: args = [groupExtId]
        val singleArgXml = """
            <?xml version="1.0"?>
            <methodCall>
                <methodName>execute_kw</methodName>
                <params>
                    <param><value><string>${escapeXml(db)}</string></value></param>
                    <param><value><int>$uid</int></value></param>
                    <param><value><string>${escapeXml(password)}</string></value></param>
                    <param><value><string>res.users</string></value></param>
                    <param><value><string>has_group</string></value></param>
                    <param>
                        <value>
                            <array>
                                <data>
                                    <value><string>${escapeXml(groupExtId)}</string></value>
                                </data>
                            </array>
                        </value>
                    </param>
                </params>
            </methodCall>
        """.trimIndent()

        // 2. Fallback to legacy Odoo <=16 signature: args = [[uid], groupExtId]
        val legacyArgXml = """
            <?xml version="1.0"?>
            <methodCall>
                <methodName>execute_kw</methodName>
                <params>
                    <param><value><string>${escapeXml(db)}</string></value></param>
                    <param><value><int>$uid</int></value></param>
                    <param><value><string>${escapeXml(password)}</string></value></param>
                    <param><value><string>res.users</string></value></param>
                    <param><value><string>has_group</string></value></param>
                    <param>
                        <value>
                            <array>
                                <data>
                                    <value>
                                        <array>
                                            <data>
                                                <value><int>$uid</int></value>
                                            </data>
                                        </array>
                                    </value>
                                    <value><string>${escapeXml(groupExtId)}</string></value>
                                </data>
                            </array>
                        </value>
                    </param>
                </params>
            </methodCall>
        """.trimIndent()

        val boolRegex = Regex("<boolean>\\s*(1|true)\\s*</boolean>", RegexOption.IGNORE_CASE)

        for (payload in listOf(singleArgXml, legacyArgXml)) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .post(payload.toRequestBody(xmlMediaType))
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful && !body.contains("<fault>", ignoreCase = true)) {
                    return@withContext Result.success(boolRegex.containsMatchIn(body))
                }
            } catch (_: Exception) {
                // Try next signature
            }
        }

        Result.success(false)
    }

    suspend fun checkSaleOrderAccess(
        serverUrl: String,
        db: String,
        uid: Int,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = serverUrl.trim().trimEnd('/')
        val endpoint = "$cleanUrl/xmlrpc/2/object"

        val xmlPayload = """
            <?xml version="1.0"?>
            <methodCall>
                <methodName>execute_kw</methodName>
                <params>
                    <param><value><string>${escapeXml(db)}</string></value></param>
                    <param><value><int>$uid</int></value></param>
                    <param><value><string>${escapeXml(password)}</string></value></param>
                    <param><value><string>sale.order</string></value></param>
                    <param><value><string>check_access_rights</string></value></param>
                    <param>
                        <value>
                            <array>
                                <data>
                                    <value><string>read</string></value>
                                </data>
                            </array>
                        </value>
                    </param>
                    <param>
                        <value>
                            <struct>
                                <member>
                                    <name>raise_exception</name>
                                    <value><boolean>0</boolean></value>
                                </member>
                            </struct>
                        </value>
                    </param>
                </params>
            </methodCall>
        """.trimIndent()

        try {
            val request = Request.Builder()
                .url(endpoint)
                .post(xmlPayload.toRequestBody(xmlMediaType))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful || body.contains("<fault>", ignoreCase = true)) {
                return@withContext Result.success(false)
            }

            val boolRegex = Regex("<boolean>\\s*(1|true)\\s*</boolean>", RegexOption.IGNORE_CASE)
            Result.success(boolRegex.containsMatchIn(body))
        } catch (_: Exception) {
            Result.success(false)
        }
    }

    suspend fun writePartnerPhone(
        serverUrl: String,
        db: String,
        uid: Int,
        password: String,
        partnerId: Int,
        newPhone: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = serverUrl.trim().trimEnd('/')
        val endpoint = "$cleanUrl/xmlrpc/2/object"

        val xmlPayload = """
            <?xml version="1.0"?>
            <methodCall>
                <methodName>execute_kw</methodName>
                <params>
                    <param><value><string>${escapeXml(db)}</string></value></param>
                    <param><value><int>$uid</int></value></param>
                    <param><value><string>${escapeXml(password)}</string></value></param>
                    <param><value><string>res.partner</string></value></param>
                    <param><value><string>write</string></value></param>
                    <param>
                        <value>
                            <array>
                                <data>
                                    <value>
                                        <array>
                                            <data>
                                                <value><int>$partnerId</int></value>
                                            </data>
                                        </array>
                                    </value>
                                    <value>
                                        <struct>
                                            <member>
                                                <name>phone</name>
                                                <value><string>${escapeXml(newPhone)}</string></value>
                                            </member>
                                        </struct>
                                    </value>
                                </data>
                            </array>
                        </value>
                    </param>
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
                if (ErrorConstants.isAccessError(httpCode = response.code, errorMessage = body)) {
                    return@withContext Result.failure(
                        Exception(ErrorConstants.ERROR_PERMISSION_DENIED_WRITE_CUSTOMER)
                    )
                }
                return@withContext Result.failure(
                    IOException(ErrorConstants.httpError(response.code, response.message))
                )
            }

            if (body.contains("<fault>", ignoreCase = true)) {
                if (ErrorConstants.isAccessError(errorMessage = body)) {
                    return@withContext Result.failure(
                        Exception(ErrorConstants.ERROR_PERMISSION_DENIED_WRITE_CUSTOMER)
                    )
                }
                return@withContext Result.failure(
                    Exception(ErrorConstants.ERROR_ODOO_SERVER)
                )
            }

            val boolRegex = Regex("<boolean>\\s*(1|true)\\s*</boolean>", RegexOption.IGNORE_CASE)
            Result.success(boolRegex.containsMatchIn(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}