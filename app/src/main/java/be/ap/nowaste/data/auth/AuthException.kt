package be.ap.nowaste.data.auth

enum class AuthError {
    INVALID_CREDENTIALS,
    EMAIL_IN_USE,
    WEAK_PASSWORD,
    NO_INTERNET,
    PROFILE_MISSING,
    UNKNOWN,
}

// Thrown by AuthRepository, so ViewModels never have to know Firebase exception types.
class AuthException(val error: AuthError, cause: Throwable? = null) : Exception(cause)
