package be.ap.nowaste.data.bag

import org.osmdroid.util.GeoPoint

/** A surprise bag, as stored in bags/{bagId}. Times are epoch milliseconds. */
data class Bag(
    val id: String,
    val storeId: String,
    val storeName: String,
    val title: String,
    val description: String,
    val price: Double,
    val originalValue: Double,
    val quantityTotal: Int,
    val quantityLeft: Int,
    val pickupStart: Long,
    val pickupEnd: Long,
    val latitude: Double,
    val longitude: Double,
    val active: Boolean,
    val photoBase64: String,
) {
    /** K3: still at least 1 left and the pickup window has not passed yet. */
    fun isAvailable(now: Long): Boolean = active && quantityLeft > 0 && pickupEnd > now

    /** K8: at least 1 and at most what is left, and only while the bag is available. */
    fun canReserve(quantity: Int, now: Long): Boolean = isAvailable(now) && quantity in 1..quantityLeft

    fun distanceInMeters(latitude: Double, longitude: Double): Double =
        GeoPoint(this.latitude, this.longitude).distanceToAsDouble(GeoPoint(latitude, longitude))
}

data class BagWithDistance(val bag: Bag, val distanceInMeters: Double)

/** Available bags around a location, nearest first. */
fun availableBagsByDistance(bags: List<Bag>, latitude: Double, longitude: Double, now: Long): List<BagWithDistance> =
    bags.filter { it.isAvailable(now) }
        .map { BagWithDistance(it, it.distanceInMeters(latitude, longitude)) }
        .sortedBy { it.distanceInMeters }
