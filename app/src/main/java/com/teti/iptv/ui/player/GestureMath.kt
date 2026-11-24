package com.teti.iptv.ui.player

import kotlin.math.abs

enum class DragAxis { HORIZONTAL, VERTICAL }

/** Oynatıcı hareketlerinin saf hesapları (birim testli). */
object GestureMath {
    /**
     * Yön kilidi: parmak eşiği aşınca baskın eksen seçilir ve hareket bitene kadar
     * değişmez. Çapraz (kararsız) hareketlerde karar ertelenir.
     */
    fun decideAxis(dx: Float, dy: Float, slop: Float, dominance: Float = 1.3f): DragAxis? {
        val ax = abs(dx)
        val ay = abs(dy)
        if (ax < slop && ay < slop) return null
        return when {
            ax >= ay * dominance -> DragAxis.HORIZONTAL
            ay >= ax * dominance -> DragAxis.VERTICAL
            ax + ay > slop * 4 -> if (ax > ay) DragAxis.HORIZONTAL else DragAxis.VERTICAL
            else -> null
        }
    }

    /** Yukarı kaydırma artırır. Ekran yüksekliğinin %80'i tam aralığı (0..1) kapsar. */
    fun adjust(start: Float, totalDy: Float, height: Float): Float {
        if (height <= 0f) return start
        return (start - totalDy / (height * 0.8f)).coerceIn(0f, 1f)
    }

    /** Sola kaydırma çekmeceyi açar: ekranın ortasından başlamalı ve genişliğin %12'sini geçmeli. */
    fun isOpenDrawerSwipe(startX: Float, totalDx: Float, width: Float): Boolean =
        width > 0f && startX in (width * 0.2f)..(width * 0.9f) && totalDx < -width * 0.12f

    fun isCloseDrawerSwipe(totalDx: Float, width: Float): Boolean = width > 0f && totalDx > width * 0.08f
}
