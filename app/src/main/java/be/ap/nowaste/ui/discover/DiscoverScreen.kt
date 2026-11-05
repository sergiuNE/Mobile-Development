package be.ap.nowaste.ui.discover

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ap.nowaste.R
import be.ap.nowaste.ui.auth.FormMessage
import be.ap.nowaste.ui.map.PinPickerMap

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@Composable
fun DiscoverScreen(modifier: Modifier = Modifier, viewModel: DiscoverViewModel = viewModel()) {
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
        LocationStep.READY -> BagsNearby(state, viewModel, modifier)
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
        // The list of bags comes here with K3
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.discover_placeholder), textAlign = TextAlign.Center)
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
