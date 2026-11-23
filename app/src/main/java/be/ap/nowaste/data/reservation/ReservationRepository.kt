package be.ap.nowaste.data.reservation

import be.ap.nowaste.data.bag.toBag
import be.ap.nowaste.data.local.CachedReservation
import be.ap.nowaste.data.local.CachedReservationDao
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

/** H6: the reservation was cancelled or collected in the meantime. */
class ReservationNotOpenException : Exception()

/** K8: someone else was faster, the bag went offline or its pickup window is over. */
class NotEnoughBagsException : Exception()

/** K11: the reservation is not the customer's, not open anymore or its pickup window already started. */
class CannotCancelException : Exception()

class ReservationRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val firestore: FirebaseFirestore = Firebase.firestore,
) {

    private val reservations = firestore.collection("reservations")
    private val bags = firestore.collection("bags")

    /**
     * K8 + K9: lower quantityLeft and create the reservation in one transaction, so two customers can never
     * reserve the same last bag. Returns the pickup code.
     */
    suspend fun reserve(bagId: String, quantity: Int): String {
        val customerId = requireNotNull(auth.currentUser).uid
        val bagDocument = bags.document(bagId)
        val reservationDocument = reservations.document()
        val pickupCode = newPickupCode()
        firestore.runTransaction { transaction ->
            val bag = transaction.get(bagDocument).toBag()
            if (bag == null || !bag.canReserve(quantity, System.currentTimeMillis())) throw NotEnoughBagsException()
            transaction.update(bagDocument, "quantityLeft", bag.quantityLeft - quantity)
            transaction.set(
                reservationDocument,
                mapOf(
                    "bagId" to bagId,
                    "storeId" to bag.storeId,
                    "customerId" to customerId,
                    "quantity" to quantity,
                    "pickupCode" to pickupCode,
                    "status" to ReservationStatus.RESERVED.value,
                    "createdAt" to FieldValue.serverTimestamp(),
                    // Extra fields, copied like storeName in bags: Orders (K10) needs them without reading the bag
                    "bagTitle" to bag.title,
                    "storeName" to bag.storeName,
                    "pickupStart" to Timestamp(Date(bag.pickupStart)),
                    "pickupEnd" to Timestamp(Date(bag.pickupEnd)),
                ),
            )
        }.await()
        return pickupCode
    }

    /** K10: the logged-in customer's orders from Room, newest first. */
    fun customerOrders(cache: CachedReservationDao): Flow<List<CachedReservation>> =
        cache.observe(auth.currentUser?.uid.orEmpty())

    /**
     * K10: follow the reservations of the logged-in customer in realtime and write every snapshot to Room.
     * Emits isFromCache for every snapshot. The Orders screen reads the reservations from Room only.
     */
    fun syncCustomerReservations(cache: CachedReservationDao): Flow<Boolean> {
        val customerId = auth.currentUser?.uid ?: return emptyFlow()
        // INCLUDE: also a new snapshot when only isFromCache changes, so the offline notice disappears again
        return reservations.whereEqualTo("customerId", customerId).snapshots(MetadataChanges.INCLUDE).map { snapshot ->
            val orders = snapshot.documents.mapNotNull { it.toCachedReservation() }
            // Offline the Firestore cache may miss reservations that Room has: then only add, never delete
            if (snapshot.metadata.isFromCache) cache.upsertAll(orders) else cache.replaceAll(customerId, orders)
            snapshot.metadata.isFromCache
        }
    }

    /**
     * K11: cancel in one transaction: the reservation becomes cancelled and the bags go back to the bag.
     * All reads come before the writes, as a Firestore transaction requires.
     */
    suspend fun cancel(reservationId: String, cache: CachedReservationDao) {
        val customerId = requireNotNull(auth.currentUser).uid
        val reservationDocument = reservations.document(reservationId)
        firestore.runTransaction { transaction ->
            val reservation = transaction.get(reservationDocument)
            val status = ReservationStatus.fromValue(reservation.getString("status"))
            val pickupStart = reservation.getTimestamp("pickupStart")?.toDate()?.time ?: 0
            if (reservation.getString("customerId") != customerId || !canCancel(status, pickupStart, System.currentTimeMillis())) {
                throw CannotCancelException()
            }
            val bagDocument = bags.document(reservation.getString("bagId").orEmpty())
            val bag = transaction.get(bagDocument)

            transaction.update(reservationDocument, "status", ReservationStatus.CANCELLED.value)
            // The merchant may have deleted the bag in the meantime: then there is nothing to give back
            if (bag.exists()) {
                val total = bag.getLong("quantityTotal")?.toInt() ?: 0
                val left = bag.getLong("quantityLeft")?.toInt() ?: 0
                val quantity = reservation.getLong("quantity")?.toInt() ?: 0
                transaction.update(bagDocument, "quantityLeft", minOf(left + quantity, total))
            }
        }.await()
        // The sync does this too, but now the screen changes at once
        cache.updateStatus(reservationId, ReservationStatus.CANCELLED.value)
    }

    /** All reservations of one store, updated in realtime (H5). */
    fun storeReservations(storeId: String): Flow<List<Reservation>> =
        reservations.whereEqualTo("storeId", storeId).snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { it.toReservation() }
        }

    /**
     * H6: the customer showed the right pickup code and got the bag. In a transaction, because the customer
     * may cancel at the same moment: a cancelled reservation must not become collected.
     */
    suspend fun markCollected(reservationId: String) {
        val document = reservations.document(reservationId)
        firestore.runTransaction { transaction ->
            val status = ReservationStatus.fromValue(transaction.get(document).getString("status"))
            if (status != ReservationStatus.RESERVED) throw ReservationNotOpenException()
            transaction.update(document, "status", ReservationStatus.COLLECTED.value)
        }.await()
    }

    private fun DocumentSnapshot.toCachedReservation(): CachedReservation? {
        val status = ReservationStatus.fromValue(getString("status")) ?: return null
        return CachedReservation(
            id = id,
            customerId = getString("customerId").orEmpty(),
            bagId = getString("bagId").orEmpty(),
            bagTitle = getString("bagTitle").orEmpty(),
            storeName = getString("storeName").orEmpty(),
            quantity = getLong("quantity")?.toInt() ?: 0,
            pickupCode = getString("pickupCode").orEmpty(),
            pickupStart = getTimestamp("pickupStart")?.toDate()?.time ?: 0,
            pickupEnd = getTimestamp("pickupEnd")?.toDate()?.time ?: 0,
            status = status.value,
            // A reservation that is still being written has no server time yet: use the estimate
            createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: 0,
        )
    }

    private fun DocumentSnapshot.toReservation(): Reservation? {
        val status = ReservationStatus.fromValue(getString("status")) ?: return null
        return Reservation(
            id = id,
            bagId = getString("bagId").orEmpty(),
            storeId = getString("storeId").orEmpty(),
            customerId = getString("customerId").orEmpty(),
            quantity = getLong("quantity")?.toInt() ?: 0,
            pickupCode = getString("pickupCode").orEmpty(),
            status = status,
            createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0,
        )
    }
}
