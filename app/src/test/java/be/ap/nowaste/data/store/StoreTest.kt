package be.ap.nowaste.data.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoreTest {

    @Test
    fun addressIsOneLine() {
        assertEquals("Meir 1, 2000 Antwerpen", formatAddress("Meir 1", "2000", "Antwerpen"))
        assertEquals("Meir 1, 2000 Antwerpen", formatAddress(" Meir 1 ", " 2000", "Antwerpen "))
    }

    @Test
    fun addressSkipsEmptyParts() {
        assertEquals("2000 Antwerpen", formatAddress("", "2000", "Antwerpen"))
        assertEquals("Meir 1, Antwerpen", formatAddress("Meir 1", "", "Antwerpen"))
        assertEquals("", formatAddress("", "", ""))
    }

    @Test
    fun categoryMapsToFirestoreValue() {
        StoreCategory.entries.forEach { assertEquals(it, StoreCategory.fromValue(it.value)) }
        assertNull(StoreCategory.fromValue("pharmacy"))
    }
}
