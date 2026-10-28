package be.ap.nowaste.ui.auth

import androidx.annotation.StringRes
import be.ap.nowaste.R
import be.ap.nowaste.data.auth.AuthError

// Firebase Auth rejects shorter passwords
const val MIN_PASSWORD_LENGTH = 6

// Plain regex instead of android.util.Patterns, so it also runs in unit tests
private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

fun isValidPassword(password: String): Boolean = password.length >= MIN_PASSWORD_LENGTH

@StringRes
fun AuthError.messageRes(): Int = when (this) {
    AuthError.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
    AuthError.EMAIL_IN_USE -> R.string.auth_error_email_in_use
    AuthError.WEAK_PASSWORD -> R.string.auth_error_weak_password
    AuthError.NO_INTERNET -> R.string.error_no_internet
    AuthError.PROFILE_MISSING -> R.string.auth_error_profile_missing
    AuthError.UNKNOWN -> R.string.error_unknown
}
