package be.ap.nowaste.ui.bagdetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ap.nowaste.R
import be.ap.nowaste.ui.auth.FormMessage
import be.ap.nowaste.ui.map.StoreLocationMap

/** K7: everything about one bag. K8: choose a quantity and reserve. K9: then the pickup code. */
@Composable
fun BagDetailScreen(onDone: () -> Unit, viewModel: BagDetailViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val bag = state.bag

    when {
        state.pickupCode != null && bag != null -> Reserved(state.pickupCode!!, bag, state.reservedQuantity, onDone)
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        bag == null -> LoadError(state.loadError ?: R.string.error_unknown, onRetry = viewModel::retryLoad)
        else -> Details(state, bag, viewModel)
    }
}

@Composable
private fun Details(state: BagDetailUiState, bag: BagDetailItem, viewModel: BagDetailViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Photo(state.photo)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(bag.storeName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(bag.title, style = MaterialTheme.typography.headlineSmall)
                }
                // K12: always usable, also offline or when the store has no address
                IconButton(onClick = viewModel::onToggleFavorite) {
                    Icon(
                        if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(if (state.isFavorite) R.string.favorite_remove else R.string.favorite_add),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(bag.price, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    bag.originalValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.LineThrough,
                )
            }
            Text(stringResource(R.string.discover_pickup_window, bag.pickupStart, bag.pickupEnd), style = MaterialTheme.typography.bodyLarge)
            Text(
                if (bag.isAvailable) {
                    pluralStringResource(R.plurals.bag_detail_left, bag.quantityLeft, bag.quantityLeft)
                } else {
                    stringResource(R.string.bag_detail_unavailable)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (bag.isAvailable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
            Text(bag.description, style = MaterialTheme.typography.bodyMedium)
            state.address?.let { address ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(address, style = MaterialTheme.typography.bodyMedium)
                }
            }
            StoreLocationMap(
                location = bag.storeLocation,
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)),
            )
        }
        ReserveBar(state, bag, viewModel)
    }
}

/** Stays at the bottom, also when the details scroll. */
@Composable
private fun ReserveBar(state: BagDetailUiState, bag: BagDetailItem, viewModel: BagDetailViewModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.errorMessage?.let { FormMessage(it, isError = true) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // material-icons-core has no minus icon: a minus sign as text, with a readable description
            val fewer = stringResource(R.string.bag_detail_fewer)
            FilledTonalIconButton(
                onClick = viewModel::onDecrease,
                enabled = bag.isAvailable && !state.isReserving && state.quantity > 1,
                modifier = Modifier.semantics { contentDescription = fewer },
            ) {
                Text(stringResource(R.string.minus_sign), style = MaterialTheme.typography.titleLarge)
            }
            Text(
                state.quantity.toString(),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 24.dp),
            )
            FilledTonalIconButton(
                onClick = viewModel::onIncrease,
                enabled = bag.isAvailable && !state.isReserving && state.quantity < bag.quantityLeft,
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.bag_detail_more))
            }
            Button(
                onClick = viewModel::reserve,
                enabled = bag.isAvailable && !state.isReserving,
                modifier = Modifier.weight(1f),
            ) {
                if (state.isReserving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.bag_detail_reserve))
                }
            }
        }
    }
}

@Composable
private fun Photo(photo: ImageBitmap?) {
    val photoModifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))
    if (photo != null) {
        Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = photoModifier)
    } else {
        Box(photoModifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** K9: the code the customer shows in the store. */
@Composable
private fun Reserved(pickupCode: String, bag: BagDetailItem, quantity: Int, onDone: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.reserved_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                pluralStringResource(R.plurals.reserved_summary, quantity, quantity, bag.title, bag.storeName),
                textAlign = TextAlign.Center,
            )
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.reserved_code_label), style = MaterialTheme.typography.labelLarge)
                    Text(
                        pickupCode,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        letterSpacing = 6.sp,
                    )
                }
            }
            Text(
                stringResource(R.string.reserved_instructions, bag.pickupStart, bag.pickupEnd),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.reserved_done)) }
        }
    }
}

@Composable
private fun LoadError(message: Int, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(message), textAlign = TextAlign.Center)
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
