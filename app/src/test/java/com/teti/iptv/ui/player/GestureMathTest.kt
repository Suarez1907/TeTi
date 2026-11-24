package com.teti.iptv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureMathTest {
    @Test
    fun axisWaitsForSlop() {
        assertNull(GestureMath.decideAxis(3f, 2f, slop = 8f))
    }

    @Test
    fun axisLocksToDominantDirection() {
        assertEquals(DragAxis.HORIZONTAL, GestureMath.decideAxis(-40f, 6f, slop = 8f))
        assertEquals(DragAxis.VERTICAL, GestureMath.decideAxis(5f, 30f, slop = 8f))
    }

    @Test
    fun diagonalWaitsThenPicksLarger() {
        assertNull(GestureMath.decideAxis(10f, 9f, slop = 8f))
        assertEquals(DragAxis.HORIZONTAL, GestureMath.decideAxis(22f, 19f, slop = 8f))
    }

    @Test
    fun swipeUpIncreasesAndClamps() {
        assertEquals(0.75f, GestureMath.adjust(0.5f, totalDy = -200f, height = 1000f), 0.001f)
        assertEquals(1f, GestureMath.adjust(0.9f, totalDy = -2000f, height = 1000f), 0.001f)
        assertEquals(0f, GestureMath.adjust(0.1f, totalDy = 2000f, height = 1000f), 0.001f)
    }

    @Test
    fun drawerOpensOnlyFromCenterLeftSwipe() {
        assertTrue(GestureMath.isOpenDrawerSwipe(startX = 600f, totalDx = -200f, width = 1000f))
        assertFalse(GestureMath.isOpenDrawerSwipe(startX = 980f, totalDx = -300f, width = 1000f))
        assertFalse(GestureMath.isOpenDrawerSwipe(startX = 600f, totalDx = 200f, width = 1000f))
        assertFalse(GestureMath.isOpenDrawerSwipe(startX = 600f, totalDx = -50f, width = 1000f))
    }
}
