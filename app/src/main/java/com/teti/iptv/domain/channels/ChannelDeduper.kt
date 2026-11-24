package com.teti.iptv.domain.channels

import java.text.Normalizer
import java.util.Locale

data class ChannelRef(val id: String, val name: String, val sortOrder: Int)

/**
 * Aynı kanalın kalite/ülke öneki farklı kopyalarını bulur ("TR: Kanal HD", "Kanal FHD",
 * "[TR] KANAL"). Her grupta en yüksek kaliteli olan kalır, eşitlikte listede önce gelen.
 */
object ChannelDeduper {
    private val PREFIX = Regex("^(\\[[^\\]]{1,6}\\]|\\([^)]{1,6}\\)|[A-Za-z]{2,4}\\s*[:|\\-–—])\\s*")
    private val QUALITY = Regex("\\b(4k|uhd|fhd|full\\s*hd|hd|sd|hevc|h\\.?265|h\\.?264|1080p?|720p?|2160p?|raw|backup|yedek)\\b")
    private val SUPERSCRIPT = Regex("[ᴴᴰˢᴰᵁᴴᴰ⁴ᴷ]+")
    private val NON_ALNUM = Regex("[^\\p{L}\\p{N}+]+")
    private val DIACRITICS = Regex("\\p{Mn}+")

    fun key(name: String): String {
        var t = name.trim().replace(PREFIX, "")
        t = t.replace(SUPERSCRIPT, " ").lowercase(Locale.ROOT).replace('ı', 'i')
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replace(DIACRITICS, "")
        t = t.replace(QUALITY, " ")
        return t.replace(NON_ALNUM, " ").trim()
    }

    fun qualityRank(name: String): Int {
        val n = name.lowercase(Locale.ROOT)
        return when {
            Regex("\\b(4k|uhd|2160p?)\\b").containsMatchIn(n) || n.contains("⁴ᴷ") -> 4
            Regex("\\b(fhd|full\\s*hd|1080p?)\\b").containsMatchIn(n) -> 3
            Regex("\\b(hd|720p?)\\b").containsMatchIn(n) || n.contains("ᴴᴰ") -> 2
            Regex("\\bsd\\b").containsMatchIn(n) -> 0
            else -> 1
        }
    }

    /** @return gizlenmesi gereken kopyaların kimlikleri. */
    fun duplicatesToHide(channels: List<ChannelRef>): Set<String> =
        channels
            .groupBy { key(it.name) }
            .filterKeys { it.isNotBlank() }
            .values
            .filter { it.size > 1 }
            .flatMap { group ->
                val keep = group.sortedWith(compareByDescending<ChannelRef> { qualityRank(it.name) }.thenBy { it.sortOrder }).first()
                group.filter { it.id != keep.id }.map { it.id }
            }
            .toSet()
}
