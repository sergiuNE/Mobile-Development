package be.ap.nowaste.data.bag

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/** What the merchant fills in for a bag (H3). Times are minutes after midnight, today. */
data class BagInput(
    val title: String,
    val description: String,
    val price: Double,
    val originalValue: Double,
    val quantityTotal: Int,
    val pickupStartMinutes: Int,
    val pickupEndMinutes: Int,
    val photoBase64: String,
)

/** "3,50" and "3.50" both give 3.5. Null for empty, negative, zero or not a number. Rounded to cents. */
fun parsePrice(text: String): Double? {
    val value = text.trim().replace(',', '.').toBigDecimalOrNull() ?: return null
    if (value <= BigDecimal.ZERO) return null
    return value.setScale(2, RoundingMode.HALF_UP).toDouble()
}

/** Epoch milliseconds of [minutes] after midnight on [day]. */
fun epochMillisAt(minutes: Int, day: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Long =
    ZonedDateTime.of(day, LocalTime.of(minutes / 60, minutes % 60), zone).toInstant().toEpochMilli()

/** Minutes after midnight of an epoch milliseconds time, in [zone]. */
fun minutesOfDay(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
    val time = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime()
    return time.hour * 60 + time.minute
}

/** "09:05" */
fun formatMinutes(minutes: Int): String = String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60)

/**
 * Bags left after changing the total (H7): bags that are reserved already stay reserved.
 * Null when the new total is lower than what customers already reserved.
 */
fun quantityLeftAfterEdit(oldTotal: Int, oldLeft: Int, newTotal: Int): Int? {
    val reserved = oldTotal - oldLeft
    return if (newTotal < reserved) null else newTotal - reserved
}
