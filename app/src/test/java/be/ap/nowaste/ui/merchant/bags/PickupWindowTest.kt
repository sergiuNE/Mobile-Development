package be.ap.nowaste.ui.merchant.bags

import be.ap.nowaste.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PickupWindowTest {

    private val now = 14 * 60

    @Test
    fun validWindowLaterToday() {
        assertNull(pickupError(start = 17 * 60, end = 19 * 60, nowMinutes = now))
        // Already started but not over yet: still fine
        assertNull(pickupError(start = 13 * 60, end = 15 * 60, nowMinutes = now))
    }

    @Test
    fun invalidWindows() {
        assertEquals(R.string.bag_error_pickup_required, pickupError(start = null, end = 19 * 60, nowMinutes = now))
        assertEquals(R.string.bag_error_pickup_order, pickupError(start = 19 * 60, end = 17 * 60, nowMinutes = now))
        assertEquals(R.string.bag_error_pickup_order, pickupError(start = 17 * 60, end = 17 * 60, nowMinutes = now))
        assertEquals(R.string.bag_error_pickup_over, pickupError(start = 11 * 60, end = 13 * 60, nowMinutes = now))
    }
}
