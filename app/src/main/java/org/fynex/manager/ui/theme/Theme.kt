package org.fynex.manager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.fynex.manager.core.settings.AppThemeMode

// Fylo-style: dynamic color (Material You) whenever possible, warm off-white / near-black fallbacks
private val FyloLightScheme = lightColorScheme(
    primary = FyloLightPrimary,
    onPrimary = FyloLightOnPrimary,
    primaryContainer = AccentPink.copy(alpha = 0.12f),
    onPrimaryContainer = FyloLightOnBg,
    secondary = AccentGreen,
    onSecondary = FyloLightOnPrimary,
    tertiary = AccentBlue,
    onTertiary = FyloLightOnPrimary,
    background = FyloLightBg,
    onBackground = FyloLightOnBg,
    surface = FyloLightSurface,
    onSurface = FyloLightOnSurface,
    surfaceVariant = FyloLightCard,
    onSurfaceVariant = FyloLightOnBg.copy(alpha = 0.7f),
    surfaceContainerHigh = FyloLightCard,
    surfaceContainer = FyloLightCard,
    surfaceContainerLow = FyloLightCard.copy(alpha = 0.5f),
    outline = FyloLightOnBg.copy(alpha = 0.25f)
)

private val FyloDarkScheme = darkColorScheme(
    primary = FyloDarkPrimary,
    onPrimary = FyloDarkOnPrimary,
    primaryContainer = AccentGreen.copy(alpha = 0.18f),
    onPrimaryContainer = FyloDarkOnBg,
    secondary = AccentGreen,
    onSecondary = FyloDarkOnBg,
    tertiary = AccentBlue,
    onTertiary = FyloDarkOnBg,
    background = FyloDarkBg,
    onBackground = FyloDarkOnBg,
    surface = FyloDarkSurface,
    onSurface = FyloDarkOnSurface,
    surfaceVariant = FyloDarkCard,
    onSurfaceVariant = FyloDarkOnBg.copy(alpha = 0.7f),
    surfaceContainerHigh = FyloDarkCard,
    surfaceContainer = FyloDarkCard,
    surfaceContainerLow = FyloDarkCard.copy(alpha = 0.5f),
    outline = FyloDarkOnBg.copy(alpha = 0.25f)
)

private val AmoledColorScheme = darkColorScheme(
    primary = FyloDarkPrimary,
    onPrimary = FyloDarkOnPrimary,
    secondary = AccentGreen,
    onSecondary = FyloDarkOnBg,
    tertiary = AccentBlue,
    onTertiary = FyloDarkOnBg,
    background = AmoledBg,
    onBackground = FyloDarkOnBg,
    surface = AmoledSurface,
    onSurface = FyloDarkOnSurface,
    surfaceVariant = AmoledCard,
    onSurfaceVariant = FyloDarkOnBg.copy(alpha = 0.7f),
    surfaceContainerHigh = AmoledCard,
    surfaceContainer = AmoledCard,
    surfaceContainerLow = AmoledCard.copy(alpha = 0.5f),
    outline = FyloDarkOnBg.copy(alpha = 0.25f)
)

@Composable
fun FynexTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()

    val colorScheme = when (themeMode) {
        AppThemeMode.SYSTEM -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isSystemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isSystemDark) FyloDarkScheme else FyloLightScheme
            }
        }
        AppThemeMode.AMOLED -> AmoledColorScheme
        AppThemeMode.DARK -> FyloDarkScheme
        AppThemeMode.LIGHT -> FyloLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}