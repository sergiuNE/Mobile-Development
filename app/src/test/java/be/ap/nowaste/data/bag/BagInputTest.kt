package be.ap.nowaste.data.bag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BagInputTest {

    @Test
    fun priceAcceptsCommaAndDot() {
        assertEquals(3.5, parsePrice("3,50")!!, 0.0)
        assertEquals(3.5, parsePrice(" 3.5 ")!!, 0.0)
        assertEquals(12.0, parsePrice("12")!!, 0.0)
        assertEquals(4.0, parsePrice("3.999")!!, 0.0)
    }

    @Test
    fun priceRejectsEmptyZeroNegativeAndText() {
        assertNull(parsePrice(""))
        assertNull(parsePrice("0"))
        assertNull(parsePrice("-2"))
        assertNull(parsePrice("abc"))
    }

    @Test
    fun pickupTimeIsTodayInLocalTime() {
        val zone = ZoneId.of("Europe/Brussels")
        val day = LocalDate.of(2026, 11, 18)
        val millis = epochMillisAt(17 * 60 + 30, day, zone)
        assertEquals(17 * 60 + 30, minutesOfDay(millis, zone))
        assertEquals(day, java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalDate())
    }

    @Test
    fun timesAreFormattedWithTwoDigits() {
        assertEquals("09:05", formatMinutes(9 * 60 + 5))
        assertEquals("17:30", formatMinutes(17 * 60 + 30))
    }

    @Test
    fun reservedBagsStayReservedAfterEdit() {
        // 5 bags, 2 reserved: raise to 8 gives 6 left, lower to 3 gives 1 left
        assertEquals(6, quantityLeftAfterEdit(oldTotal = 5, oldLeft = 3, newTotal = 8))
        assertEquals(1, quantityLeftAfterEdit(oldTotal = 5, oldLeft = 3, newTotal = 3))
        assertEquals(0, quantityLeftAfterEdit(oldTotal = 5, oldLeft = 3, newTotal = 2))
        assertNull(quantityLeftAfterEdit(oldTotal = 5, oldLeft = 3, newTotal = 1))
    }

    @Test
    fun photoIsScaledToMax800OnTheLongestSide() {
        assertEquals(800 to 600, scaledSize(4000, 3000, 800))
        assertEquals(450 to 800, scaledSize(1080, 1920, 800))
        // Small photos are never scaled up
        assertEquals(320 to 240, scaledSize(320, 240, 800))
    }
}
