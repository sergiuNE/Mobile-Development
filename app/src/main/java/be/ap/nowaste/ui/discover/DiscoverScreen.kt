package be.ap.nowaste.ui.discover

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.ap.nowaste.R
import be.ap.nowaste.data.store.StoreCategory
import be.ap.nowaste.ui.auth.FormMessage
import be.ap.nowaste.ui.map.PinPickerMap
import be.ap.nowaste.ui.merchant.labelRes

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@Composable
fun DiscoverScreen(viewModel: DiscoverViewModel, modifier: Modifier = Modifier) {
    LocationGate(viewModel, modifier) { state -> BagsNearby(state, viewModel, modifier) }
}

/**
 * Shows [content] once there is a location (K1): before that, it explains and asks for the permission,
 * looks up the location, or lets the user pick one on the map. Used by the Discover and Map tabs.
 */
@Composable
internal fun LocationGate(
    viewModel: DiscoverViewModel,
    modifier: Modifier,
    content: @Composable (DiscoverUiState) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        // Approximate only (COARSE) is also fine for bags nearby
        val granted = result.values.any { it }
        // After a refusal Android stops showing the dialog and no longer wants a rationale: then only the settings help
        val permanentlyDenied = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        viewModel.onPermissionResult(granted, permanentlyDenied)
    }
    val requestPermission = { permissionLauncher.launch(LOCATION_PERMISSIONS) }
    val openSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
        )
    }

    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    when (state.step) {
        LocationStep.ASK_PERMISSION -> AskPermission(onAllow = requestPermission, onChooseOnMap = viewModel::onChooseOnMap, modifier)
        LocationStep.LOCATING -> Locating(modifier)
        LocationStep.PICK_ON_MAP -> PickOnMap(state, viewModel, onUseMyLocation = requestPermission, onOpenSettings = openSettings, modifier)
        LocationStep.READY -> content(state)
    }
}

@Composable
private fun AskPermission(onAllow: () -> Unit, onChooseOnMap: () -> Unit, modifier: Modifier) {
    CenteredColumn(modifier) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.location_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(stringResource(R.string.location_explanation), textAlign = TextAlign.Center)
        Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.location_use_mine)) }
        OutlinedButton(onClick = onChooseOnMap, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.location_choose_on_map))
        }
    }
}

@Composable
private fun Locating(modifier: Modifier) {
    CenteredColumn(modifier) {
        CircularProgressIndicator()
        Text(stringResource(R.string.location_finding), textAlign = TextAlign.Center)
    }
}

@Composable
private fun PickOnMap(
    state: DiscoverUiState,
    viewModel: DiscoverViewModel,
    onUseMyLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.location_pick_title), style = MaterialTheme.typography.headlineSmall)
        state.locationMessage?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.isPermanentlyDenied) {
            OutlinedButton(onClick = onOpenSettings) { Text(stringResource(R.string.location_open_settings)) }
        } else {
            TextButton(onClick = onUseMyLocation) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.location_use_mine), modifier = Modifier.padding(start = 8.dp))
            }
        }
        Text(
            stringResource(R.string.location_pick_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PinPickerMap(
            pin = state.pin,
            onPinPlaced = viewModel::onPinPlaced,
            modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp)),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (state.location != null) {
                OutlinedButton(onClick = viewModel::onCancelPick, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cancel))
                }
            }
            Button(onClick = viewModel::onConfirmPin, enabled = state.pin != null, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.location_confirm))
            }
        }
    }
}

@Composable
private fun BagsNearby(state: DiscoverUiState, viewModel: DiscoverViewModel, modifier: Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when {
                    state.city != null -> stringResource(R.string.discover_bags_near, state.city)
                    else -> stringResource(R.string.discover_bags_near_you)
                },
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            if (state.isLoadingCity) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            TextButton(onClick = viewModel::onChooseOnMap) { Text(stringResource(R.string.discover_change_location)) }
        }
        if (state.isApproximate) {
            Text(
                stringResource(R.string.discover_approximate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.cityError != null) {
            FormMessage(state.cityError, isError = true)
            TextButton(onClick = viewModel::retryCity, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.retry))
            }
        }
        BagFilterChips(state, viewModel)
        if (state.isOffline) OfflineNote()
        BagList(state, onRetry = viewModel::retryBags, modifier = Modifier.fillMaxWidth().weight(1f))
    }
}

/** Filters (K6): max distance and category. Tapping a selected chip turns it off. */
@Composable
internal fun BagFilterChips(state: DiscoverUiState, viewModel: DiscoverViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DISTANCE_FILTERS_KM.forEach { km ->
            FilterChip(
                selected = state.maxDistanceKm == km,
                onClick = { viewModel.onDistanceFilter(km) },
                label = { Text(stringResource(R.string.discover_filter_distance, km)) },
            )
        }
        StoreCategory.entries.forEach { category ->
            FilterChip(
                selected = state.category == category,
                onClick = { viewModel.onCategoryFilter(category) },
                label = { Text(stringResource(category.labelRes())) },
            )
        }
    }
}

@Composable
internal fun OfflineNote(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.discover_offline),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier,
    )
}

@Composable
private fun BagList(state: DiscoverUiState, onRetry: () -> Unit, modifier: Modifier) {
    when {
        state.isLoadingBags -> Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.bagsError != null -> Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(state.bagsError), textAlign = TextAlign.Center)
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
        state.bags.isEmpty() -> Box(modifier, contentAlignment = Alignment.Center) {
            Text(stringResource(state.noBagsMessage()), textAlign = TextAlign.Center)
        }
        else -> LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(state.bags, key = { it.id }) { BagCard(it) }
        }
    }
}

@StringRes
internal fun DiscoverUiState.noBagsMessage(): Int =
    if (hasFilters) R.string.discover_no_bags_filtered else R.string.discover_no_bags

@Composable
internal fun BagCard(bag: BagListItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BagPhoto(bag.photo)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    bag.storeName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(bag.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.discover_pickup_window, bag.pickupStart, bag.pickupEnd),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    pluralStringResource(R.plurals.discover_left_and_distance, bag.quantityLeft, bag.quantityLeft, bag.distanceInKm),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    bag.originalValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.LineThrough,
                )
                Text(bag.price, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun BagPhoto(photo: ImageBitmap?) {
    val photoModifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))
    if (photo != null) {
        Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = photoModifier)
    } else {
        Box(photoModifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CenteredColumn(modifier: Modifier, content: @Composable () -> Unit) {
    // Scrolls so the buttons stay reachable in landscape
    Box(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}
