package be.ap.nowaste.ui.orders

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ap.nowaste.R
import be.ap.nowaste.data.reservation.ReservationStatus
import be.ap.nowaste.ui.auth.FormMessage

/** K10: active and past reservations, read from Room only. K11: cancel until the pickup window starts. */
@Composable
fun OrdersScreen(modifier: Modifier = Modifier, viewModel: OrdersViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        state.isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.active.isEmpty() && state.past.isEmpty() -> Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FormMessage(state.syncNotice, isError = true)
            Text(stringResource(R.string.orders_empty), textAlign = TextAlign.Center)
        }
        else -> OrderList(state, viewModel, modifier)
    }

    // K11: kept in the ViewModel, so the dialog is still open after a rotation
    val confirmId = state.confirmCancelId
    if (confirmId != null) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissCancel,
            title = { Text(stringResource(R.string.orders_cancel_title)) },
            text = { Text(stringResource(R.string.orders_cancel_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmCancel) {
                    Text(stringResource(R.string.orders_cancel_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissCancel) { Text(stringResource(R.string.orders_cancel_keep)) }
            },
        )
    }
}

@Composable
private fun OrderList(state: OrdersUiState, viewModel: OrdersViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormMessage(state.syncNotice, isError = true)
                FormMessage(state.errorMessage, isError = true)
            }
        }
        if (state.active.isNotEmpty()) {
            item { Text(stringResource(R.string.orders_active), style = MaterialTheme.typography.titleMedium) }
            items(state.active, key = { it.id }) { OrderCard(it, state.cancellingId, viewModel::onCancelClick) }
        }
        if (state.past.isNotEmpty()) {
            item { Text(stringResource(R.string.orders_past), style = MaterialTheme.typography.titleMedium) }
            items(state.past, key = { it.id }) { OrderCard(it, state.cancellingId, viewModel::onCancelClick) }
        }
    }
}

@Composable
private fun OrderCard(order: OrderItem, cancellingId: String?, onCancelClick: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(order.storeName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(order.status.label()), style = MaterialTheme.typography.labelLarge, color = order.status.color())
            }
            Text(order.bagTitle, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.orders_pickup_window, order.pickupDate, order.pickupStart, order.pickupEnd))
            Text(
                pluralStringResource(R.plurals.orders_quantity, order.quantity, order.quantity),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (order.isActive) {
                Text(stringResource(R.string.reserved_code_label), style = MaterialTheme.typography.labelMedium)
                Text(
                    order.pickupCode,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = 4.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (order.canCancel) {
                Button(
                    onClick = { onCancelClick(order.id) },
                    enabled = cancellingId == null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    if (cancellingId == order.id) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.orders_cancel_button))
                    }
                }
            }
        }
    }
}

private fun ReservationStatus.label(): Int = when (this) {
    ReservationStatus.RESERVED -> R.string.orders_status_reserved
    ReservationStatus.COLLECTED -> R.string.orders_status_collected
    ReservationStatus.CANCELLED -> R.string.orders_status_cancelled
}

@Composable
private fun ReservationStatus.color() =
    if (this == ReservationStatus.CANCELLED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
