package be.ap.nowaste.data.geocoding

import be.ap.nowaste.data.local.CachedAddress
import java.util.Locale

data class Address(
    val street: String,
    val postcode: String,
    val city: String,
    val displayName: String,
) {
    val isEmpty: Boolean get() = street.isBlank() && postcode.isBlank() && city.isBlank()
}

// Zoom levels from the assignment: 18 = building (H2), 10 = municipality (K2)
const val ZOOM_BUILDING = 18
const val ZOOM_CITY = 10

/** 4 decimals is about 10 m. The zoom is part of the key, because zoom 10 and 18 give different results. */
fun addressCacheKey(lat: Double, lon: Double, zoom: Int): String =
    String.format(Locale.ROOT, "%.4f,%.4f@%d", lat, lon, zoom)

fun NominatimResponse.toAddress(): Address {
    val address = address
    return Address(
        street = listOfNotNull(address?.road, address?.houseNumber).joinToString(" "),
        postcode = address?.postcode.orEmpty(),
        // Depending on the place, Nominatim fills city, town or village
        city = address?.city ?: address?.town ?: address?.village ?: address?.municipality.orEmpty(),
        displayName = displayName.orEmpty(),
    )
}

fun CachedAddress.toAddress() = Address(street, postcode, city, displayName)

fun Address.toCachedAddress(key: String, fetchedAt: Long) =
    CachedAddress(key, displayName, street, postcode, city, fetchedAt)
