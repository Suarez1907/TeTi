package com.teti.iptv.domain.tmdb

import com.teti.iptv.domain.model.TmdbItem
import java.text.Normalizer
import java.util.Locale

/**
 * TMDB arama sonuçlarından katalog başlığına en uygun olanı seçer.
 * İlk sonucu körü körüne almak yerine başlık benzerliği, yıl ve oy sayısını tartar.
 */
object TmdbMatcher {
    private val NON_ALNUM = Regex("[^\\p{L}\\p{N}]+")
    private val DIACRITICS = Regex("\\p{Mn}+")

    fun normalize(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
            .replace('ı', 'i')
        val stripped = Normalizer.normalize(lower, Normalizer.Form.NFD).replace(DIACRITICS, "")
        return stripped.replace(NON_ALNUM, " ").trim()
    }

    fun pickBest(results: List<TmdbItem>, title: String, year: Int?): TmdbItem? {
        if (results.isEmpty()) return null
        val wanted = normalize(title)
        return results
            .map { it to score(it, wanted, year) }
            .filter { (_, s) -> s > 0 }
            .maxByOrNull { (_, s) -> s }
            ?.first
    }

    private fun score(item: TmdbItem, wanted: String, year: Int?): Int {
        val names = listOfNotNull(item.title, item.name, item.originalTitle, item.originalName)
            .map(::normalize)
        var s = when {
            names.any { it == wanted } -> 100
            names.any { it.isNotEmpty() && (it.startsWith(wanted) || wanted.startsWith(it)) } -> 60
            names.any { it.isNotEmpty() && (it.contains(wanted) || wanted.contains(it)) } -> 40
            else -> 10
        }
        val itemYear = (item.releaseDate ?: item.firstAirDate)?.take(4)?.toIntOrNull()
        if (year != null && itemYear != null) {
            s += when (kotlin.math.abs(year - itemYear)) {
                0 -> 50
                1 -> 25
                else -> -40
            }
        }
        val votes = item.voteCount ?: 0
        s += when {
            votes >= 1000 -> 15
            votes >= 100 -> 10
            votes >= 10 -> 5
            else -> 0
        }
        return s
    }

    /** Az oy alan (ör. 1 kişinin 10 verdiği) puanlar yanıltıcıdır; göstermeyiz. */
    fun trustedRating(voteAverage: Float?, voteCount: Int?): Float? {
        val v = voteAverage ?: return null
        if (v <= 0f) return null
        if (voteCount != null && voteCount < MIN_VOTES) return null
        return v
    }

    const val MIN_VOTES = 5
}
