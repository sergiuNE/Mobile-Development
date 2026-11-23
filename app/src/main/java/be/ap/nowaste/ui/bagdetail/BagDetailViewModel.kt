package be.ap.nowaste.ui.bagdetail

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import be.ap.nowaste.R
import be.ap.nowaste.data.bag.Bag
import be.ap.nowaste.data.bag.BagRepository
import be.ap.nowaste.data.bag.decodeBagPhoto
import be.ap.nowaste.data.isOnline
import be.ap.nowaste.data.reservation.NotEnoughBagsException
import be.ap.nowaste.data.reservation.ReservationRepository
import be.ap.nowaste.data.store.StoreRepository
import be.ap.nowaste.ui.map.Coordinates
import be.ap.nowaste.ui.navigation.BagDetail
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency

// The photo fills the width of the screen
private const val PHOTO_SIZE_PX = 800
private const val RESERVE_TIMEOUT_MS = 15_000L

/** The bag as shown on the detail screen: texts are formatted and the photo is decoded already. */
data class BagDetailItem(
    val storeName: String,
    val title: String,
    val description: String,
    val price: String,
    val originalValue: String,
    val pickupStart: String,
    val pickupEnd: String,
    val quantityLeft: Int,
    val storeLocation: Coordinates,
    /** False when it is sold out, offline or the pickup window is over: then it cannot be reserved. */
    val isAvailable: Boolean,
)

data class BagDetailUiState(
    val isLoading: Boolean = true,
    @StringRes val loadError: Int? = null,
    val bag: BagDetailItem? = null,
    val photo: ImageBitmap? = null,
    /** Null until the store is loaded, or when it could not be loaded. */
    val address: String? = null,
    /** K8: how many bags to reserve, from 1 to what is left. */
    val quantity: Int = 1,
    val isReserving: Boolean = false,
    @StringRes val errorMessage: Int? = null,
    /** K9: set after a successful reservation. */
    val pickupCode: String? = null,
    /** Kept apart from [quantity]: after reserving, fewer are left and [quantity] gets lowered to that. */
    val reservedQuantity: Int = 0,
)

class BagDetailViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val bagId: String = savedStateHandle.toRoute<BagDetail>().bagId
    private val bagRepository = BagRepository()
    private val storeRepository = StoreRepository()
    private val reservationRepository = ReservationRepository()

    private val _uiState = MutableStateFlow(BagDetailUiState())
    val uiState: StateFlow<BagDetailUiState> = _uiState.asStateFlow()

    private val priceFormat = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("EUR") }
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    private var bagJob: Job? = null
    private var decodedPhotoBase64: String? = null
    private var loadedStoreId: String? = null

    init {
        observeBag()
    }

    fun retryLoad() = observeBag()

    /** Realtime (like K5): when someone else reserves, the number left and the maximum quantity drop at once. */
    private fun observeBag() {
        _uiState.update { it.copy(isLoading = it.bag == null, loadError = null) }
        bagJob?.cancel()
        bagJob = viewModelScope.launch {
            bagRepository.bag(bagId)
                .catch { e ->
                    val offline = e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE
                    _uiState.update { it.copy(isLoading = false, loadError = if (offline) R.string.error_no_internet else R.string.error_unknown) }
                }
                .collect { bag ->
                    if (bag == null) {
                        _uiState.update { it.copy(isLoading = false, bag = null, loadError = R.string.bag_detail_not_found) }
                        return@collect
                    }
                    val item = bag.toDetailItem()
                    _uiState.update {
                        it.copy(isLoading = false, loadError = null, bag = item, quantity = it.quantity.coerceIn(1, maxOf(1, bag.quantityLeft)))
                    }
                    decodePhoto(bag)
                    loadAddress(bag.storeId)
                }
        }
    }

    /** Only when the photo changed: decoding is slow, so it happens on Dispatchers.Default. */
    private suspend fun decodePhoto(bag: Bag) {
        if (bag.photoBase64 == decodedPhotoBase64) return
        decodedPhotoBase64 = bag.photoBase64
        val photo = withContext(Dispatchers.Default) { decodeBagPhoto(bag.photoBase64, PHOTO_SIZE_PX)?.asImageBitmap() }
        _uiState.update { it.copy(photo = photo) }
    }

    /** Bags do not have an address, so it comes from the store. Without it the screen still works. */
    private fun loadAddress(storeId: String) {
        if (storeId == loadedStoreId) return
        loadedStoreId = storeId
        viewModelScope.launch {
            val address = try {
                storeRepository.getStore(storeId)?.address?.ifBlank { null }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadedStoreId = null
                null
            }
            _uiState.update { it.copy(address = address) }
        }
    }

    fun onIncrease() = _uiState.update { state ->
        val max = state.bag?.quantityLeft ?: 1
        state.copy(quantity = (state.quantity + 1).coerceAtMost(max), errorMessage = null)
    }

    fun onDecrease() = _uiState.update { it.copy(quantity = (it.quantity - 1).coerceAtLeast(1), errorMessage = null) }

    /** K8: reserve in a transaction. K9: on success the pickup code appears. */
    fun reserve() {
        val state = _uiState.value
        if (state.isReserving || state.pickupCode != null || state.bag?.isAvailable != true) return
        if (!getApplication<Application>().isOnline()) {
            _uiState.update { it.copy(errorMessage = R.string.error_no_internet) }
            return
        }

        _uiState.update { it.copy(isReserving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val code = withTimeoutOrNull(RESERVE_TIMEOUT_MS) { reservationRepository.reserve(bagId, state.quantity) }
                _uiState.update {
                    if (code == null) {
                        it.copy(isReserving = false, errorMessage = R.string.reserve_error_slow)
                    } else {
                        it.copy(isReserving = false, pickupCode = code, reservedQuantity = state.quantity)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The transaction may hand the exception back wrapped
                val error = when {
                    e is NotEnoughBagsException || e.cause is NotEnoughBagsException -> R.string.reserve_error_not_enough
                    e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_no_internet
                    else -> R.string.error_unknown
                }
                _uiState.update { it.copy(isReserving = false, errorMessage = error) }
            }
        }
    }

    private fun Bag.toDetailItem() = BagDetailItem(
        storeName = storeName,
        title = title,
        description = description,
        price = priceFormat.format(price),
        originalValue = priceFormat.format(originalValue),
        pickupStart = timeFormat.format(Instant.ofEpochMilli(pickupStart)),
        pickupEnd = timeFormat.format(Instant.ofEpochMilli(pickupEnd)),
        quantityLeft = quantityLeft,
        storeLocation = Coordinates(latitude, longitude),
        isAvailable = isAvailable(System.currentTimeMillis()),
    )
}
