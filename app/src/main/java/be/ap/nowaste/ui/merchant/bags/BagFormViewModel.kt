package be.ap.nowaste.ui.merchant.bags

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import be.ap.nowaste.R
import be.ap.nowaste.data.bag.BagInput
import be.ap.nowaste.data.bag.BagRepository
import be.ap.nowaste.data.bag.BelowReservedException
import be.ap.nowaste.data.bag.decodeBagPhoto
import be.ap.nowaste.data.bag.encodeBagPhoto
import be.ap.nowaste.data.bag.loadPhoto
import be.ap.nowaste.data.bag.minutesOfDay
import be.ap.nowaste.data.bag.parsePrice
import be.ap.nowaste.data.isOnline
import be.ap.nowaste.data.store.StoreRepository
import be.ap.nowaste.ui.navigation.BagForm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalTime

private const val PREVIEW_SIZE_PX = 600
private const val SAVE_TIMEOUT_MS = 15_000L
const val MAX_BAG_QUANTITY = 99

data class BagFormUiState(
    val isEdit: Boolean = false,
    /** Only when editing: the bag is loaded first. */
    val isLoading: Boolean = false,
    @StringRes val loadError: Int? = null,
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val originalValue: String = "",
    val quantity: String = "",
    /** Minutes after midnight, today. */
    val pickupStart: Int? = null,
    val pickupEnd: Int? = null,
    val photo: ImageBitmap? = null,
    val photoBase64: String = "",
    val isProcessingPhoto: Boolean = false,
    @StringRes val titleError: Int? = null,
    @StringRes val descriptionError: Int? = null,
    @StringRes val priceError: Int? = null,
    @StringRes val originalValueError: Int? = null,
    @StringRes val quantityError: Int? = null,
    @StringRes val pickupError: Int? = null,
    @StringRes val photoError: Int? = null,
    @StringRes val errorMessage: Int? = null,
    val isSaving: Boolean = false,
    /** Saved: the screen goes back to "My bags". */
    val isSaved: Boolean = false,
)

class BagFormViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val bagId: String? = savedStateHandle.toRoute<BagForm>().bagId
    private val bagRepository = BagRepository()
    // Chosen once: after a timeout, Firestore may still save the first try, and the next try must not make a second bag
    private val newBagId: String by lazy { bagRepository.newBagId() }
    private val storeRepository = StoreRepository()

    private val _uiState = MutableStateFlow(BagFormUiState(isEdit = bagId != null, isLoading = bagId != null))
    val uiState: StateFlow<BagFormUiState> = _uiState.asStateFlow()

    init {
        if (bagId != null) loadBag(bagId)
    }

    fun retryLoad() {
        bagId?.let(::loadBag)
    }

    private fun loadBag(id: String) {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            try {
                val bag = bagRepository.getBag(id)
                if (bag == null) {
                    _uiState.update { it.copy(isLoading = false, loadError = R.string.error_unknown) }
                    return@launch
                }
                val preview = withContext(Dispatchers.Default) { decodeBagPhoto(bag.photoBase64, PREVIEW_SIZE_PX)?.asImageBitmap() }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        title = bag.title,
                        description = bag.description,
                        price = bag.price.toPlainPrice(),
                        originalValue = bag.originalValue.toPlainPrice(),
                        quantity = bag.quantityTotal.toString(),
                        pickupStart = minutesOfDay(bag.pickupStart),
                        pickupEnd = minutesOfDay(bag.pickupEnd),
                        photo = preview,
                        photoBase64 = bag.photoBase64,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadError = R.string.error_no_internet) }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, titleError = null, errorMessage = null) }

    fun onDescriptionChange(value: String) =
        _uiState.update { it.copy(description = value, descriptionError = null, errorMessage = null) }

    fun onPriceChange(value: String) =
        _uiState.update { it.copy(price = value, priceError = null, originalValueError = null, errorMessage = null) }

    fun onOriginalValueChange(value: String) =
        _uiState.update { it.copy(originalValue = value, originalValueError = null, priceError = null, errorMessage = null) }

    fun onQuantityChange(value: String) =
        _uiState.update { it.copy(quantity = value.filter { c -> c.isDigit() }.take(2), quantityError = null, errorMessage = null) }

    fun onPickupStartChange(minutes: Int) = _uiState.update { it.copy(pickupStart = minutes, pickupError = null) }

    fun onPickupEndChange(minutes: Int) = _uiState.update { it.copy(pickupEnd = minutes, pickupError = null) }

    /** H4: photo from the camera (TakePicturePreview). */
    fun onPhotoTaken(photo: Bitmap?) {
        if (photo != null) processPhoto { photo }
    }

    /** H4: photo from the gallery (PickVisualMedia). */
    fun onPhotoPicked(uri: Uri?) {
        if (uri != null) processPhoto { loadPhoto(getApplication<Application>().contentResolver, uri) }
    }

    fun onNoCamera() = _uiState.update { it.copy(photoError = R.string.bag_error_no_camera) }

    private fun processPhoto(load: () -> Bitmap?) {
        _uiState.update { it.copy(isProcessingPhoto = true, photoError = null) }
        viewModelScope.launch {
            try {
                // Scale, compress and encode off the main thread; the preview shows exactly what customers will see
                val (base64, preview) = withContext(Dispatchers.Default) {
                    val bitmap = load() ?: return@withContext null
                    val base64 = encodeBagPhoto(bitmap)
                    base64 to decodeBagPhoto(base64, PREVIEW_SIZE_PX)?.asImageBitmap()
                } ?: (null to null)
                _uiState.update {
                    if (base64 == null) {
                        it.copy(isProcessingPhoto = false, photoError = R.string.bag_error_photo_unreadable)
                    } else {
                        it.copy(isProcessingPhoto = false, photoBase64 = base64, photo = preview)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessingPhoto = false, photoError = R.string.bag_error_photo_unreadable) }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isProcessingPhoto) return
        val input = validate(state) ?: return
        if (!getApplication<Application>().isOnline()) {
            _uiState.update { it.copy(errorMessage = R.string.error_no_internet) }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val error = try {
                when (withTimeoutOrNull(SAVE_TIMEOUT_MS) { write(input) }) {
                    SaveResult.SAVED -> null
                    SaveResult.NO_STORE -> R.string.bags_no_store
                    // Firestore keeps the write and finishes it when the connection is back
                    null -> R.string.error_slow_connection
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The transaction may hand the exception back wrapped
                if (e is BelowReservedException || e.cause is BelowReservedException) R.string.bag_error_below_reserved else R.string.error_unknown
            }
            _uiState.update {
                if (error == null) it.copy(isSaving = false, isSaved = true) else it.copy(isSaving = false, errorMessage = error)
            }
        }
    }

    private enum class SaveResult { SAVED, NO_STORE }

    private suspend fun write(input: BagInput): SaveResult {
        if (bagId != null) {
            bagRepository.updateBag(bagId, input)
        } else {
            val store = storeRepository.getMyStore() ?: return SaveResult.NO_STORE
            bagRepository.createBag(newBagId, store, input)
        }
        return SaveResult.SAVED
    }

    /** Sets the field errors and returns the input when everything is valid. */
    private fun validate(state: BagFormUiState): BagInput? {
        val price = parsePrice(state.price)
        val originalValue = parsePrice(state.originalValue)
        val quantity = state.quantity.toIntOrNull()
        val now = LocalTime.now().let { it.hour * 60 + it.minute }
        val errors = BagFormErrors(
            title = if (state.title.isBlank()) R.string.bag_error_title_required else null,
            description = if (state.description.isBlank()) R.string.bag_error_description_required else null,
            price = if (price == null) R.string.bag_error_price_invalid else null,
            originalValue = when {
                originalValue == null -> R.string.bag_error_price_invalid
                price != null && price >= originalValue -> R.string.bag_error_price_not_lower
                else -> null
            },
            quantity = if (quantity == null || quantity !in 1..MAX_BAG_QUANTITY) R.string.bag_error_quantity else null,
            pickup = pickupError(state.pickupStart, state.pickupEnd, now),
            photo = if (state.photoBase64.isEmpty()) R.string.bag_error_photo_required else null,
        )
        _uiState.update {
            it.copy(
                titleError = errors.title,
                descriptionError = errors.description,
                priceError = errors.price,
                originalValueError = errors.originalValue,
                quantityError = errors.quantity,
                pickupError = errors.pickup,
                photoError = errors.photo,
            )
        }
        if (errors.hasAny) return null
        return BagInput(
            title = state.title.trim(),
            description = state.description.trim(),
            price = price!!,
            originalValue = originalValue!!,
            quantityTotal = quantity!!,
            pickupStartMinutes = state.pickupStart!!,
            pickupEndMinutes = state.pickupEnd!!,
            photoBase64 = state.photoBase64,
        )
    }

    private fun Double.toPlainPrice(): String = BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP).toPlainString()
}

private data class BagFormErrors(
    val title: Int?,
    val description: Int?,
    val price: Int?,
    val originalValue: Int?,
    val quantity: Int?,
    val pickup: Int?,
    val photo: Int?,
) {
    val hasAny: Boolean get() = listOf(title, description, price, originalValue, quantity, pickup, photo).any { it != null }
}

/** H3: the pickup window is today, ends after it starts and is not over yet. */
@StringRes
fun pickupError(start: Int?, end: Int?, nowMinutes: Int): Int? = when {
    start == null || end == null -> R.string.bag_error_pickup_required
    end <= start -> R.string.bag_error_pickup_order
    end <= nowMinutes -> R.string.bag_error_pickup_over
    else -> null
}
