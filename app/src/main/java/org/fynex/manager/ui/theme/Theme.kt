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

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    secondary = EmeraldAccent,
    tertiary = PurpleAi,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onPrimary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite
)

private val AmoledColorScheme = darkColorScheme(
    primary = BlueLight,
    secondary = EmeraldAccent,
    tertiary = PurpleAi,
    background = AmoledBackground,
    surface = AmoledSurface,
    surfaceVariant = AmoledCard,
    onPrimary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = EmeraldDark,
    tertiary = PurpleAi,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightCard,
    onPrimary = TextWhite,
    onBackground = TextDark,
    onSurface = TextDark
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
                if (isSystemDark) DarkColorScheme else LightColorScheme
            }
        }
        AppThemeMode.AMOLED -> AmoledColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
