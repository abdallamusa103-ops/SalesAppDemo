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
