package be.ap.nowaste.data.bag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BagTest {

    private val now = 1_000_000L

    private fun bag(
        id: String = "bag",
        quantityLeft: Int = 2,
        pickupEnd: Long = now + 60_000,
        active: Boolean = true,
        latitude: Double = 51.2172,
        longitude: Double = 4.4127,
    ) = Bag(
        id = id,
        storeId = "store",
        storeName = "Store",
        title = "Bag",
        description = "",
        price = 3.99,
        originalValue = 12.0,
        quantityTotal = 5,
        quantityLeft = quantityLeft,
        pickupStart = now - 60_000,
        pickupEnd = pickupEnd,
        latitude = latitude,
        longitude = longitude,
        active = active,
        photoBase64 = "",
    )

    @Test
    fun bagWithStockAndOpenWindowIsAvailable() {
        assertTrue(bag().isAvailable(now))
    }

    @Test
    fun soldOutBagIsNotAvailable() {
        assertFalse(bag(quantityLeft = 0).isAvailable(now))
    }

    @Test
    fun bagAfterPickupWindowIsNotAvailable() {
        assertFalse(bag(pickupEnd = now).isAvailable(now))
        assertFalse(bag(pickupEnd = now - 1).isAvailable(now))
    }

    @Test
    fun inactiveBagIsNotAvailable() {
        assertFalse(bag(active = false).isAvailable(now))
    }

    @Test
    fun listIsSortedByDistanceWithoutUnavailableBags() {
        val bags = listOf(
            bag(id = "far", latitude = 51.30),
            bag(id = "soldOut", quantityLeft = 0),
            bag(id = "near", latitude = 51.2180),
            bag(id = "middle", latitude = 51.25),
        )

        val result = availableBagsByDistance(bags, 51.2172, 4.4127, now)

        assertEquals(listOf("near", "middle", "far"), result.map { it.bag.id })
    }

    @Test
    fun maxDistanceLeavesOutBagsFurtherAway() {
        val bags = listOf(
            bag(id = "near", latitude = 51.2180),
            bag(id = "3km", latitude = 51.2442),
            bag(id = "9km", latitude = 51.30),
        )

        assertEquals(listOf("near"), availableBagsByDistance(bags, 51.2172, 4.4127, now, 1_000.0).map { it.bag.id })
        assertEquals(listOf("near", "3km"), availableBagsByDistance(bags, 51.2172, 4.4127, now, 5_000.0).map { it.bag.id })
        assertEquals(3, availableBagsByDistance(bags, 51.2172, 4.4127, now, null).size)
    }

    @Test
    fun distanceIsInMeters() {
        // 0.01 degree of latitude is about 1.1 km
        val distance = bag(latitude = 51.2272).distanceInMeters(51.2172, 4.4127)
        assertEquals(1112.0, distance, 10.0)
    }
}
