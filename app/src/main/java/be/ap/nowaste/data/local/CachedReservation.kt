package be.ap.nowaste.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * K10: a reservation of the customer, copied from Firestore. The Orders screen reads only this table.
 * Times are epoch milliseconds, [status] is the Firestore value ("reserved", "collected" or "cancelled").
 * [customerId] keeps the orders of different users on one phone apart, [bagId] is needed to cancel (K11).
 */
@Entity(tableName = "cached_reservation")
data class CachedReservation(
    @PrimaryKey val id: String,
    val customerId: String,
    val bagId: String,
    val bagTitle: String,
    val storeName: String,
    val quantity: Int,
    val pickupCode: String,
    val pickupStart: Long,
    val pickupEnd: Long,
    val status: String,
    val createdAt: Long,
)

@Dao
interface CachedReservationDao {
    @Query("SELECT * FROM cached_reservation WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun observe(customerId: String): Flow<List<CachedReservation>>

    @Upsert
    suspend fun upsertAll(reservations: List<CachedReservation>)

    // Reservations that are gone from Firestore must disappear here too
    @Query("DELETE FROM cached_reservation WHERE customerId = :customerId AND id NOT IN (:keepIds)")
    suspend fun deleteOthers(customerId: String, keepIds: List<String>)

    @Query("UPDATE cached_reservation SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    /** One transaction, so the screen never sees a half-updated list. */
    @Transaction
    suspend fun replaceAll(customerId: String, reservations: List<CachedReservation>) {
        upsertAll(reservations)
        deleteOthers(customerId, reservations.map { it.id })
    }
}
