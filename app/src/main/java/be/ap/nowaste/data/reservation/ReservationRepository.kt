package be.ap.nowaste.data.reservation

import be.ap.nowaste.data.bag.toBag
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

/** H6: the reservation was cancelled or collected in the meantime. */
class ReservationNotOpenException : Exception()

/** K8: someone else was faster, the bag went offline or its pickup window is over. */
class NotEnoughBagsException : Exception()

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
