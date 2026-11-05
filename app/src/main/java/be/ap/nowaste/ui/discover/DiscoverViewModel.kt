package be.ap.nowaste.ui.discover

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import be.ap.nowaste.R
import be.ap.nowaste.data.geocoding.GeocodingRepository
import be.ap.nowaste.data.geocoding.ZOOM_CITY
import be.ap.nowaste.data.local.AppDatabase
import be.ap.nowaste.data.location.LocationRepository
import be.ap.nowaste.ui.map.Coordinates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

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
)

class DiscoverViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository = LocationRepository(application)
    private val geocodingRepository = GeocodingRepository(AppDatabase.get(application).cachedAddressDao())

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var cityJob: Job? = null

    init {
        // Allowed earlier: no need to explain and ask again
        if (locationRepository.hasPermission()) locate()
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
