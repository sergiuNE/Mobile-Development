package be.ap.nowaste.ui.merchant

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import be.ap.nowaste.R
import be.ap.nowaste.ui.navigation.LogoutButton

// "My store" (H1, H2). "My bags" (H5) is added later as a second tab.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantHomeScreen(onLogout: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.store_my_store)) },
                actions = { LogoutButton(onLogout) },
            )
        },
    ) { innerPadding ->
        MyStoreScreen(modifier = Modifier.padding(innerPadding))
    }
}
