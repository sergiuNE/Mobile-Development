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

    fun distanceInMeters(latitude: Double, longitude: Double): Double =
        GeoPoint(this.latitude, this.longitude).distanceToAsDouble(GeoPoint(latitude, longitude))
}

data class BagWithDistance(val bag: Bag, val distanceInMeters: Double)

/** Available bags around a location, nearest first. [maxDistanceInMeters] null means no limit (K6). */
fun availableBagsByDistance(
    bags: List<Bag>,
    latitude: Double,
    longitude: Double,
    now: Long,
    maxDistanceInMeters: Double? = null,
): List<BagWithDistance> =
    bags.filter { it.isAvailable(now) }
        .map { BagWithDistance(it, it.distanceInMeters(latitude, longitude)) }
        .filter { maxDistanceInMeters == null || it.distanceInMeters <= maxDistanceInMeters }
        .sortedBy { it.distanceInMeters }
