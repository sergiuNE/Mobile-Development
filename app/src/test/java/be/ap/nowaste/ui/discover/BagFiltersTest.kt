package be.ap.nowaste.ui.discover

import be.ap.nowaste.data.store.StoreCategory
import be.ap.nowaste.ui.map.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Test

class BagFiltersTest {

    private fun bag(id: String, storeId: String = "bakery", distanceInKm: Double = 0.5) = BagListItem(
        id = id,
        storeId = storeId,
        storeName = "Store",
        storeLocation = Coordinates(51.2172, 4.4127),
        title = "Bag",
        price = "€3.99",
        originalValue = "€12.00",
        pickupStart = "17:00",
        pickupEnd = "18:00",
        quantityLeft = 2,
        distanceInKm = distanceInKm,
        photo = null,
    )

    private val categories = mapOf("bakery" to StoreCategory.BAKERY, "other" to StoreCategory.OTHER)

    @Test
    fun maxDistanceLeavesOutBagsFurtherAway() {
        val bags = listOf(bag("near", distanceInKm = 0.1), bag("3km", distanceInKm = 3.0), bag("9km", distanceInKm = 9.0))

        assertEquals(listOf("near"), bags.inFilters(categories, null, 1).map { it.id })
        assertEquals(listOf("near", "3km"), bags.inFilters(categories, null, 3).map { it.id })
        assertEquals(3, bags.inFilters(categories, null, null).size)
    }

    @Test
    fun categoryLeavesOutBagsOfOtherStores() {
        val bags = listOf(bag("bread", storeId = "bakery"), bag("rest", storeId = "other"), bag("unknown", storeId = "missing"))

        assertEquals(listOf("bread"), bags.inFilters(categories, StoreCategory.BAKERY, null).map { it.id })
    }
}
