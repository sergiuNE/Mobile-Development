package be.ap.nowaste.ui.orders

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import be.ap.nowaste.R
import be.ap.nowaste.data.isOnline
import be.ap.nowaste.data.local.AppDatabase
import be.ap.nowaste.data.local.CachedReservation
import be.ap.nowaste.data.reservation.CannotCancelException
import be.ap.nowaste.data.reservation.ReservationRepository
import be.ap.nowaste.data.reservation.ReservationStatus
import be.ap.nowaste.data.reservation.canCancel
import be.ap.nowaste.data.reservation.isActive
import be.ap.nowaste.data.reservation.splitActiveAndPast
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val CANCEL_TIMEOUT_MS = 15_000L
private const val CLOCK_TICK_MS = 60_000L

/** One reservation as shown on a card: texts are formatted already. */
data class OrderItem(
    val id: String,
    val bagTitle: String,
    val storeName: String,
    val pickupDate: String,
    val pickupStart: String,
    val pickupEnd: String,
    val quantity: Int,
    val status: ReservationStatus,
    /** K11: false when the pickup window started or the reservation is not open anymore. */
    val canCancel: Boolean,
    /** Active: the pickup code is shown. */
    val isActive: Boolean,
    val pickupCode: String,
)

data class OrdersUiState(
    /** True until Room gave the first list. */
    val isLoading: Boolean = true,
    val active: List<OrderItem> = emptyList(),
    val past: List<OrderItem> = emptyList(),
    /** K11: the reservation the confirm dialog is about, or null when no dialog is open. */
    val confirmCancelId: String? = null,
    /** K11: the reservation that is being cancelled right now. */
    val cancellingId: String? = null,
    @StringRes val errorMessage: Int? = null,
    /** The sync with Firestore failed (offline): the list is still shown, from Room. */
    @StringRes val syncNotice: Int? = null,
)

class OrdersViewModel(application: Application) : AndroidViewModel(application) {

    private val cache = AppDatabase.get(application).cachedReservationDao()
    private val repository = ReservationRepository()

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM").withZone(ZoneId.systemDefault())
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    init {
        sync()
        observeOrders()
    }

    /** K10: Firestore -> Room. The screen does not read this directly. */
    private fun sync() {
        viewModelScope.launch {
            repository.syncCustomerReservations(cache)
                .catch { e ->
                    val offline = e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE
                    _uiState.update { it.copy(syncNotice = if (offline) R.string.orders_offline else R.string.error_unknown) }
                }
                .collect { isFromCache -> _uiState.update { it.copy(syncNotice = if (isFromCache) R.string.orders_offline else null) } }
        }
    }

    /** K10: Room -> screen. Offline this still works. The clock moves cards to Past and hides cancel when the time comes. */
    private fun observeOrders() {
        viewModelScope.launch {
            combine(repository.customerOrders(cache), clock()) { orders, now -> orders to now }.collect { (orders, now) ->
                val (active, past) = splitActiveAndPast(orders, now)
                _uiState.update {
                    it.copy(isLoading = false, active = active.map { order -> order.toItem(now) }, past = past.map { order -> order.toItem(now) })
                }
            }
        }
    }

    fun onCancelClick(reservationId: String) =
        _uiState.update { if (it.cancellingId == null) it.copy(confirmCancelId = reservationId, errorMessage = null) else it }

    fun onDismissCancel() = _uiState.update { it.copy(confirmCancelId = null) }

    /** K11: after the confirmation. Room is updated by the repository, so the card changes at once. */
    fun onConfirmCancel() {
        val reservationId = _uiState.value.confirmCancelId ?: return
        _uiState.update { it.copy(confirmCancelId = null) }
        if (!getApplication<Application>().isOnline()) {
            _uiState.update { it.copy(errorMessage = R.string.error_no_internet) }
            return
        }

        _uiState.update { it.copy(cancellingId = reservationId, errorMessage = null) }
        viewModelScope.launch {
            try {
                val done = withTimeoutOrNull(CANCEL_TIMEOUT_MS) { repository.cancel(reservationId, cache) }
                _uiState.update { it.copy(cancellingId = null, errorMessage = if (done == null) R.string.orders_cancel_slow else null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The transaction may hand the exception back wrapped
                val error = when {
                    e is CannotCancelException || e.cause is CannotCancelException -> R.string.orders_cancel_not_possible
                    e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_no_internet
                    else -> R.string.error_unknown
                }
                _uiState.update { it.copy(cancellingId = null, errorMessage = error) }
            }
        }
    }

    private fun clock(): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(CLOCK_TICK_MS)
        }
    }

    private fun CachedReservation.toItem(now: Long): OrderItem {
        val start = Instant.ofEpochMilli(pickupStart)
        return OrderItem(
            id = id,
            bagTitle = bagTitle,
            storeName = storeName,
            pickupDate = dateFormat.format(start),
            pickupStart = timeFormat.format(start),
            pickupEnd = timeFormat.format(Instant.ofEpochMilli(pickupEnd)),
            quantity = quantity,
            status = ReservationStatus.fromValue(status) ?: ReservationStatus.RESERVED,
            canCancel = canCancel(now),
            isActive = isActive(now),
            pickupCode = pickupCode,
        )
    }
}
