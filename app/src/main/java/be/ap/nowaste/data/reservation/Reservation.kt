package be.ap.nowaste.data.reservation

import java.security.SecureRandom
import java.util.Random

const val PICKUP_CODE_LENGTH = 6

// No 0/O and 1/I: the merchant reads the code from the customer's screen and types it (H6)
private const val PICKUP_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

/** K9: a random code of 6 characters, shown to the customer and checked by the merchant. */
fun newPickupCode(random: Random = SecureRandom()): String =
    (1..PICKUP_CODE_LENGTH).map { PICKUP_CODE_ALPHABET[random.nextInt(PICKUP_CODE_ALPHABET.length)] }.joinToString("")

// The value is what we store in the "status" field of reservations/{id}.
enum class ReservationStatus(val value: String) {
    RESERVED("reserved"),
    COLLECTED("collected"),
    CANCELLED("cancelled");

    companion object {
        fun fromValue(value: String?): ReservationStatus? = entries.find { it.value == value }
    }
}

/** A reservation, as stored in reservations/{id}. [createdAt] is epoch milliseconds. */
data class Reservation(
    val id: String,
    val bagId: String,
    val storeId: String,
    val customerId: String,
    val quantity: Int,
    val pickupCode: String,
    val status: ReservationStatus,
    val createdAt: Long,
)

/** Reserved bags per bag id (H5). Cancelled reservations gave their bags back, so they do not count. */
fun reservedQuantityByBag(reservations: List<Reservation>): Map<String, Int> =
    reservations.filter { it.status != ReservationStatus.CANCELLED }
        .groupBy { it.bagId }
        .mapValues { (_, list) -> list.sumOf { it.quantity } }

/** Pickup codes are shown in capitals, but the merchant may type them in lowercase or with spaces. */
fun normalizePickupCode(code: String): String = code.filterNot { it.isWhitespace() }.uppercase()

/** The open reservation with this pickup code (H6), or null when the code is wrong or already used. */
fun findOpenReservation(reservations: List<Reservation>, code: String): Reservation? {
    val normalized = normalizePickupCode(code)
    if (normalized.isEmpty()) return null
    return reservations.find { it.status == ReservationStatus.RESERVED && it.pickupCode.uppercase() == normalized }
}
