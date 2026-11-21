package com.teti.iptv.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogQueryTest {
    @Test
    fun recentMoviesOrderByAddedTimestamp() {
        val query = CatalogQuery.vod(4L, null, null, CatalogSort.RECENTLY_ADDED)
        assertTrue(query.sql.contains("FROM vod_movies"))
        assertTrue(query.sql.contains("ORDER BY addedTimestamp DESC"))
        assertEquals(listOf<Any>(4L), query.args)
    }

    @Test
    fun seriesNameSortUsesLastModifiedColumnOnlyForRecent() {
        val query = CatalogQuery.series(2L, CatalogSort.CATEGORY_ALL, null, CatalogSort.NAME_DESC)
        assertTrue(query.sql.contains("FROM vod_series"))
        assertTrue(query.sql.contains("ORDER BY title COLLATE NOCASE DESC"))
        assertFalse(query.sql.contains("lastModified"))
        assertEquals(listOf<Any>(2L), query.args)
    }

    @Test
    fun categoryYearAndFavoritesAreBoundNotInterpolated() {
        val filtered = CatalogQuery.vod(7L, "Aksiyon & Dram", 2024, CatalogSort.RATING)
        assertTrue(filtered.sql.contains("categoryName = ?"))
        assertTrue(filtered.sql.contains("releaseYear = ?"))
        assertTrue(filtered.sql.contains("(imdbRating IS NULL) ASC"))
        assertFalse(filtered.sql.contains("Aksiyon"))
        assertEquals(listOf<Any>(7L, "Aksiyon & Dram", 2024), filtered.args)

        val favorites = CatalogQuery.series(7L, CatalogSort.CATEGORY_FAVORITES, null, CatalogSort.YEAR_ASC)
        assertTrue(favorites.sql.contains("isFavorite = 1"))
        assertFalse(favorites.sql.contains("categoryName"))
        assertTrue(favorites.sql.contains("releaseYear ASC"))
        assertEquals(listOf<Any>(7L), favorites.args)
    }

    @Test
    fun unknownSortKeyFallsBackToRecent() {
        assertEquals(CatalogSort.RECENTLY_ADDED, CatalogSort.fromStorage(null))
        assertEquals(CatalogSort.RECENTLY_ADDED, CatalogSort.fromStorage("nope"))
        assertEquals(CatalogSort.NAME_ASC, CatalogSort.fromStorage("name_asc"))
    }
}
