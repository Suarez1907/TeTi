package com.teti.iptv.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TeTiAmber,
    onPrimary = TeTiAmberDeep,
    secondary = TeTiSignal,
    onSecondary = TeTiInk,
    tertiary = TeTiGold,
    onTertiary = TeTiInk,
    background = TeTiInk,
    onBackground = TeTiText,
    surface = TeTiSurface,
    onSurface = TeTiText,
    surfaceVariant = TeTiSurfaceHigh,
    onSurfaceVariant = TeTiMuted,
    surfaceContainer = TeTiSurface,
    surfaceContainerLow = TeTiInk,
    surfaceContainerHigh = TeTiSurfaceHigh,
    surfaceContainerHighest = Color(0xFF2C3142),
    outline = Color(0xFF3A3F50)
)

private val TeTiShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun TeTiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = TeTiShapes,
        content = content
    )
}
