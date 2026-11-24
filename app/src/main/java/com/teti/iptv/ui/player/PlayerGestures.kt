package com.teti.iptv.ui.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

/** Sol yarı: parlaklık, sağ yarı: medya sesi. */
enum class HudKind { BRIGHTNESS, VOLUME }

/**
 * Tam ekran oynatıcı kaydırma hareketleri, eksen kilidiyle:
 *  - yatay: sola kaydırma [onSwipeLeft], sağa kaydırma [onSwipeRight]
 *  - dikey: sol yarıda parlaklık, sağ yarıda ses ([onVertical] 0..1 değer verir)
 * Eksen bir kez seçilince hareket bitene kadar değişmez; dokunmalar (tek/çift tık)
 * eşik aşılmadıkça tüketilmez ve tap algılayıcıya kalır.
 */
fun Modifier.playerDragGestures(
    enabled: Boolean,
    readLevel: (HudKind) -> Float,
    onVertical: (HudKind, Float) -> Unit,
    onVerticalEnd: () -> Unit,
    onSwipeLeft: (startX: Float, totalDx: Float, width: Float) -> Unit,
    onSwipeRight: (totalDx: Float, width: Float) -> Unit
): Modifier = pointerInput(enabled) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val width = size.width.toFloat()
        val height = size.height.toFloat()
        val kind = if (down.position.x < width / 2f) HudKind.BRIGHTNESS else HudKind.VOLUME
        val slop = viewConfiguration.touchSlop
        var axis: DragAxis? = null
        var total = Offset.Zero
        var startLevel = 0f

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            // İkinci parmak (yakınlaştırma vb.) bu hareketi iptal eder.
            if (event.changes.count { it.pressed } > 1) {
                axis = null
                break
            }
            total += change.positionChange()
            if (axis == null) {
                axis = GestureMath.decideAxis(total.x, total.y, slop)
                if (axis == DragAxis.VERTICAL) startLevel = readLevel(kind)
            }
            when (axis) {
                DragAxis.VERTICAL -> {
                    change.consume()
                    onVertical(kind, GestureMath.adjust(startLevel, total.y, height))
                }
                DragAxis.HORIZONTAL -> change.consume()
                null -> Unit
            }
        }

        when (axis) {
            DragAxis.HORIZONTAL -> {
                if (total.x < 0) onSwipeLeft(down.position.x, total.x, width) else onSwipeRight(total.x, width)
            }
            DragAxis.VERTICAL -> onVerticalEnd()
            null -> Unit
        }
    }
}

/** Ekran parlaklığı yalnızca oynatıcı penceresinde değişir; çıkışta sistem ayarına döner. */
class BrightnessController(private val activity: Activity?) {
    fun read(): Float {
        val window = activity?.window ?: return 0.5f
        val current = window.attributes.screenBrightness
        if (current >= 0f) return current
        val system = runCatching {
            Settings.System.getInt(window.context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrDefault(128)
        return (system / 255f).coerceIn(0f, 1f)
    }

    fun set(level: Float) {
        val window = activity?.window ?: return
        val attrs = window.attributes
        // Tamamen 0 bazı cihazlarda ekranı kapatır.
        attrs.screenBrightness = level.coerceIn(0.01f, 1f)
        window.attributes = attrs
    }

    fun reset() {
        val window = activity?.window ?: return
        val attrs = window.attributes
        attrs.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = attrs
    }
}

class VolumeController(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

    // Ses adımları kaba (genelde 15); parmağı yumuşak izlemek için kesirli değeri tutarız.
    private var fine: Float? = null

    fun read(): Float = fine ?: (audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max)

    fun set(level: Float) {
        fine = level
        val index = (level * max).roundToInt().coerceIn(0, max)
        if (index != audio.getStreamVolume(AudioManager.STREAM_MUSIC)) {
            runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0) }
        }
    }

    fun endGesture() {
        fine = null
    }
}

@Composable
fun rememberBrightnessController(): BrightnessController {
    val context = LocalContext.current
    val controller = remember(context) { BrightnessController(context as? Activity) }
    val latest = rememberUpdatedState(controller)
    DisposableEffect(Unit) {
        onDispose { latest.value.reset() }
    }
    return controller
}

@Composable
fun rememberVolumeController(): VolumeController {
    val context = LocalContext.current
    return remember(context) { VolumeController(context.applicationContext) }
}
