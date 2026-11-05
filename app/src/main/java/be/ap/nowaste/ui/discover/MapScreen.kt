package be.ap.nowaste.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import be.ap.nowaste.R
import be.ap.nowaste.ui.map.StoreOnMap
import be.ap.nowaste.ui.map.StoresMap

/** Map tab (K4): your location and a marker per store with available bags, with the same filters as the list. */
@Composable
fun MapScreen(viewModel: DiscoverViewModel, modifier: Modifier = Modifier) {
    LocationGate(viewModel, modifier) { state ->
        Box(modifier = modifier.fillMaxSize()) {
            StoresMap(
                ownLocation = state.location!!,
                stores = state.stores,
                onStoreClick = viewModel::onStoreSelected,
                onMapClick = viewModel::onMapClick,
                modifier = Modifier.fillMaxSize(),
            )

            // Chips need a background, otherwise they are hard to read on top of the map
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    BagFilterChips(state, viewModel)
                    if (state.isOffline) OfflineNote()
                    MapStatus(state, onRetry = viewModel::retryBags)
                }
            }

            state.selectedStore?.let { store ->
                SelectedStoreCard(
                    store = store,
                    onView = viewModel::onViewStore,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                )
            }
        }

        state.viewedStore?.let { store ->
            StoreBagsSheet(store, state.bags.filter { it.storeId == store.id }, onDismiss = viewModel::onCloseStore)
        }
    }
}

/** Loading, error or "no bags" message above the map: the map itself stays usable. */
@Composable
private fun MapStatus(state: DiscoverUiState, onRetry: () -> Unit) {
    when {
        state.isLoadingBags -> Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.map_loading_bags), style = MaterialTheme.typography.bodySmall)
        }
        state.bagsError != null -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(state.bagsError),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
        state.stores.isEmpty() -> Text(
            stringResource(state.noBagsMessage()),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SelectedStoreCard(store: StoreOnMap, onView: () -> Unit, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(store.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    pluralStringResource(R.plurals.map_bags_available, store.bagCount, store.bagCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onView) { Text(stringResource(R.string.map_view_store)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreBagsSheet(store: StoreOnMap, bags: List<BagListItem>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            store.name,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            items(bags, key = { it.id }) { BagCard(it) }
        }
    }
}
