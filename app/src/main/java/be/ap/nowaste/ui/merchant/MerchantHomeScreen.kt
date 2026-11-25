package be.ap.nowaste.ui.merchant

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import be.ap.nowaste.R
import be.ap.nowaste.ui.merchant.bags.BagFormScreen
import be.ap.nowaste.ui.merchant.bags.MyBagsScreen
import be.ap.nowaste.ui.navigation.BagForm
import be.ap.nowaste.ui.navigation.LogoutButton
import be.ap.nowaste.ui.navigation.MyBags
import be.ap.nowaste.ui.navigation.MyStore
import be.ap.nowaste.ui.navigation.NavigationScaffold
import be.ap.nowaste.ui.navigation.NavigationTab

private data class MerchantTab(val route: Any, @StringRes val label: Int, val icon: ImageVector)

private val merchantTabs = listOf(
    MerchantTab(MyBags, R.string.bags_my_bags, Icons.Filled.ShoppingCart),
    MerchantTab(MyStore, R.string.store_my_store, Icons.Filled.Home),
)

/** Merchant home: "My bags" (H3–H7) and "My store" (H1, H2). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantHomeScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val isBagForm = destination?.hasRoute(BagForm::class) == true
    val title = if (destination?.hasRoute(MyStore::class) == true) R.string.store_my_store else R.string.bags_my_bags

    // The form gets the whole screen; back returns to "My bags"
    val tabs = if (isBagForm) emptyList() else merchantTabs.map { tab ->
        NavigationTab(
            label = tab.label,
            icon = tab.icon,
            selected = destination?.hasRoute(tab.route::class) == true,
            onClick = { navController.openTab(tab.route) },
        )
    }

    NavigationScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    if (isBagForm) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = { LogoutButton(onLogout) },
            )
        },
        tabs = tabs,
    ) {
        NavHost(navController = navController, startDestination = MyBags) {
            composable<MyBags> {
                MyBagsScreen(
                    onNewBag = { navController.navigate(BagForm()) },
                    onEditBag = { navController.navigate(BagForm(it)) },
                    onSetUpStore = { navController.openTab(MyStore) },
                )
            }
            composable<MyStore> { MyStoreScreen() }
            composable<BagForm> { BagFormScreen(onDone = { navController.popBackStack() }) }
        }
    }
}

private fun NavHostController.openTab(route: Any) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
