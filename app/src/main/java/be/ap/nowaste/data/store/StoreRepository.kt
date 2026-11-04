package be.ap.nowaste.data.store

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class StoreRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val firestore: FirebaseFirestore = Firebase.firestore,
) {
    private val stores = firestore.collection("stores")

    // One store per merchant: the document id is the uid of the owner
    private val myStoreDocument get() = stores.document(requireNotNull(auth.currentUser).uid)

    suspend fun getMyStore(): Store? = myStoreDocument.get().await().toStore()

    suspend fun saveMyStore(
        name: String,
        category: StoreCategory,
        latitude: Double,
        longitude: Double,
        street: String,
        postcode: String,
        city: String,
    ) {
        val document = myStoreDocument
        val data = mapOf(
            "ownerId" to document.id,
            "name" to name,
            "category" to category.value,
            "location" to GeoPoint(latitude, longitude),
            "address" to formatAddress(street, postcode, city),
            "city" to city,
            // Extra fields, so the merchant can edit street and postcode separately
            "street" to street,
            "postcode" to postcode,
        )
        document.set(data).await()
    }

    private fun DocumentSnapshot.toStore(): Store? {
        if (!exists()) return null
        val location = getGeoPoint("location") ?: return null
        return Store(
            id = id,
            ownerId = getString("ownerId").orEmpty(),
            name = getString("name").orEmpty(),
            category = StoreCategory.fromValue(getString("category")) ?: StoreCategory.OTHER,
            latitude = location.latitude,
            longitude = location.longitude,
            street = getString("street").orEmpty(),
            postcode = getString("postcode").orEmpty(),
            city = getString("city").orEmpty(),
        )
    }
}
