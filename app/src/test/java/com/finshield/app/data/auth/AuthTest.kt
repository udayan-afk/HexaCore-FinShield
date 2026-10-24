package com.finshield.app.data.auth

import com.finshield.app.ui.screens.auth.AuthValidation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthTest {

    @Test
    fun testNameValidation() {
        assertNotNull(AuthValidation.name(""))
        assertNotNull(AuthValidation.name("A"))
        assertNotNull(AuthValidation.name("   "))
        assertNull(AuthValidation.name("Rahul Verma"))
        assertNull(AuthValidation.name("Ananya"))
    }

    @Test
    fun testEmailValidation() {
        assertNotNull(AuthValidation.email(""))
        assertNotNull(AuthValidation.email("invalid-email"))
        assertNotNull(AuthValidation.email("user@"))
        assertNotNull(AuthValidation.email("@domain.com"))
        assertNull(AuthValidation.email("user@finshield.com"))
        assertNull(AuthValidation.email("ananya.sharma@example.co.in"))
    }

    @Test
    fun testLoginPasswordValidation() {
        assertNotNull(AuthValidation.loginPassword(""))
        assertNull(AuthValidation.loginPassword("anyPassword"))
    }

    @Test
    fun testNewPasswordValidation() {
        // Empty
        assertNotNull(AuthValidation.newPassword(""))
        // Too short (< 8 chars)
        assertEquals("Use at least 8 characters", AuthValidation.newPassword("Short1"))
        // Letters only (no digits)
        assertEquals("Include both letters and numbers", AuthValidation.newPassword("PasswordOnly"))
        // Digits only (no letters)
        assertEquals("Include both letters and numbers", AuthValidation.newPassword("123456789"))
        // Valid (>= 8 chars with letters and numbers)
        assertNull(AuthValidation.newPassword("SecurePass123"))
    }

    @Test
    fun testConfirmPasswordValidation() {
        assertNotNull(AuthValidation.confirmPassword("Pass123", ""))
        assertEquals("Passwords do not match", AuthValidation.confirmPassword("Pass123", "Pass456"))
        assertNull(AuthValidation.confirmPassword("Pass123", "Pass123"))
    }

    @Test
    fun testUserProfileModel() {
        val profile = UserProfile(
            uid = "usr_test_123",
            fullName = "Pooja Patel",
            email = "pooja@finshield.com",
            createdAt = null,
            updatedAt = null,
        )

        assertEquals("usr_test_123", profile.uid)
        assertEquals("Pooja Patel", profile.fullName)
        assertEquals("pooja@finshield.com", profile.email)
        assertNull(profile.createdAt)
        assertNull(profile.updatedAt)
    }

    @Test
    fun testAuthRepositoryContract_withTestDouble() = runBlocking {
        // Test-only fake repository strictly verifying the AuthRepository contract
        val repo = FakeTestAuthRepository()
        assertEquals(Session.SignedOut, repo.session.value)
        assertFalse(repo.session.value.isAuthenticated)

        // Register
        val regResult = repo.register("Vikram Malhotra", "vikram@example.com", "Password123")
        assertTrue(regResult.isSuccess)
        val regSession = repo.session.value
        assertTrue(regSession is Session.Authenticated)
        assertTrue(regSession.isAuthenticated)
        assertEquals("Vikram Malhotra", (regSession as Session.Authenticated).displayName)
        assertEquals("vikram@example.com", regSession.email)

        // Sign out
        repo.signOut()
        assertEquals(Session.SignedOut, repo.session.value)
        assertFalse(repo.session.value.isAuthenticated)

        // Sign In
        val signInResult = repo.signIn("priya.nair@example.com", "Password123")
        assertTrue(signInResult.isSuccess)
        val signedInSession = repo.session.value
        assertTrue(signedInSession is Session.Authenticated)
        assertEquals("priya.nair@example.com", (signedInSession as Session.Authenticated).email)

        // Password Reset
        val resetResult = repo.sendPasswordReset("priya.nair@example.com")
        assertTrue(resetResult.isSuccess)

        // Sign out returns to SignedOut
        repo.signOut()
        assertEquals(Session.SignedOut, repo.session.value)
    }

    @Test
    fun testSessionGreetingName() {
        val signedOut: Session = Session.SignedOut
        assertEquals("there", signedOut.greetingName())

        val authNamed: Session = Session.Authenticated(uid = "uid_123", displayName = "Kavita", email = "kavita@test.com")
        assertEquals("Kavita", authNamed.greetingName())
        assertTrue(authNamed.isAuthenticated)

        val authEmailOnly: Session = Session.Authenticated(uid = "uid_456", displayName = null, email = "deepak@test.com")
        assertEquals("deepak", authEmailOnly.greetingName())
        assertTrue(authEmailOnly.isAuthenticated)
    }

    /**
     * Test-only fake AuthRepository isolated within unit tests.
     */
    private class FakeTestAuthRepository : AuthRepository {
        private val _session = MutableStateFlow<Session>(Session.SignedOut)
        override val session: StateFlow<Session> = _session.asStateFlow()

        override suspend fun signIn(email: String, password: String): Result<Session> {
            val s = Session.Authenticated(uid = "test_uid", displayName = null, email = email.trim())
            _session.value = s
            return Result.success(s)
        }

        override suspend fun register(name: String, email: String, password: String): Result<Session> {
            val s = Session.Authenticated(uid = "test_uid_new", displayName = name.trim(), email = email.trim())
            _session.value = s
            return Result.success(s)
        }

        override suspend fun sendPasswordReset(email: String): Result<Unit> {
            return Result.success(Unit)
        }

        override fun signOut() {
            _session.value = Session.SignedOut
        }
    }
}
