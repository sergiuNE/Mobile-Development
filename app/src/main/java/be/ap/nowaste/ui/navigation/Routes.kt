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
data object Orders

@Serializable
data object Favorites
