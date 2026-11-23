package be.ap.nowaste.data.reservation

import be.ap.nowaste.data.local.CachedReservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

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
    fun pickupCodeHasSixReadableCharacters() {
        val random = Random(42)
        repeat(100) {
            val code = newPickupCode(random)
            assertEquals(PICKUP_CODE_LENGTH, code.length)
            // Capitals and digits only, without the look-alikes 0, O, 1 and I
            assertTrue(code, code.all { it in 'A'..'Z' || it in '2'..'9' })
            assertTrue(code, code.none { it in "0O1I" })
        }
    }

    @Test
    fun newPickupCodeIsFoundByTheMerchant() {
        val code = newPickupCode()
        val list = reservations + reservation("r5", "bagC", 1, code, ReservationStatus.RESERVED)
        assertEquals("r5", findOpenReservation(list, code.lowercase())?.id)
    }

    private fun order(id: String, status: String, pickupStart: Long = 1_000, pickupEnd: Long = 2_000) = CachedReservation(
        id, customerId = "customer", bagId = "bag", bagTitle = "Bread", storeName = "Bakery", quantity = 1,
        pickupCode = "AB12CD", pickupStart = pickupStart, pickupEnd = pickupEnd, status = status, createdAt = 0,
    )

    @Test
    fun canCancelOnlyBeforePickupStart() {
        assertTrue(order("a", "reserved").canCancel(now = 999))
        assertFalse(order("a", "reserved").canCancel(now = 1_000))
        assertFalse(order("a", "reserved").canCancel(now = 1_500))
    }

    @Test
    fun collectedCancelledOrUnknownCannotBeCancelled() {
        assertFalse(order("a", "collected").canCancel(now = 0))
        assertFalse(order("a", "cancelled").canCancel(now = 0))
        assertFalse(order("a", "lost").canCancel(now = 0))
    }

    @Test
    fun activeMeansReservedAndPickupNotOver() {
        assertTrue(order("a", "reserved").isActive(now = 1_500))
        assertFalse(order("a", "reserved").isActive(now = 2_000))
        assertFalse(order("a", "collected").isActive(now = 0))
        assertFalse(order("a", "cancelled").isActive(now = 0))
    }

    @Test
    fun ordersAreSplitInActiveAndPastKeepingTheOrder() {
        val orders = listOf(
            order("r1", "reserved"),
            order("r2", "cancelled"),
            order("r3", "reserved", pickupEnd = 500),
            order("r4", "reserved"),
            order("r5", "collected"),
        )
        val (active, past) = splitActiveAndPast(orders, now = 600)
        assertEquals(listOf("r1", "r4"), active.map { it.id })
        assertEquals(listOf("r2", "r3", "r5"), past.map { it.id })
        assertEquals(Pair(emptyList<CachedReservation>(), emptyList<CachedReservation>()), splitActiveAndPast(emptyList(), now = 0))
    }

    @Test
    fun statusMapsToFirestoreValue() {
        ReservationStatus.entries.forEach { assertEquals(it, ReservationStatus.fromValue(it.value)) }
        assertNull(ReservationStatus.fromValue("lost"))
    }
}
