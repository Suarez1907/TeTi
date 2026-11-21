package com.teti.iptv.ui.device

import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

data class DeviceProfile(
    val isTelevision: Boolean,
    val supportsTouch: Boolean,
    val useNavigationRail: Boolean,
    val pagerSwipeEnabled: Boolean,
    val widthDp: Int
)

@Composable
fun rememberDeviceProfile(): DeviceProfile {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val width = configuration.screenWidthDp
    val uiMode = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    return remember(width, uiMode, configuration.screenHeightDp) {
        val television = uiMode == Configuration.UI_MODE_TYPE_TELEVISION ||
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val touch = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) && !television
        DeviceProfile(
            isTelevision = television,
            supportsTouch = touch,
            useNavigationRail = television || width >= 700,
            pagerSwipeEnabled = touch,
            widthDp = width
        )
    }
}
