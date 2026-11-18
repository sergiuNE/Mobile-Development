package be.ap.nowaste.ui.merchant.bags

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import be.ap.nowaste.R
import be.ap.nowaste.data.bag.Bag
import be.ap.nowaste.data.bag.BagRepository
import be.ap.nowaste.data.bag.decodeBagPhoto
import be.ap.nowaste.data.isOnline
import be.ap.nowaste.data.reservation.Reservation
import be.ap.nowaste.data.reservation.ReservationNotOpenException
import be.ap.nowaste.data.reservation.ReservationRepository
import be.ap.nowaste.data.reservation.findOpenReservation
import be.ap.nowaste.data.reservation.reservedQuantityByBag
import be.ap.nowaste.data.store.StoreRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.concurrent.ConcurrentHashMap

private const val THUMBNAIL_SIZE_PX = 160
private const val WRITE_TIMEOUT_MS = 15_000L

/** A bag as shown in "My bags": texts are formatted and the photo is decoded already. */
data class MerchantBagItem(
    val id: String,
    val title: String,
    val price: String,
    val originalValue: String,
    val pickupStart: String,
    val pickupEnd: String,
    val quantityLeft: Int,
    val quantityTotal: Int,
    /** H5: bags reserved by customers, collected ones included. */
    val reserved: Int,
    val active: Boolean,
    val isPickupOver: Boolean,
    val photo: ImageBitmap?,
)

/** H6: reservation found with the pickup code, waiting for "Mark as collected". */
data class FoundReservation(val id: String, val bagTitle: String, val quantity: Int)

data class MyBagsUiState(
    val isLoading: Boolean = true,
    /** Without a store there is no name and location to copy into a bag. */
    val hasStore: Boolean = true,
    @StringRes val loadError: Int? = null,
    val bags: List<MerchantBagItem> = emptyList(),
    /** Bags whose online/offline switch is being saved. */
    val updatingBagIds: Set<String> = emptySet(),
    @StringRes val message: Int? = null,
    val isCodeDialogOpen: Boolean = false,
    val code: String = "",
    @StringRes val codeError: Int? = null,
    val foundReservation: FoundReservation? = null,
    val isMarkingCollected: Boolean = false,
)

class MyBagsViewModel(application: Application) : AndroidViewModel(application) {

    private val storeRepository = StoreRepository()
    private val bagRepository = BagRepository()
    private val reservationRepository = ReservationRepository()

    private val _uiState = MutableStateFlow(MyBagsUiState())
    val uiState: StateFlow<MyBagsUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    // Newest snapshots, to look up a pickup code (H6) without asking Firestore again
    private var reservations: List<Reservation> = emptyList()
    private var bagTitles: Map<String, String> = emptyMap()

