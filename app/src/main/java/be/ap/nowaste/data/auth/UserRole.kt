package be.ap.nowaste.data.auth

// The value is what we store in the "role" field of users/{uid}.
enum class UserRole(val value: String) {
    CUSTOMER("customer"),
    MERCHANT("merchant");

    companion object {
        fun fromValue(value: String?): UserRole? = entries.find { it.value == value }
    }
}
