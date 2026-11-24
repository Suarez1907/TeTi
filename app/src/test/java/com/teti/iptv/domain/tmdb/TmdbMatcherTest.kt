package com.teti.iptv.domain.tmdb

import com.teti.iptv.domain.model.TmdbItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbMatcherTest {
    @Test
    fun prefersExactTitleAndYearOverFirstResult() {
        val results = listOf(
            TmdbItem(id = 1, title = "Dune: Part Two", releaseDate = "2024-02-27", voteCount = 5000),
            TmdbItem(id = 2, title = "Dune", releaseDate = "1984-12-14", voteCount = 3000),
            TmdbItem(id = 3, title = "Dune", releaseDate = "2021-09-15", voteCount = 12000)
        )
        assertEquals(3L, TmdbMatcher.pickBest(results, "Dune", 2021)?.id)
    }

    @Test
    fun matchesTurkishTitleIgnoringDiacritics() {
        val results = listOf(
            TmdbItem(id = 7, title = "Kış Uykusu", originalTitle = "Kış Uykusu", releaseDate = "2014-05-16", voteCount = 600)
        )
        assertEquals(7L, TmdbMatcher.pickBest(results, "Kis Uykusu", 2014)?.id)
    }

    @Test
    fun ignoresRatingsWithTooFewVotes() {
        assertNull(TmdbMatcher.trustedRating(10f, 1))
        assertNull(TmdbMatcher.trustedRating(0f, 500))
        assertEquals(7.8f, TmdbMatcher.trustedRating(7.8f, 500))
    }
}
