package be.ap.nowaste.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes. Add arguments as properties, e.g. data class BagDetail(val bagId: String).

// Top level: one home per role
@Serializable
data object Login

@Serializable
data object Register

@Serializable
data object CustomerHome

@Serializable
data object MerchantHome

// Customer tabs inside CustomerHome
@Serializable
data object Discover

@Serializable
data object BagsMap

@Serializable
data object Orders

@Serializable
data object Favorites

/** Bag detail with reserve button (K7, K8, K9), opened from the list or the map. */
@Serializable
data class BagDetail(val bagId: String)

// Merchant tabs inside MerchantHome
@Serializable
data object MyBags

@Serializable
data object MyStore

/** Create a bag (H3) when [bagId] is null, otherwise edit it (H7). */
@Serializable
data class BagForm(val bagId: String? = null)
