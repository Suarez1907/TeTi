package com.teti.iptv.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TeTiWhite,
    onPrimary = TeTiOnWhite,
    primaryContainer = TeTiGlassHigh,
    onPrimaryContainer = TeTiText,
    secondary = TeTiRed,
    onSecondary = TeTiWhite,
    tertiary = TeTiGold,
    onTertiary = TeTiOnWhite,
    background = TeTiBlack,
    onBackground = TeTiText,
    surface = TeTiBlack,
    onSurface = TeTiText,
    surfaceVariant = TeTiGlass,
    onSurfaceVariant = TeTiMuted,
    surfaceContainerLowest = TeTiBlack,
    surfaceContainerLow = TeTiBlack,
    surfaceContainer = TeTiGlass,
    surfaceContainerHigh = TeTiGlass,
    surfaceContainerHighest = TeTiGlassHigh,
    outline = TeTiBorder,
    outlineVariant = TeTiBorder,
    error = TeTiRed
)

private val TeTiShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TeTiTheme(content: @Composable () -> Unit) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    // Android 15+ kenardan kenara çizer; bu renkler yalnızca eski sürümlerde gerekir.
                    @Suppress("DEPRECATION")
                    window.statusBarColor = colorScheme.background.toArgb()
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = colorScheme.background.toArgb()
                }
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
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
