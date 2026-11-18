package be.ap.nowaste.data.reservation

import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** H6: the reservation was cancelled or collected in the meantime. */
class ReservationNotOpenException : Exception()

class ReservationRepository(private val firestore: FirebaseFirestore = Firebase.firestore) {

    private val reservations = firestore.collection("reservations")

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
