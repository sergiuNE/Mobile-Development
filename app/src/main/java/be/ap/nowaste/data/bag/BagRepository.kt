package be.ap.nowaste.data.bag

import be.ap.nowaste.data.store.Store
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

/** [isFromCache]: Firestore could not reach the server, so these are the bags it saved earlier. */
data class BagsResult(val bags: List<Bag>, val isFromCache: Boolean)

/** H7: the merchant tried to set the total lower than what customers reserved already. */
class BelowReservedException : Exception()

class BagRepository(private val firestore: FirebaseFirestore = Firebase.firestore) {

    private val bags = firestore.collection("bags")

    /** All bags of one store, also inactive ones, updated in realtime (H5). */
    fun storeBags(storeId: String): Flow<List<Bag>> =
        bags.whereEqualTo("storeId", storeId).snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { it.toBag() }.sortedByDescending { it.pickupStart }
        }

    suspend fun getBag(bagId: String): Bag? = bags.document(bagId).get().await().toBag()

    /** Id for a bag that is not saved yet. Saving twice with the same id gives one bag, not two. */
    fun newBagId(): String = bags.document().id

    /** H3: a new, active bag. storeName and location are copied from the store on purpose (see datamodel). */
    suspend fun createBag(bagId: String, store: Store, input: BagInput) {
        val data = input.toFields() + mapOf(
            "storeId" to store.id,
            "storeName" to store.name,
            "location" to GeoPoint(store.latitude, store.longitude),
            "quantityLeft" to input.quantityTotal,
            "active" to true,
        )
        bags.document(bagId).set(data).await()
    }

    /**
     * H7: change a bag. In a transaction, because a customer can reserve at the same moment:
     * quantityLeft is computed from the newest numbers.
     */
    suspend fun updateBag(bagId: String, input: BagInput) {
        val document = bags.document(bagId)
        firestore.runTransaction { transaction ->
            val current = transaction.get(document)
            val oldTotal = current.getLong("quantityTotal")?.toInt() ?: 0
            val oldLeft = current.getLong("quantityLeft")?.toInt() ?: 0
            val left = quantityLeftAfterEdit(oldTotal, oldLeft, input.quantityTotal) ?: throw BelowReservedException()
            transaction.update(document, input.toFields() + mapOf("quantityLeft" to left))
        }.await()
    }

    /** H7: take a bag offline (customers no longer see it) or put it back online. */
    suspend fun setActive(bagId: String, active: Boolean) {
        bags.document(bagId).update("active", active).await()
    }

    private fun BagInput.toFields(): Map<String, Any> = mapOf(
        "title" to title,
        "description" to description,
        "price" to price,
        "originalValue" to originalValue,
        "quantityTotal" to quantityTotal,
        "pickupStart" to Timestamp(Date(epochMillisAt(pickupStartMinutes))),
        "pickupEnd" to Timestamp(Date(epochMillisAt(pickupEndMinutes))),
        "photoBase64" to photoBase64,
    )

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
