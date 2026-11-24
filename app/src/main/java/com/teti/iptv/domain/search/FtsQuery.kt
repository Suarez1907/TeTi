package com.teti.iptv.domain.search

import java.util.Locale

/** Kullanıcı metnini güvenli bir FTS4 MATCH ifadesine çevirir: her kelime önek araması, hepsi zorunlu. */
object FtsQuery {
    private val TOKEN = Regex("[\\p{L}\\p{N}]+")

    /** @return MATCH ifadesi; anlamlı kelime yoksa null (LIKE aramasına düşülür). */
    fun build(raw: String): String? {
        val tokens = TOKEN.findAll(raw.lowercase(Locale.ROOT))
            .map { it.value }
            .filter { it.isNotEmpty() }
            .take(6)
            .toList()
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "$it*" }
    }
}
