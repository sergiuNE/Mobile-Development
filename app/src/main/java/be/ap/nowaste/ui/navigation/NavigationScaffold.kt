package be.ap.nowaste.ui.navigation

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource

/** A tab of the customer or merchant home. */
data class NavigationTab(
    @StringRes val label: Int,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/**
 * Tabs at the bottom in portrait. In landscape they move to a rail at the side: a bottom bar there
 * takes a third of the height, and the list or map below the filters had almost no room left.
 * Pass no tabs to hide them, for example on a detail screen.
 */
@Composable
fun NavigationScaffold(
    topBar: @Composable () -> Unit,
    tabs: List<NavigationTab>,
    content: @Composable () -> Unit,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Scaffold(
        topBar = topBar,
        bottomBar = {
            if (!isLandscape && tabs.isNotEmpty()) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = tab.selected,
                            onClick = tab.onClick,
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Row(modifier = Modifier.padding(innerPadding)) {
            if (isLandscape && tabs.isNotEmpty()) {
                // The Scaffold padding already keeps it clear of the system bars
                NavigationRail(windowInsets = WindowInsets(0)) {
                    tabs.forEach { tab ->
                        NavigationRailItem(
                            selected = tab.selected,
                            onClick = tab.onClick,
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
            // Clipped: the OSMDroid MapView draws past its own bounds, over the rail and the system bar
            Box(modifier = Modifier.weight(1f).fillMaxSize().clipToBounds()) { content() }
        }
    }
}
