package be.ap.nowaste.data.store

// The value is what we store in the "category" field of stores/{storeId}.
enum class StoreCategory(val value: String) {
    BAKERY("bakery"),
    SUPERMARKET("supermarket"),
    RESTAURANT("restaurant"),
    CAFE("cafe"),
    OTHER("other");

    companion object {
        fun fromValue(value: String?): StoreCategory? = entries.find { it.value == value }
    }
}

data class Store(
    val id: String,
    val ownerId: String,
    val name: String,
    val category: StoreCategory,
    val latitude: Double,
    val longitude: Double,
    val street: String,
    val postcode: String,
    val city: String,
) {
    val address: String get() = formatAddress(street, postcode, city)
}

/** One line, as shown to customers: "Meir 1, 2000 Antwerpen". */
fun formatAddress(street: String, postcode: String, city: String): String =
    listOf(street.trim(), listOf(postcode.trim(), city.trim()).filter { it.isNotEmpty() }.joinToString(" "))
        .filter { it.isNotEmpty() }
        .joinToString(", ")
