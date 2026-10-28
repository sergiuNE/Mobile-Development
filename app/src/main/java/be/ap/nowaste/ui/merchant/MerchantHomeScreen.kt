package be.ap.nowaste.ui.merchant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import be.ap.nowaste.R
import be.ap.nowaste.ui.navigation.LogoutButton

// Placeholder until H1/H5: becomes "My store" and "My bags".
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantHomeScreen(onLogout: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = { LogoutButton(onLogout) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.merchant_placeholder))
        }
    }
}
