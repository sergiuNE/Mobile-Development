package be.ap.nowaste.ui.discover

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import be.ap.nowaste.R
import be.ap.nowaste.data.bag.Bag
import be.ap.nowaste.data.bag.BagRepository
import be.ap.nowaste.data.bag.BagWithDistance
import be.ap.nowaste.data.bag.availableBagsByDistance
import be.ap.nowaste.data.bag.decodeBagPhoto
import be.ap.nowaste.data.geocoding.GeocodingRepository
import be.ap.nowaste.data.geocoding.ZOOM_CITY
import be.ap.nowaste.data.local.AppDatabase
import be.ap.nowaste.data.location.LocationRepository
import be.ap.nowaste.data.store.StoreCategory
import be.ap.nowaste.data.store.StoreRepository
import be.ap.nowaste.ui.map.Coordinates
import be.ap.nowaste.ui.map.StoreOnMap
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.concurrent.ConcurrentHashMap

// Thumbnails in the list are 72 dp: about 200 px is sharp enough on most screens
private const val THUMBNAIL_SIZE_PX = 200
// Expired bags have to leave the list, also when nothing changes in Firestore
private const val CLOCK_TICK_MS = 60_000L

/** Choices for the distance filter (K6). */
val DISTANCE_FILTERS_KM = listOf(1, 5, 10)

/** A bag as shown in the list: texts are formatted and the photo is decoded already. */
data class BagListItem(
    val id: String,
    val storeId: String,
    val storeName: String,
    val storeLocation: Coordinates,
    val title: String,
    val price: String,
    val originalValue: String,
    val pickupStart: String,
    val pickupEnd: String,
    val quantityLeft: Int,
    val distanceInKm: Double,
    val photo: ImageBitmap?,
)

enum class LocationStep {
    /** Explain why we need the location, before asking for the permission. */
    ASK_PERMISSION,
    LOCATING,
    PICK_ON_MAP,
    READY,
}

data class DiscoverUiState(
    val step: LocationStep = LocationStep.ASK_PERMISSION,
    /** Current or chosen location: the bags are searched around this point. */
    val location: Coordinates? = null,
    /** Pin on the map that is not confirmed yet. */
    val pin: Coordinates? = null,
    val isApproximate: Boolean = false,
    val isPermanentlyDenied: Boolean = false,
    @StringRes val locationMessage: Int? = null,
    val city: String? = null,
    val isLoadingCity: Boolean = false,
    @StringRes val cityError: Int? = null,
    val bags: List<BagListItem> = emptyList(),
    /** Stores with at least one bag in [bags], for the markers on the map (K4). */
    val stores: List<StoreOnMap> = emptyList(),
    val isLoadingBags: Boolean = true,
    @StringRes val bagsError: Int? = null,
    val isOffline: Boolean = false,
    val maxDistanceKm: Int? = null,
    val category: StoreCategory? = null,
    /** Marker tapped on the map: shows its name and a "View" button. */
    val selectedStoreId: String? = null,
    /** "View" tapped: shows the bags of this store. */
    val viewedStoreId: String? = null,
) {
    val hasFilters: Boolean get() = maxDistanceKm != null || category != null
    val selectedStore: StoreOnMap? get() = stores.find { it.id == selectedStoreId }
    val viewedStore: StoreOnMap? get() = stores.find { it.id == viewedStoreId }
}

private data class BagFilters(val location: Coordinates?, val maxDistanceKm: Int?, val category: StoreCategory?)

/**
 * Shared by the Discover (list) and Map tabs: both show the same bags around the same location, with the same filters.
 */
class DiscoverViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository = LocationRepository(application)
    private val geocodingRepository = GeocodingRepository(AppDatabase.get(application).cachedAddressDao())
    private val bagRepository = BagRepository()
    private val storeRepository = StoreRepository()

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var cityJob: Job? = null
    private var bagsJob: Job? = null

    // bagId -> (photoBase64, decoded photo): a bag is only decoded again when its photo changes
    private val photoCache = ConcurrentHashMap<String, Pair<String, ImageBitmap?>>()
    private val priceFormat = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("EUR") }
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    init {
        // Allowed earlier: no need to explain and ask again
        if (locationRepository.hasPermission()) locate()
        observeBags()
    }

    fun retryBags() = observeBags()

    /** Tap the selected distance again to turn the filter off. */
    fun onDistanceFilter(km: Int) = _uiState.update { it.copy(maxDistanceKm = if (it.maxDistanceKm == km) null else km) }

    fun onCategoryFilter(category: StoreCategory) =
        _uiState.update { it.copy(category = if (it.category == category) null else category) }

    fun onStoreSelected(storeId: String) = _uiState.update { it.copy(selectedStoreId = storeId) }

    fun onMapClick() = _uiState.update { it.copy(selectedStoreId = null) }

    fun onViewStore() = _uiState.update { it.copy(viewedStoreId = it.selectedStoreId) }

    fun onCloseStore() = _uiState.update { it.copy(viewedStoreId = null) }

    /**
     * Realtime list and map (K3, K4, K5): a new snapshot of bags or stores, another location or filter,
     * or a minute later all give a new list.
     */
    private fun observeBags() {
        _uiState.update { it.copy(isLoadingBags = true, bagsError = null) }
        val filters = _uiState.map { BagFilters(it.location, it.maxDistanceKm, it.category) }.distinctUntilChanged()
        bagsJob?.cancel()
        bagsJob = viewModelScope.launch {
            combine(bagRepository.activeBags(), storeRepository.categoriesByStore(), filters, clock()) { result, categories, filters, now ->
                val location = filters.location
                val bags = if (location == null) {
                    emptyList()
                } else {
                    val inCategory = result.bags.filter { filters.category == null || categories[it.storeId] == filters.category }
                    val maxDistance = filters.maxDistanceKm?.let { it * 1000.0 }
                    availableBagsByDistance(inCategory, location.latitude, location.longitude, now, maxDistance)
                        .map { it.toListItem() }
                }
                Triple(bags, bags.toStores(), result.isFromCache)
            }
                // Sorting and decoding photos is too heavy for the main thread
                .flowOn(Dispatchers.Default)
                .catch { e ->
                    val offline = e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE
                    _uiState.update {
                        it.copy(isLoadingBags = false, bagsError = if (offline) R.string.error_no_internet else R.string.error_unknown)
                    }
                }
                .collect { (bags, stores, isFromCache) ->
                    _uiState.update { state ->
                        // A store whose last bag is gone also disappears from the selection
                        val storeIds = stores.map { it.id }.toSet()
                        state.copy(
                            bags = bags,
                            stores = stores,
                            isOffline = isFromCache,
                            isLoadingBags = false,
                            bagsError = null,
                            selectedStoreId = state.selectedStoreId?.takeIf { it in storeIds },
                            viewedStoreId = state.viewedStoreId?.takeIf { it in storeIds },
                        )
                    }
                }
        }
    }

    private fun List<BagListItem>.toStores(): List<StoreOnMap> =
        groupBy { it.storeId }.map { (storeId, bags) ->
            StoreOnMap(id = storeId, name = bags.first().storeName, location = bags.first().storeLocation, bagCount = bags.size)
        }

    private fun clock(): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(CLOCK_TICK_MS)
        }
    }

    private fun BagWithDistance.toListItem() = BagListItem(
        id = bag.id,
        storeId = bag.storeId,
        storeName = bag.storeName,
        storeLocation = Coordinates(bag.latitude, bag.longitude),
        title = bag.title,
        price = priceFormat.format(bag.price),
        originalValue = priceFormat.format(bag.originalValue),
        pickupStart = timeFormat.format(Instant.ofEpochMilli(bag.pickupStart)),
        pickupEnd = timeFormat.format(Instant.ofEpochMilli(bag.pickupEnd)),
        quantityLeft = bag.quantityLeft,
        distanceInKm = distanceInMeters / 1000,
        photo = photoOf(bag),
    )

    private fun photoOf(bag: Bag): ImageBitmap? {
        photoCache[bag.id]?.let { (base64, photo) -> if (base64 == bag.photoBase64) return photo }
        val photo = decodeBagPhoto(bag.photoBase64, THUMBNAIL_SIZE_PX)?.asImageBitmap()
        photoCache[bag.id] = bag.photoBase64 to photo
        return photo
    }

    fun onPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        if (granted) {
            locate()
        } else {
            _uiState.update {
                it.copy(
                    step = LocationStep.PICK_ON_MAP,
                    isPermanentlyDenied = permanentlyDenied,
                    locationMessage = if (permanentlyDenied) R.string.location_permanently_denied else R.string.location_denied,
                )
            }
        }
    }

    /** Back from the app settings: the user may have allowed the location there. */
    fun onResume() {
        if (_uiState.value.isPermanentlyDenied && locationRepository.hasPermission()) locate()
    }

    fun onChooseOnMap() = _uiState.update {
        it.copy(step = LocationStep.PICK_ON_MAP, pin = it.location, locationMessage = null)
    }

    /** Only possible when there is a location to go back to. */
    fun onCancelPick() = _uiState.update {
        if (it.location == null) it else it.copy(step = LocationStep.READY, pin = null, locationMessage = null)
    }

    fun onPinPlaced(pin: Coordinates) = _uiState.update { it.copy(pin = pin) }

    fun onConfirmPin() {
        val pin = _uiState.value.pin ?: return
        _uiState.update { it.copy(isApproximate = false) }
        useLocation(pin)
    }

    fun retryCity() {
        _uiState.value.location?.let(::loadCity)
    }

    private fun locate() {
        _uiState.update {
            it.copy(
                step = LocationStep.LOCATING,
                isPermanentlyDenied = false,
                isApproximate = !locationRepository.hasPreciseLocation(),
                locationMessage = null,
            )
        }
        viewModelScope.launch {
            val location = locationRepository.currentLocation()
            if (location == null) {
                _uiState.update { it.copy(step = LocationStep.PICK_ON_MAP, locationMessage = R.string.location_unavailable) }
            } else {
                useLocation(location)
            }
        }
    }

    private fun useLocation(location: Coordinates) {
        _uiState.update { it.copy(step = LocationStep.READY, location = location, locationMessage = null) }
        loadCity(location)
    }

    private fun loadCity(location: Coordinates) {
        _uiState.update { it.copy(city = null, isLoadingCity = true, cityError = null) }
        // A newer location wins: the city of an older one must not overwrite it
        cityJob?.cancel()
        cityJob = viewModelScope.launch {
            try {
                val address = geocodingRepository.reverseGeocode(location.latitude, location.longitude, ZOOM_CITY)
                // No city (sea, forest): the title falls back to "Bags near you"
                _uiState.update { it.copy(isLoadingCity = false, city = address.city.ifBlank { null }) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _uiState.update { it.copy(isLoadingCity = false, cityError = R.string.discover_error_city_offline) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingCity = false, cityError = R.string.discover_error_city) }
            }
        }
    }
}
