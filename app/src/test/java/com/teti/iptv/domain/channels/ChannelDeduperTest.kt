package com.teti.iptv.domain.channels

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelDeduperTest {
    @Test
    fun groupsPrefixAndQualityVariants() {
        assertEquals(ChannelDeduper.key("TR: Kanal Yedi HD"), ChannelDeduper.key("[TR] KANAL YEDİ FHD"))
        assertEquals("kanal yedi", ChannelDeduper.key("Kanal Yedi ᴴᴰ"))
    }

    @Test
    fun keepsHighestQualityThenEarliest() {
        val channels = listOf(
            ChannelRef("1", "TR: Spor Bir SD", 0),
            ChannelRef("2", "TR: Spor Bir FHD", 5),
            ChannelRef("3", "Spor Bir HD", 1),
            ChannelRef("4", "Haber Kanalı", 2),
            ChannelRef("5", "Spor Bir+", 3)
        )
        assertEquals(setOf("1", "3"), ChannelDeduper.duplicatesToHide(channels))
    }
}