    // bagId -> (photoBase64, decoded photo): a bag is only decoded again when its photo changes
    private val photoCache = ConcurrentHashMap<String, Pair<String, ImageBitmap?>>()
    private val priceFormat = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("EUR") }
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            val store = try {
                storeRepository.getMyStore()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadError = e.toMessage()) }
                return@launch
            }
            if (store == null) {
                _uiState.update { it.copy(isLoading = false, hasStore = false) }
                return@launch
            }

            // H5: bags and reservations in realtime, so a new reservation shows up right away
            combine(bagRepository.storeBags(store.id), reservationRepository.storeReservations(store.id)) { bags, reservations ->
                val reserved = reservedQuantityByBag(reservations)
                val now = System.currentTimeMillis()
                Triple(bags.map { it.toItem(reserved[it.id] ?: 0, now) }, reservations, bags.associate { it.id to it.title })
            }
                // Decoding photos is too heavy for the main thread
                .flowOn(Dispatchers.Default)
                .catch { e -> _uiState.update { it.copy(isLoading = false, loadError = e.toMessage()) } }
                .collect { (items, newReservations, titles) ->
                    // Set on the main thread, where checkCode reads them
                    reservations = newReservations
                    bagTitles = titles
                    _uiState.update { it.copy(isLoading = false, hasStore = true, bags = items) }
                }
        }
    }

    /** H7: take a bag offline or put it back online. */
    fun setActive(bagId: String, active: Boolean) {
        if (!getApplication<Application>().isOnline()) {
            _uiState.update { it.copy(message = R.string.error_no_internet) }
            return
        }
        _uiState.update { it.copy(updatingBagIds = it.updatingBagIds + bagId, message = null) }
        viewModelScope.launch {
            val message = try {
                val done = withTimeoutOrNull(WRITE_TIMEOUT_MS) { bagRepository.setActive(bagId, active) }
                when {
                    // Firestore keeps the write and finishes it when the connection is back
                    done == null -> R.string.error_slow_connection
                    active -> R.string.bags_now_online
                    else -> R.string.bags_now_offline
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.toMessage()
            }
            _uiState.update { it.copy(updatingBagIds = it.updatingBagIds - bagId, message = message) }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    fun openCodeDialog() =
        _uiState.update { it.copy(isCodeDialogOpen = true, code = "", codeError = null, foundReservation = null) }

    fun closeCodeDialog() = _uiState.update { it.copy(isCodeDialogOpen = false, foundReservation = null) }

    fun onCodeChange(code: String) =
        _uiState.update { it.copy(code = code.take(PICKUP_CODE_INPUT_LENGTH), codeError = null, foundReservation = null) }

    /** H6: look the code up in the open reservations of this store. */
    fun checkCode() {
        val reservation = findOpenReservation(reservations, _uiState.value.code)
        _uiState.update {
            if (reservation == null) {
                it.copy(codeError = R.string.bags_code_not_found)
            } else {
                val title = bagTitles[reservation.bagId].orEmpty()
                it.copy(foundReservation = FoundReservation(reservation.id, title, reservation.quantity))
            }
        }
    }

    fun markCollected() {
        val reservation = _uiState.value.foundReservation ?: return
        if (!getApplication<Application>().isOnline()) {
            _uiState.update { it.copy(codeError = R.string.error_no_internet) }
            return
        }
        _uiState.update { it.copy(isMarkingCollected = true) }
        viewModelScope.launch {
            try {
                val done = withTimeoutOrNull(WRITE_TIMEOUT_MS) { reservationRepository.markCollected(reservation.id) }
                _uiState.update {
                    if (done == null) {
                        it.copy(isMarkingCollected = false, codeError = R.string.error_no_internet)
                    } else {
                        it.copy(isMarkingCollected = false, isCodeDialogOpen = false, foundReservation = null, message = R.string.bags_collected)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The transaction may hand the exception back wrapped
                val error = if (e is ReservationNotOpenException || e.cause is ReservationNotOpenException) {
                    R.string.bags_code_no_longer_open
                } else {
                    e.toMessage()
                }
                _uiState.update { it.copy(isMarkingCollected = false, foundReservation = null, codeError = error) }
            }
        }
    }

    private fun Bag.toItem(reserved: Int, now: Long) = MerchantBagItem(
        id = id,
        title = title,
        price = priceFormat.format(price),
        originalValue = priceFormat.format(originalValue),
        pickupStart = timeFormat.format(Instant.ofEpochMilli(pickupStart)),
        pickupEnd = timeFormat.format(Instant.ofEpochMilli(pickupEnd)),
        quantityLeft = quantityLeft,
        quantityTotal = quantityTotal,
        reserved = reserved,
        active = active,
        isPickupOver = pickupEnd <= now,
        photo = photoFor(this),
    )

    private fun photoFor(bag: Bag): ImageBitmap? {
        photoCache[bag.id]?.let { (base64, photo) -> if (base64 == bag.photoBase64) return photo }
        val photo = decodeBagPhoto(bag.photoBase64, THUMBNAIL_SIZE_PX)?.asImageBitmap()
        photoCache[bag.id] = bag.photoBase64 to photo
        return photo
    }

    @StringRes
    private fun Throwable.toMessage(): Int =
        if (this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.UNAVAILABLE) {
            R.string.error_no_internet
        } else {
            R.string.error_unknown
        }
}

// Pickup codes have 6 characters (K9); a little room for a space typed by mistake
private const val PICKUP_CODE_INPUT_LENGTH = 8
