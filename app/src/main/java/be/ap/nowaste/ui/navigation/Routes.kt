package be.ap.nowaste.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes. Add arguments as properties, e.g. data class BagDetail(val bagId: String).

@Serializable
data object Discover

@Serializable
data object Orders

@Serializable
data object Favorites
