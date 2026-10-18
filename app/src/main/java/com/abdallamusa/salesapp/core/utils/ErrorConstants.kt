package com.abdallamusa.salesapp.core.utils

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorConstants {
    const val MIN_PASSWORD_LENGTH = 4
    const val EMAIL_REGEX_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"

    const val ERROR_AUTH_FAILED = "Authentication failed"
    const val ERROR_ACCESS_DENIED = "Access Denied: Invalid username or password"
    const val ERROR_ODOO_SERVER = "Odoo Server Error"
    const val ERROR_NETWORK_UNAVAILABLE = "No internet connection. Please check your network and try again."
    const val ERROR_CONNECTION_TIMEOUT = "Connection timed out. Please try again."
    const val ERROR_PERMISSION_DENIED_WRITE_CUSTOMER =
        "Permission denied: Your account does not have write access to update customer records."

    fun isAccessError(
        httpCode: Int? = null,
        errorName: String? = null,
        errorMessage: String? = null
    ): Boolean {
        if (httpCode == 401 || httpCode == 403) return true
        if (errorName?.contains("AccessError", ignoreCase = true) == true) return true
        if (errorName?.contains("AccessDenied", ignoreCase = true) == true) return true
        val msg = errorMessage.orEmpty()
        return msg.contains("AccessError", ignoreCase = true) ||
            msg.contains("Access Denied", ignoreCase = true) ||
            msg.contains("not allowed to modify", ignoreCase = true) ||
            msg.contains("not allowed to", ignoreCase = true) ||
            msg == ERROR_PERMISSION_DENIED_WRITE_CUSTOMER
    }

    fun isAccessError(throwable: Throwable?): Boolean {
        if (throwable == null) return false
        return isAccessError(errorMessage = throwable.message)
    }

    fun httpError(code: Int, message: String? = null): String {
        return if (!message.isNullOrBlank()) {
            "HTTP Error $code: $message"
        } else {
            "HTTP Error $code"
        }
    }

    fun mapThrowableToMessage(throwable: Throwable): String {
        val rawMessage = throwable.message.orEmpty()
        return when {
            isAccessError(errorMessage = rawMessage) -> {
                if (rawMessage == ERROR_PERMISSION_DENIED_WRITE_CUSTOMER) {
                    ERROR_PERMISSION_DENIED_WRITE_CUSTOMER
                } else {
                    rawMessage
                }
            }
            throwable is UnknownHostException ||
                throwable is ConnectException ||
                rawMessage.contains("Unable to resolve host", ignoreCase = true) ||
                rawMessage.contains("No address associated with hostname", ignoreCase = true) ||
                rawMessage.contains("Failed to connect", ignoreCase = true) ||
                rawMessage.contains("Network is unreachable", ignoreCase = true) -> {
                ERROR_NETWORK_UNAVAILABLE
            }
            throwable is SocketTimeoutException ||
                rawMessage.contains("timeout", ignoreCase = true) ||
                rawMessage.contains("timed out", ignoreCase = true) -> {
                ERROR_CONNECTION_TIMEOUT
            }
            throwable is IOException -> {
                ERROR_NETWORK_UNAVAILABLE
            }
            rawMessage.isNotBlank() -> rawMessage
            else -> ERROR_NETWORK_UNAVAILABLE
        }
    }
}
