package be.ap.nowaste.ui.merchant.bags

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ap.nowaste.R
import kotlinx.coroutines.Dispatchers

/** "My bags" (H5): own bags with their reservations in realtime, pickup code check (H6), edit and offline (H7). */
@Composable
fun MyBagsScreen(
    onNewBag: () -> Unit,
    onEditBag: (String) -> Unit,
    onSetUpStore: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyBagsViewModel = viewModel(),
) {
    // Main.immediate: text field updates arrive in the same frame, so fast typing loses no characters
    val state by viewModel.uiState.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    // Back from "My store": the store may exist now
    LaunchedEffect(Unit) {
        if (!state.hasStore) viewModel.load()
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(resources.getString(it))
            viewModel.clearMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            state.loadError != null -> CenteredMessage(state.loadError!!, R.string.retry, viewModel::load)
            !state.hasStore -> CenteredMessage(R.string.bags_no_store, R.string.bags_go_to_store, onSetUpStore)
            else -> BagsList(state, viewModel, onEditBag)
        }

        if (!state.isLoading && state.loadError == null && state.hasStore) {
            ExtendedFloatingActionButton(
                onClick = onNewBag,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.bags_new)) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
    }

    if (state.isCodeDialogOpen) PickupCodeDialog(state, viewModel)
}

@Composable
private fun CenteredMessage(@StringRes message: Int, @StringRes action: Int, onAction: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(message), textAlign = TextAlign.Center)
        Button(onClick = onAction) { Text(stringResource(action)) }
    }
}

@Composable
private fun BagsList(state: MyBagsUiState, viewModel: MyBagsViewModel, onEditBag: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        // Room at the bottom, so the button never covers the last bag
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
    ) {
        item {
            OutlinedButton(onClick = viewModel::openCodeDialog, modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.bags_check_code), modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (state.bags.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.bags_empty),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 32.dp),
                )
            }
        }
        items(state.bags, key = { it.id }) { bag ->
            MerchantBagCard(
                bag = bag,
                isUpdating = bag.id in state.updatingBagIds,
                onActiveChange = { viewModel.setActive(bag.id, it) },
                onEdit = { onEditBag(bag.id) },
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MerchantBagCard(
    bag: MerchantBagItem,
    isUpdating: Boolean,
    onActiveChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BagThumbnail(bag.photo, Modifier.size(80.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(bag.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(bag.price, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Text(
                            bag.originalValue,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        stringResource(R.string.discover_pickup_window, bag.pickupStart, bag.pickupEnd),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    // H5: how many are left and how many customers reserved
                    Text(
                        pluralStringResource(R.plurals.bags_left_and_reserved, bag.reserved, bag.quantityLeft, bag.quantityTotal, bag.reserved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    BagStatus(bag)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.bags_online), style = MaterialTheme.typography.labelLarge)
                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.padding(start = 12.dp).size(20.dp), strokeWidth = 2.dp)
                } else {
                    Switch(checked = bag.active, onCheckedChange = onActiveChange, modifier = Modifier.padding(start = 8.dp))
                }
                Box(modifier = Modifier.weight(1f))
                TextButton(onClick = onEdit) { Text(stringResource(R.string.bags_edit)) }
            }
        }
    }
}

/** Why customers do not see this bag, if they do not. */
@Composable
private fun BagStatus(bag: MerchantBagItem) {
    val status = when {
        !bag.active -> R.string.bags_status_offline
        bag.isPickupOver -> R.string.bags_status_ended
        bag.quantityLeft == 0 -> R.string.bags_status_sold_out
        else -> return
    }
    Text(
        stringResource(status),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .padding(top = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
internal fun BagThumbnail(photo: ImageBitmap?, modifier: Modifier = Modifier) {
    val shaped = modifier.clip(RoundedCornerShape(12.dp))
    if (photo != null) {
        Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = shaped)
    } else {
        Box(shaped.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** H6: the customer shows the pickup code, the merchant checks it and marks the reservation as collected. */
@Composable
private fun PickupCodeDialog(state: MyBagsUiState, viewModel: MyBagsViewModel) {
    val found = state.foundReservation
    AlertDialog(
        onDismissRequest = viewModel::closeCodeDialog,
        title = { Text(stringResource(R.string.bags_check_code)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.bags_code_hint), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = state.code,
                    onValueChange = viewModel::onCodeChange,
                    label = { Text(stringResource(R.string.bags_code_label)) },
                    isError = state.codeError != null,
                    supportingText = state.codeError?.let { { Text(stringResource(it)) } },
                    singleLine = true,
                    enabled = !state.isMarkingCollected,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (found != null) {
                    Text(
                        pluralStringResource(R.plurals.bags_code_found, found.quantity, found.bagTitle, found.quantity),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            when {
                state.isMarkingCollected -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                found != null -> Button(onClick = viewModel::markCollected) { Text(stringResource(R.string.bags_mark_collected)) }
                else -> Button(onClick = viewModel::checkCode, enabled = state.code.isNotBlank()) {
                    Text(stringResource(R.string.bags_code_check))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::closeCodeDialog) { Text(stringResource(R.string.cancel)) }
        },
    )
}
