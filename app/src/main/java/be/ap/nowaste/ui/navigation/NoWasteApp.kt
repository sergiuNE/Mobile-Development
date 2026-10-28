package be.ap.nowaste.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import be.ap.nowaste.R
import be.ap.nowaste.ui.discover.DiscoverScreen
import be.ap.nowaste.ui.favorites.FavoritesScreen
import be.ap.nowaste.ui.orders.OrdersScreen

private data class TopLevelDestination(
    val route: Any,
    @StringRes val label: Int,
    val icon: ImageVector,
)

private val customerDestinations = listOf(
    TopLevelDestination(Discover, R.string.nav_discover, Icons.Filled.Search),
    TopLevelDestination(Orders, R.string.nav_orders, Icons.AutoMirrored.Filled.List),
    TopLevelDestination(Favorites, R.string.nav_favorites, Icons.Filled.Favorite),
)

@Composable
fun NoWasteApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                customerDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute(destination.route::class) == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Discover,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<Discover> { DiscoverScreen() }
            composable<Orders> { OrdersScreen() }
            composable<Favorites> { FavoritesScreen() }
        }
    }
}
