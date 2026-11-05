package be.ap.nowaste.data.bag

import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [isFromCache]: Firestore could not reach the server, so these are the bags it saved earlier. */
data class BagsResult(val bags: List<Bag>, val isFromCache: Boolean)

class BagRepository(firestore: FirebaseFirestore = Firebase.firestore) {

    private val bags = firestore.collection("bags")

    /**
     * Active bags, updated in realtime (K5). Sold out and expired bags are filtered in the app with
     * [Bag.isAvailable]: combining those filters in one Firestore query would need a composite index.
     */
    fun activeBags(): Flow<BagsResult> =
        // INCLUDE: also a new snapshot when only isFromCache changes, e.g. when the server confirms the cached bags
        bags.whereEqualTo("active", true).snapshots(MetadataChanges.INCLUDE).map { snapshot ->
            BagsResult(snapshot.documents.mapNotNull { it.toBag() }, snapshot.metadata.isFromCache)
        }

    private fun DocumentSnapshot.toBag(): Bag? {
        val location = getGeoPoint("location") ?: return null
        return Bag(
            id = id,
            storeId = getString("storeId").orEmpty(),
            storeName = getString("storeName").orEmpty(),
            title = getString("title").orEmpty(),
            description = getString("description").orEmpty(),
            price = getDouble("price") ?: 0.0,
            originalValue = getDouble("originalValue") ?: 0.0,
            quantityTotal = getLong("quantityTotal")?.toInt() ?: 0,
            quantityLeft = getLong("quantityLeft")?.toInt() ?: 0,
            pickupStart = getTimestamp("pickupStart")?.toDate()?.time ?: 0,
            pickupEnd = getTimestamp("pickupEnd")?.toDate()?.time ?: 0,
            latitude = location.latitude,
            longitude = location.longitude,
            active = getBoolean("active") ?: false,
            photoBase64 = getString("photoBase64").orEmpty(),
        )
    }
}
