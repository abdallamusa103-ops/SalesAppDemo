package com.abdallamusa.salesapp.core.utils

object ErrorConstants {
    const val MIN_PASSWORD_LENGTH = 4
    const val EMAIL_REGEX_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"

    const val ERROR_AUTH_FAILED = "Authentication failed"
    const val ERROR_ACCESS_DENIED = "Access Denied: Invalid username or password"
    const val ERROR_ODOO_SERVER = "Odoo Server Error"
    const val ERROR_NETWORK_UNAVAILABLE = "Network error: Unable to reach Odoo server"

    fun httpError(code: Int, message: String? = null): String {
        return if (!message.isNullOrBlank()) {
            "HTTP Error $code: $message"
        } else {
            "HTTP Error $code"
        }
    }
}
