package com.teti.iptv.domain.catalog

/**
 * Sıralama seçenekleri. SQL tarafı yalnızca bu enum'dan üretilir;
 * kullanıcı metni sorguya yazılmaz.
 */
enum class CatalogSort(val storageKey: String) {
    RECENTLY_ADDED("recent"),
    NAME_ASC("name_asc"),
    NAME_DESC("name_desc"),
    RATING("rating"),
    YEAR_DESC("year_desc"),
    YEAR_ASC("year_asc");

    fun label(): String = when (this) {
        RECENTLY_ADDED -> "Son eklenenler"
        NAME_ASC -> "İsim (A–Z)"
        NAME_DESC -> "İsim (Z–A)"
        RATING -> "Puana göre"
        YEAR_DESC -> "Yıl (yeni)"
        YEAR_ASC -> "Yıl (eski)"
    }

    /**
     * @param recentColumn Filmlerde addedTimestamp, dizilerde lastModified.
     * NULL puan ve yıl her zaman listenin sonuna gider.
     */
    fun orderBy(recentColumn: String): String = when (this) {
        RECENTLY_ADDED -> "$recentColumn DESC, title COLLATE NOCASE ASC"
        NAME_ASC -> "title COLLATE NOCASE ASC"
        NAME_DESC -> "title COLLATE NOCASE DESC"
        RATING -> "(imdbRating IS NULL) ASC, imdbRating DESC, title COLLATE NOCASE ASC"
        YEAR_DESC -> "(releaseYear IS NULL) ASC, releaseYear DESC, title COLLATE NOCASE ASC"
        YEAR_ASC -> "(releaseYear IS NULL) ASC, releaseYear ASC, title COLLATE NOCASE ASC"
    }

    companion object {
        const val CATEGORY_ALL = "Tümü"
        const val CATEGORY_FAVORITES = "Favoriler"

        fun fromStorage(raw: String?): CatalogSort {
            return entries.firstOrNull { it.storageKey == raw } ?: RECENTLY_ADDED
        }
    }
}
