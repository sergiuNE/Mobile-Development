package be.ap.nowaste.ui.merchant

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ap.nowaste.R
import be.ap.nowaste.data.store.StoreCategory
import be.ap.nowaste.ui.auth.AuthTextField
import be.ap.nowaste.ui.auth.FormMessage
import be.ap.nowaste.ui.auth.LoadingButton
import be.ap.nowaste.ui.map.PinPickerMap
import kotlinx.coroutines.Dispatchers

@Composable
fun MyStoreScreen(modifier: Modifier = Modifier, viewModel: MyStoreViewModel = viewModel()) {
    // Main.immediate: text field updates arrive in the same frame, so fast typing loses no characters
    val state by viewModel.uiState.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)

    when {
        state.isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.loadError != null -> Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(state.loadError!!), textAlign = TextAlign.Center)
            Button(onClick = viewModel::loadStore) { Text(stringResource(R.string.retry)) }
        }
        else -> StoreForm(state, viewModel, modifier)
    }
}

@Composable
private fun StoreForm(state: MyStoreUiState, viewModel: MyStoreViewModel, modifier: Modifier) {
    val enabled = !state.isSaving
    Box(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (state.hasStore) R.string.store_title_edit else R.string.store_title_new),
                style = MaterialTheme.typography.headlineSmall,
            )

            AuthTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = R.string.store_name,
                error = state.nameError,
                enabled = enabled,
            )

            SectionLabel(R.string.store_category)
            CategoryPicker(state.category, enabled, viewModel::onCategoryChange)
            FormMessage(state.categoryError, isError = true)

            SectionLabel(R.string.store_location)
            Text(
                stringResource(R.string.store_location_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box {
                PinPickerMap(
                    pin = state.location,
                    onPinPlaced = viewModel::onLocationPicked,
                    modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(12.dp)),
                )
                if (state.isGeocoding) GeocodingBadge(Modifier.align(Alignment.TopCenter).padding(8.dp))
            }
            FormMessage(state.locationError, isError = true)

            SectionLabel(R.string.store_address)
            Text(
                stringResource(R.string.store_address_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AuthTextField(
                value = state.street,
                onValueChange = viewModel::onStreetChange,
                label = R.string.store_street,
                error = null,
                enabled = enabled,
            )
            AuthTextField(
                value = state.postcode,
                onValueChange = viewModel::onPostcodeChange,
                label = R.string.store_postcode,
                error = null,
                enabled = enabled,
                keyboardType = KeyboardType.Number,
            )
            AuthTextField(
                value = state.city,
                onValueChange = viewModel::onCityChange,
                label = R.string.store_city,
                error = null,
                enabled = enabled,
                imeAction = ImeAction.Done,
            )
            FormMessage(state.addressError, isError = true)

            FormMessage(state.errorMessage, isError = true)
            FormMessage(state.infoMessage, isError = false)

            LoadingButton(
                text = if (state.hasStore) R.string.store_save else R.string.store_create,
                // Also busy while the address of a new pin is looked up
                isLoading = state.isSaving || state.isGeocoding,
                onClick = viewModel::save,
            )
        }
    }
}

@Composable
private fun SectionLabel(@StringRes text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun CategoryPicker(selected: StoreCategory?, enabled: Boolean, onSelect: (StoreCategory) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StoreCategory.entries.forEach { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                enabled = enabled,
                label = { Text(stringResource(category.labelRes())) },
            )
        }
    }
}

@StringRes
fun StoreCategory.labelRes(): Int = when (this) {
    StoreCategory.BAKERY -> R.string.category_bakery
    StoreCategory.SUPERMARKET -> R.string.category_supermarket
    StoreCategory.RESTAURANT -> R.string.category_restaurant
    StoreCategory.CAFE -> R.string.category_cafe
    StoreCategory.OTHER -> R.string.category_other
}

@Composable
private fun GeocodingBadge(modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(50), tonalElevation = 3.dp, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.store_geocoding), style = MaterialTheme.typography.labelLarge)
        }
    }
}
