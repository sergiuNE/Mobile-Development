package be.ap.nowaste.data.bag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Performance: the Discover list is filtered and sorted again on every snapshot, filter change and minute.
 * With far more bags than a real city has, that must still stay well under a frame budget on the JVM.
 */
class BagPerformanceTest {

    private val now = 1_700_000_000_000L
    private val hour = 3_600_000L

    private fun randomBags(count: Int): List<Bag> {
        val random = Random(42)
        return List(count) { i ->
            Bag(
                id = "bag$i",
                storeId = "store${i % 500}",
                storeName = "Store",
                title = "Bag",
                description = "",
                price = 3.99,
                originalValue = 12.0,
                quantityTotal = 5,
                quantityLeft = random.nextInt(0, 5),
                pickupStart = now - hour,
                pickupEnd = now + random.nextLong(-hour, 8 * hour),
                // Around Antwerp, up to about 20 km away
                latitude = 51.2172 + random.nextDouble(-0.2, 0.2),
                longitude = 4.4127 + random.nextDouble(-0.3, 0.3),
                active = random.nextInt(10) != 0,
                photoBase64 = "",
            )
        }
    }

    @Test
    fun tenThousandBagsAreFilteredAndSortedQuickly() {
        val bags = randomBags(10_000)
        // Warm up the JIT, so the measurement is not about class loading
        availableBagsByDistance(bags, 51.2299, 4.4161, now)

        lateinit var result: List<BagWithDistance>
        val millis = measureTimeMillis { result = availableBagsByDistance(bags, 51.2299, 4.4161, now) }

        assertTrue("Took $millis ms", millis < 200)
        assertEquals(bags.count { it.isAvailable(now) }, result.size)
        assertEquals(result.sortedBy { it.distanceInMeters }, result)
    }
}
