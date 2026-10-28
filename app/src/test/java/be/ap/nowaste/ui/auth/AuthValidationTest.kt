package be.ap.nowaste.ui.auth

import be.ap.nowaste.data.auth.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {

    @Test
    fun validEmails() {
        assertTrue(isValidEmail("klant@nowaste.be"))
        assertTrue(isValidEmail("  first.last@ap.be  "))
    }

    @Test
    fun invalidEmails() {
        assertFalse(isValidEmail(""))
        assertFalse(isValidEmail("klant"))
        assertFalse(isValidEmail("klant@nowaste"))
        assertFalse(isValidEmail("kl ant@nowaste.be"))
    }

    @Test
    fun passwordNeedsMinimumLength() {
        assertFalse(isValidPassword("12345"))
        assertTrue(isValidPassword("123456"))
    }

    @Test
    fun roleMapsToFirestoreValue() {
        assertEquals(UserRole.CUSTOMER, UserRole.fromValue("customer"))
        assertEquals(UserRole.MERCHANT, UserRole.fromValue("merchant"))
        assertNull(UserRole.fromValue("admin"))
        assertNull(UserRole.fromValue(null))
    }
}
