package com.abdallamusa.salesapp.core.network

import com.abdallamusa.salesapp.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OdooServerConfig @Inject constructor(
    private val odooApiService: OdooApiService
) {

    val baseUrl: String
        get() {
            val raw = BuildConfig.ODOO_BASE_URL.trim().trimEnd('/')
            val parsed = raw.toHttpUrlOrNull() ?: return raw
            val portPart = if (
                (parsed.scheme == "http" && parsed.port != 80) ||
                (parsed.scheme == "https" && parsed.port != 443)
            ) {
                ":${parsed.port}"
            } else {
                ""
            }
            return "${parsed.scheme}://${parsed.host}$portPart"
        }

    suspend fun resolveDatabaseName(): String {
        val parsedUrl = baseUrl.toHttpUrlOrNull()
        val host = parsedUrl?.host.orEmpty()

        if (host.endsWith(".odoo.com", ignoreCase = true)) {
            val subdomain = host.substringBefore(".odoo.com")
            if (subdomain.isNotBlank() && !subdomain.contains(".")) {
                return subdomain
            }
        }

        try {
            val response = odooApiService.listDatabases()
            val array = response.body()?.result?.takeIf { it.isJsonArray }?.asJsonArray
            if (array != null && array.size() > 0) {
                val detected = array.get(0)?.takeIf { !it.isJsonNull }?.asString
                if (!detected.isNullOrBlank()) {
                    return detected
                }
            }
        } catch (_: Exception) {
            // Fall through to BuildConfig fallback when listing is disabled or unreachable
        }

        return BuildConfig.ODOO_DB.trim()
    }
}
