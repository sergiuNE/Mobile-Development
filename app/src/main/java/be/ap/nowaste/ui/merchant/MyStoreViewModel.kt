package be.ap.nowaste.ui.merchant

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import be.ap.nowaste.R
import be.ap.nowaste.data.geocoding.GeocodingRepository
import be.ap.nowaste.data.isOnline
import be.ap.nowaste.data.local.AppDatabase
import be.ap.nowaste.data.store.StoreCategory
import be.ap.nowaste.data.store.StoreRepository
import be.ap.nowaste.ui.map.Coordinates
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

data class MyStoreUiState(
    val isLoading: Boolean = true,
    @StringRes val loadError: Int? = null,
    val name: String = "",
    val category: StoreCategory? = null,
    val location: Coordinates? = null,
    val street: String = "",
    val postcode: String = "",
    val city: String = "",
    val isGeocoding: Boolean = false,
    val isSaving: Boolean = false,
    val hasStore: Boolean = false,
    @StringRes val nameError: Int? = null,
    @StringRes val categoryError: Int? = null,
    @StringRes val locationError: Int? = null,
    @StringRes val addressError: Int? = null,
    @StringRes val errorMessage: Int? = null,
    @StringRes val infoMessage: Int? = null,
)

private const val SAVE_TIMEOUT_MS = 15_000L

class MyStoreViewModel(application: Application) : AndroidViewModel(application) {

    private val storeRepository = StoreRepository()
    private val geocodingRepository = GeocodingRepository(AppDatabase.get(application).cachedAddressDao())

    private val _uiState = MutableStateFlow(MyStoreUiState())
    val uiState: StateFlow<MyStoreUiState> = _uiState.asStateFlow()

    private var geocodeJob: Job? = null

    init {
        loadStore()
    }

    fun loadStore() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            try {
                val store = storeRepository.getMyStore()
                _uiState.update { state ->
                    if (store == null) {
                        state.copy(isLoading = false)
                    } else {
                        state.copy(
                            isLoading = false,
                            hasStore = true,
                            name = store.name,
                            category = store.category,
                            location = Coordinates(store.latitude, store.longitude),
                            street = store.street,
                            postcode = store.postcode,
                            city = store.city,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val offline = e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE
                _uiState.update {
                    it.copy(isLoading = false, loadError = if (offline) R.string.error_no_internet else R.string.error_unknown)
                }
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, nameError = null, infoMessage = null) }

    fun onCategoryChange(category: StoreCategory) =
        _uiState.update { it.copy(category = category, categoryError = null, infoMessage = null) }

    // Typing the address yourself solves a failed lookup, so its error disappears too
    fun onStreetChange(street: String) =
        _uiState.update { it.copy(street = street, addressError = null, errorMessage = null, infoMessage = null) }

    fun onPostcodeChange(postcode: String) =
        _uiState.update { it.copy(postcode = postcode, addressError = null, errorMessage = null, infoMessage = null) }

    fun onCityChange(city: String) =
        _uiState.update { it.copy(city = city, addressError = null, errorMessage = null, infoMessage = null) }

    /** Called when the pin is placed or released after dragging, never while dragging (Nominatim usage policy). */
    fun onLocationPicked(location: Coordinates) {
        // Clear the old address: if the lookup fails, it must not be saved with the new location
        _uiState.update {
            it.copy(
                location = location,
                street = "",
                postcode = "",
                city = "",
                locationError = null,
                isGeocoding = true,
                errorMessage = null,
                infoMessage = null,
            )
        }
        // A newer pin wins: the address of an older pin must not overwrite it
        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch {
            try {
                val address = geocodingRepository.reverseGeocode(location.latitude, location.longitude)
                _uiState.update { state ->
                    if (address.isEmpty) {
                        state.copy(isGeocoding = false, errorMessage = R.string.store_error_no_address)
                    } else {
                        state.copy(
                            isGeocoding = false,
                            street = address.street,
                            postcode = address.postcode,
                            city = address.city,
                            addressError = null,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _uiState.update { it.copy(isGeocoding = false, errorMessage = R.string.store_error_geocoding_offline) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGeocoding = false, errorMessage = R.string.store_error_geocoding) }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        // Wait for the address of the new pin, otherwise location and address do not match
        if (state.isGeocoding || state.isSaving) return
        val nameError = if (state.name.isBlank()) R.string.store_error_name_required else null
        val categoryError = if (state.category == null) R.string.store_error_category_required else null
        val locationError = if (state.location == null) R.string.store_error_location_required else null
        val addressError =
            if (state.street.isBlank() || state.postcode.isBlank() || state.city.isBlank()) R.string.store_error_address_required else null
        if (nameError != null || categoryError != null || locationError != null || addressError != null) {
            _uiState.update {
                it.copy(nameError = nameError, categoryError = categoryError, locationError = locationError, addressError = addressError)
            }
            return
        }
        if (!getApplication<Application>().isOnline()) {
            _uiState.update { it.copy(errorMessage = R.string.error_no_internet) }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            try {
                // Firestore only finishes a write when the server confirms it: give up after a while instead of spinning forever
                val saved = withTimeoutOrNull(SAVE_TIMEOUT_MS) {
                    storeRepository.saveMyStore(
                        name = state.name.trim(),
                        category = state.category!!,
                        latitude = state.location!!.latitude,
                        longitude = state.location.longitude,
                        street = state.street.trim(),
                        postcode = state.postcode.trim(),
                        city = state.city.trim(),
                    )
                }
                _uiState.update {
                    if (saved == null) {
                        it.copy(isSaving = false, errorMessage = R.string.error_no_internet)
                    } else {
                        it.copy(isSaving = false, hasStore = true, infoMessage = R.string.store_saved)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = R.string.error_unknown) }
            }
        }
    }
}
