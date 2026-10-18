package com.abdallamusa.salesapp.core.utils

import androidx.annotation.StringRes
import com.abdallamusa.salesapp.R

object CredentialValidator {

    private val emailRegex = Regex(ErrorConstants.EMAIL_REGEX_PATTERN)

    @StringRes
    fun validateEmail(email: String): Int? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> R.string.error_email_empty
            !emailRegex.matches(trimmed) -> R.string.error_email_invalid
            else -> null
        }
    }

    @StringRes
    fun validatePassword(password: String): Int? {
        return when {
            password.isEmpty() -> R.string.error_password_empty
            password.length < ErrorConstants.MIN_PASSWORD_LENGTH -> R.string.error_password_short
            else -> null
        }
    }
}
