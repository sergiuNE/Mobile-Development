package be.ap.nowaste.data.geocoding

import be.ap.nowaste.data.local.CachedAddress
import be.ap.nowaste.data.local.CachedAddressDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressTest {

    @Test
    fun cacheKeyRoundsToFourDecimalsAndIncludesZoom() {
        assertEquals("51.2172,4.4127@18", addressCacheKey(51.21721234, 4.41269876, 18))
        assertEquals("51.2172,4.4127@10", addressCacheKey(51.21721234, 4.41269876, 10))
        // Points about 1 m apart share the same cache entry
        assertEquals(addressCacheKey(51.21720, 4.41270, 18), addressCacheKey(51.21721, 4.41271, 18))
    }

    @Test
    fun buildingAddressHasStreetNumberPostcodeAndCity() {
        val response = NominatimResponse(
            displayName = "1, Meir, Antwerpen, 2000, België",
            address = NominatimAddress(road = "Meir", houseNumber = "1", postcode = "2000", city = "Antwerpen"),
        )
        assertEquals(Address("Meir 1", "2000", "Antwerpen", "1, Meir, Antwerpen, 2000, België"), response.toAddress())
    }

    @Test
    fun cityFallsBackToTownThenVillage() {
        assertEquals("Lier", NominatimResponse(address = NominatimAddress(town = "Lier")).toAddress().city)
        assertEquals("Zoersel", NominatimResponse(address = NominatimAddress(village = "Zoersel")).toAddress().city)
    }

    @Test
    fun noResultGivesEmptyAddress() {
        assertTrue(NominatimResponse().toAddress().isEmpty)
    }

    @Test
    fun cachedAddressIsReusedWithoutCallingNominatim() = runBlocking {
        val dao = object : CachedAddressDao {
            override suspend fun get(key: String) = CachedAddress(key, "Meir 1", "Meir 1", "2000", "Antwerpen", 0)
            override suspend fun upsert(address: CachedAddress) = error("should not write")
        }
        val api = object : NominatimApi {
            override suspend fun reverse(lat: Double, lon: Double, zoom: Int, format: String, addressDetails: Int) =
                error("Nominatim must not be called for a cached location")
        }

        val address = GeocodingRepository(dao, api).reverseGeocode(51.2172, 4.4127)

        assertEquals("Meir 1", address.street)
    }

    @Test
    fun newLocationIsAskedOnceAndCached() = runBlocking {
        val dao = FakeDao()
        var calls = 0
        val api = fakeApi { calls++; NominatimResponse(address = NominatimAddress(road = "Meir", houseNumber = "1", city = "Antwerpen")) }
        val repository = GeocodingRepository(dao, api)

        repository.reverseGeocode(51.2172, 4.4127)
        repository.reverseGeocode(51.21721, 4.41271) // about 1 m away: same cache key

        assertEquals(1, calls)
        assertEquals("Meir 1", dao.rows["51.2172,4.4127@18"]?.street)
    }

    @Test
    fun emptyResultIsNotCached() = runBlocking {
        val dao = FakeDao()
        GeocodingRepository(dao, fakeApi { NominatimResponse() }).reverseGeocode(51.0, 2.5)
        assertTrue(dao.rows.isEmpty())
    }

    private class FakeDao : CachedAddressDao {
        val rows = mutableMapOf<String, CachedAddress>()
        override suspend fun get(key: String) = rows[key]
        override suspend fun upsert(address: CachedAddress) {
            rows[address.key] = address
        }
    }

    private fun fakeApi(answer: () -> NominatimResponse) = object : NominatimApi {
        override suspend fun reverse(lat: Double, lon: Double, zoom: Int, format: String, addressDetails: Int) = answer()
    }
}
