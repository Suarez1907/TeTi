package com.teti.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Test

class CastMediaTest {
    @Test
    fun xtreamLiveTsBecomesHls() {
        assertEquals(
            "http://host:8080/live/user/pass/123.m3u8",
            CastMedia.castableUrl("http://host:8080/live/user/pass/123.ts")
        )
    }

    @Test
    fun otherUrlsAreUnchanged() {
        val movie = "http://host/movie/user/pass/55.mkv"
        assertEquals(movie, CastMedia.castableUrl(movie))
        val catchup = "http://host/timeshift/u/p/60/2026-11-24:20-00/12.ts"
        assertEquals(catchup, CastMedia.castableUrl(catchup))
    }
}
