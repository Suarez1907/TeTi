package com.teti.iptv.data.remote.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class TitleCleanerTest {
    @Test
    fun removesPrefixQualityAndYear() {
        val cleaned = TitleCleaner.clean("[TR] Inception 4K (2010)")
        assertEquals("Inception", cleaned.title)
        assertEquals(2010, cleaned.year)
    }

    @Test
    fun yearAfterQualityTagDoesNotEatTitle() {
        val cleaned = TitleCleaner.clean("TR: FHD Yeşilçam Hikayesi 2019")
        assertEquals("Yeşilçam Hikayesi", cleaned.title)
        assertEquals(2019, cleaned.year)
    }

    @Test
    fun keepsTitleWithoutYear() {
        val cleaned = TitleCleaner.clean("The Office")
        assertEquals("The Office", cleaned.title)
        assertEquals(null, cleaned.year)
    }
}
