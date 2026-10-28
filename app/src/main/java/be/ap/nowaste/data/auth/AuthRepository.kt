package be.ap.nowaste.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val firestore: FirebaseFirestore = Firebase.firestore,
) {
    private val users = firestore.collection("users")

    /** Role of the logged-in user, or null when nobody is logged in. */
    suspend fun currentUserRole(): UserRole? {
        val uid = auth.currentUser?.uid ?: return null
        return mapErrors { fetchRole(uid) }
    }

    suspend fun login(email: String, password: String): UserRole = mapErrors {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        fetchRole(result.user!!.uid)
    }

    suspend fun register(name: String, email: String, password: String, role: UserRole): UserRole =
        mapErrors {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val profile = mapOf("name" to name, "email" to email, "role" to role.value)
            try {
                users.document(result.user!!.uid).set(profile).await()
            } catch (e: Exception) {
                // Without a profile the account has no role, so remove it and let the user retry
                result.user?.delete()
                throw e
            }
            role
        }

    suspend fun sendPasswordReset(email: String) = mapErrors {
        auth.sendPasswordResetEmail(email).await()
        Unit
    }

    fun logout() = auth.signOut()

    private suspend fun fetchRole(uid: String): UserRole {
        val snapshot = users.document(uid).get().await()
        return UserRole.fromValue(snapshot.getString("role")) ?: run {
            // Sign out, otherwise the app would get stuck on this account at every launch
            auth.signOut()
            throw AuthException(AuthError.PROFILE_MISSING)
        }
    }

    private suspend fun <T> mapErrors(block: suspend () -> T): T = try {
        block()
    } catch (e: AuthException) {
        throw e
    } catch (e: Exception) {
        throw AuthException(e.toAuthError(), e)
    }

    private fun Exception.toAuthError(): AuthError = when (this) {
        // WeakPassword extends InvalidCredentials, so it has to come first
        is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
        is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException ->
            AuthError.INVALID_CREDENTIALS
        is FirebaseAuthUserCollisionException -> AuthError.EMAIL_IN_USE
        is FirebaseNetworkException -> AuthError.NO_INTERNET
        is FirebaseFirestoreException ->
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE) AuthError.NO_INTERNET else AuthError.UNKNOWN
        else -> AuthError.UNKNOWN
    }
}
