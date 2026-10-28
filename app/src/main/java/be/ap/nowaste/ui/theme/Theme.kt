package be.ap.nowaste.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dynamic color is off on purpose: the app always uses the NoWaste palette.
private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE6F2),
    onPrimaryContainer = Navy,
    secondary = Cyan,
    onSecondary = Color.White,
    secondaryContainer = LightGreen,
    onSecondaryContainer = Navy,
    tertiary = Teal,
    onTertiary = Color.White,
    tertiaryContainer = Lime,
    onTertiaryContainer = Navy,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightBackground,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = Blue,
    outline = LightOutline,
    outlineVariant = LightSurfaceVariant,
    surfaceBright = LightBackground,
    surfaceDim = LightSurfaceContainerHighest,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
)

private val DarkColors = darkColorScheme(
    primary = Emerald,
    onPrimary = DarkOnPrimary,
    primaryContainer = DeepBlue,
    onPrimaryContainer = Lime,
    secondary = LightGreen,
    onSecondary = DarkOnPrimary,
    secondaryContainer = Navy,
    onSecondaryContainer = Lime,
    tertiary = Lime,
    onTertiary = DarkOnPrimary,
    tertiaryContainer = Blue,
    onTertiaryContainer = Color.White,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkBackground,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = Emerald,
    outline = DarkOutline,
    outlineVariant = DarkSurfaceVariant,
    surfaceBright = DarkSurfaceContainerHighest,
    surfaceDim = DarkBackground,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
)

@Composable
fun NoWasteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
