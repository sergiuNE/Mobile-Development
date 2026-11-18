package be.ap.nowaste.ui.merchant.bags

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ap.nowaste.R
import be.ap.nowaste.data.bag.formatMinutes
import be.ap.nowaste.ui.auth.AuthTextField
import be.ap.nowaste.ui.auth.FormMessage
import be.ap.nowaste.ui.auth.LoadingButton
import kotlinx.coroutines.Dispatchers
import java.time.LocalTime

private enum class TimeField { START, END }

/** Offer a new bag (H3, H4) or change one (H7). Calls [onDone] after saving. */
@Composable
fun BagFormScreen(onDone: () -> Unit, modifier: Modifier = Modifier, viewModel: BagFormViewModel = viewModel()) {
    // Main.immediate: text field updates arrive in the same frame, so fast typing loses no characters
    val state by viewModel.uiState.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    when {
        state.isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.loadError != null -> Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(state.loadError!!), textAlign = TextAlign.Center)
            Button(onClick = viewModel::retryLoad) { Text(stringResource(R.string.retry)) }
        }
        else -> BagForm(state, viewModel, modifier)
    }
}

@Composable
private fun BagForm(state: BagFormUiState, viewModel: BagFormViewModel, modifier: Modifier) {
    val enabled = !state.isSaving
    // Which time dialog is open; saveable, so it stays open after a rotation
    var openTimeField by rememberSaveable { mutableStateOf<TimeField?>(null) }

    Box(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (state.isEdit) R.string.bag_form_title_edit else R.string.bag_form_title_new),
                style = MaterialTheme.typography.headlineSmall,
            )

            PhotoPicker(state, enabled, viewModel)

            AuthTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = R.string.bag_title,
                error = state.titleError,
                enabled = enabled,
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.bag_description)) },
                isError = state.descriptionError != null,
                supportingText = state.descriptionError?.let { { Text(stringResource(it)) } },
                enabled = enabled,
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    AuthTextField(
                        value = state.price,
                        onValueChange = viewModel::onPriceChange,
                        label = R.string.bag_price,
                        error = state.priceError,
                        enabled = enabled,
                        keyboardType = KeyboardType.Decimal,
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    AuthTextField(
                        value = state.originalValue,
                        onValueChange = viewModel::onOriginalValueChange,
                        label = R.string.bag_original_value,
                        error = state.originalValueError,
                        enabled = enabled,
                        keyboardType = KeyboardType.Decimal,
                    )
                }
            }
            AuthTextField(
                value = state.quantity,
                onValueChange = viewModel::onQuantityChange,
                label = R.string.bag_quantity,
                error = state.quantityError,
                enabled = enabled,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            )

            Text(stringResource(R.string.bag_pickup), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeButton(R.string.bag_pickup_from, state.pickupStart, enabled, Modifier.weight(1f)) { openTimeField = TimeField.START }
                TimeButton(R.string.bag_pickup_until, state.pickupEnd, enabled, Modifier.weight(1f)) { openTimeField = TimeField.END }
            }
            FormMessage(state.pickupError, isError = true)

            FormMessage(state.errorMessage, isError = true)
            LoadingButton(
                text = if (state.isEdit) R.string.bag_save else R.string.bag_offer,
                // Also busy while a photo is being prepared: it would not be saved yet
                isLoading = state.isSaving || state.isProcessingPhoto,
                onClick = viewModel::save,
            )
        }
    }

    openTimeField?.let { field ->
        TimeDialog(
            initialMinutes = if (field == TimeField.START) state.pickupStart else state.pickupEnd,
            onConfirm = { minutes ->
                if (field == TimeField.START) viewModel.onPickupStartChange(minutes) else viewModel.onPickupEndChange(minutes)
                openTimeField = null
            },
            onDismiss = { openTimeField = null },
        )
    }
}

/** H4: photo with the camera (TakePicturePreview) or from the gallery (PickVisualMedia). No extra permission needed. */
@Composable
private fun PhotoPicker(state: BagFormUiState, enabled: Boolean, viewModel: BagFormViewModel) {
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview(), viewModel::onPhotoTaken)
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia(), viewModel::onPhotoPicked)

    Box(contentAlignment = Alignment.Center) {
        // Fixed height: in landscape a 16:9 photo would fill the whole screen
        BagThumbnail(state.photo, Modifier.fillMaxWidth().height(200.dp))
        if (state.isProcessingPhoto) CircularProgressIndicator()
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = {
                // A phone without a camera app has nothing to open
                try {
                    takePhoto.launch(null)
                } catch (e: ActivityNotFoundException) {
                    viewModel.onNoCamera()
                }
            },
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.bag_take_photo)) }
        OutlinedButton(
            onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.bag_choose_photo)) }
    }
    FormMessage(state.photoError, isError = true)
}

@Composable
private fun TimeButton(@StringRes label: Int, minutes: Int?, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(stringResource(label, minutes?.let(::formatMinutes) ?: stringResource(R.string.bag_pickup_choose)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initialMinutes: Int?, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    // Without a time yet: start at the next full hour
    val start = initialMinutes ?: ((LocalTime.now().hour + 1) % 24 * 60)
    val pickerState = rememberTimePickerState(initialHour = start / 60, initialMinute = start % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour * 60 + pickerState.minute) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
