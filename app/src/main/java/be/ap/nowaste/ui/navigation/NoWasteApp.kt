package be.ap.nowaste.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import be.ap.nowaste.R
import be.ap.nowaste.data.auth.UserRole
import be.ap.nowaste.ui.auth.LoginScreen
import be.ap.nowaste.ui.auth.RegisterScreen
import be.ap.nowaste.ui.discover.DiscoverScreen
import be.ap.nowaste.ui.favorites.FavoritesScreen
import be.ap.nowaste.ui.merchant.MerchantHomeScreen
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

private fun UserRole.homeRoute(): Any = when (this) {
    UserRole.CUSTOMER -> CustomerHome
    UserRole.MERCHANT -> MerchantHome
}

@Composable
fun NoWasteApp(appViewModel: AppViewModel = viewModel()) {
    val state by appViewModel.uiState.collectAsStateWithLifecycle()

    when (val current = state) {
        AppUiState.Loading -> LoadingScreen()
        is AppUiState.Error -> ErrorScreen(current.message, onRetry = appViewModel::loadSession)
        AppUiState.LoggedOut -> AppNavHost(startDestination = Login, onLogout = appViewModel::logout)
        is AppUiState.LoggedIn -> AppNavHost(startDestination = current.role.homeRoute(), onLogout = appViewModel::logout)
    }
}

@Composable
private fun AppNavHost(startDestination: Any, onLogout: () -> Unit) {
    val navController = rememberNavController()

    fun goHome(role: UserRole) = navController.navigate(role.homeRoute()) {
        // Login and register must not stay on the back stack
        popUpTo(navController.graph.id) { inclusive = true }
    }

    fun logout() {
        onLogout()
        navController.navigate(Login) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<Login> {
            LoginScreen(
                onLoggedIn = ::goHome,
                onRegisterClick = { navController.navigate(Register) { launchSingleTop = true } },
            )
        }
        composable<Register> {
            RegisterScreen(
                onRegistered = ::goHome,
                onLoginClick = { navController.popBackStack() },
            )
        }
        composable<CustomerHome> { CustomerHome(onLogout = ::logout) }
        composable<MerchantHome> { MerchantHomeScreen(onLogout = ::logout) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerHome(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = { LogoutButton(onLogout) },
            )
        },
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

/** Red, like every destructive action (see UI/UI.md). */
@Composable
fun LogoutButton(onLogout: () -> Unit) {
    IconButton(onClick = onLogout) {
        Icon(
            Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = stringResource(R.string.logout),
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorScreen(@StringRes message: Int, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(message), textAlign = TextAlign.Center)
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
