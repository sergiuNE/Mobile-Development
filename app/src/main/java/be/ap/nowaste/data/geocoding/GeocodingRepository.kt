package be.ap.nowaste.data.geocoding

import be.ap.nowaste.data.local.CachedAddressDao

class GeocodingRepository(
    private val cache: CachedAddressDao,
    private val api: NominatimApi = NominatimClient.api,
) {
    /** Address for a location: from the Room cache when we already asked it, otherwise from Nominatim. */
    suspend fun reverseGeocode(lat: Double, lon: Double, zoom: Int = ZOOM_BUILDING): Address {
        val key = addressCacheKey(lat, lon, zoom)
        cache.get(key)?.let { return it.toAddress() }

        val address = NominatimClient.throttled { api.reverse(lat, lon, zoom) }.toAddress()
        // Empty results (sea, forest) are not cached, so a retry after moving the pin still asks Nominatim
        if (!address.isEmpty) cache.upsert(address.toCachedAddress(key, System.currentTimeMillis()))
        return address
    }
}
