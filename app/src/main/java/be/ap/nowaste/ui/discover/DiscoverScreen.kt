package be.ap.nowaste.ui.discover

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import be.ap.nowaste.R

// Placeholder until K3: gets a DiscoverViewModel with a StateFlow<DiscoverUiState>.
@Composable
fun DiscoverScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.discover_placeholder))
    }
}
