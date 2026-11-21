package com.teti.iptv.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavArgsTest {
    @Test
    fun playbackRoundTripKeepsReservedCharactersOutOfTheRoute() {
        val url = "http://cdn.example/live/user/a+b c/play.m3u8?token=a&b=c%20#frag"
        val title = "100% Ölçü & Şov / Sezon"
        val contentId = "series/42_1"
        val encoded = NavArgs.encodePlayback(url, title, contentId, true)

        assertFalse(encoded.contains("/"))
        assertFalse(encoded.contains("?"))
        assertFalse(encoded.contains("&"))
        assertFalse(encoded.contains("%"))
        assertFalse(encoded.contains(" "))
        assertFalse(encoded.contains("+"))

        val decoded = NavArgs.decodePlayback(encoded)
        assertNotNull(decoded)
        assertEquals(url, decoded!!.url)
        assertEquals(title, decoded.title)
        assertEquals(contentId, decoded.contentId)
        assertTrue(decoded.isLive)
    }

    @Test
    fun idRoundTripAndRouteHelpers() {
        val id = "12/34 56%&"
        val encoded = NavArgs.encodeId(id)
        assertEquals(id, NavArgs.decodeId(encoded))
        assertEquals("vod_detail/$encoded", Screen.VodDetail.createRoute(id))
        assertEquals("series_detail/$encoded", Screen.SeriesDetail.createRoute(id))
        val route = Screen.Player.createRoute("http://x/a?b=1", "Ad", id, false)
        assertTrue(route.startsWith("player/"))
        assertFalse(route.contains("?"))
        val payload = route.removePrefix("player/")
        val request = NavArgs.decodePlayback(payload)
        assertEquals("http://x/a?b=1", request?.url)
        assertEquals(false, request?.isLive)
    }

    @Test
    fun corruptPayloadDoesNotThrow() {
        assertNull(NavArgs.decodePlayback("@@@"))
    }

    @Test
    fun backFromSecondaryTabReturnsToLive() {
        assertEquals(Screen.TAB_LIVE, Screen.backTab(Screen.TAB_SERIES))
        assertNull(Screen.backTab(Screen.TAB_LIVE))
    }
}
