package be.ap.nowaste.data.reservation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReservationTest {

    private fun reservation(id: String, bagId: String, quantity: Int, code: String, status: ReservationStatus) =
        Reservation(id, bagId, storeId = "store", customerId = "customer", quantity, code, status, createdAt = 0)

    private val reservations = listOf(
        reservation("r1", "bagA", 2, "AB12CD", ReservationStatus.RESERVED),
        reservation("r2", "bagA", 1, "XY34ZW", ReservationStatus.COLLECTED),
        reservation("r3", "bagA", 3, "QQ11QQ", ReservationStatus.CANCELLED),
        reservation("r4", "bagB", 1, "KL56MN", ReservationStatus.RESERVED),
    )

    @Test
    fun reservedCountSkipsCancelled() {
        assertEquals(mapOf("bagA" to 3, "bagB" to 1), reservedQuantityByBag(reservations))
    }

    @Test
    fun codeIsFoundCaseAndSpaceInsensitive() {
        assertEquals("r1", findOpenReservation(reservations, "ab12cd")?.id)
        assertEquals("r1", findOpenReservation(reservations, " AB1 2CD ")?.id)
    }

    @Test
    fun usedCancelledOrWrongCodesAreNotFound() {
        assertNull(findOpenReservation(reservations, "XY34ZW"))
        assertNull(findOpenReservation(reservations, "QQ11QQ"))
        assertNull(findOpenReservation(reservations, "ZZZZZZ"))
        assertNull(findOpenReservation(reservations, ""))
    }

    @Test
    fun statusMapsToFirestoreValue() {
        ReservationStatus.entries.forEach { assertEquals(it, ReservationStatus.fromValue(it.value)) }
        assertNull(ReservationStatus.fromValue("lost"))
    }
}
