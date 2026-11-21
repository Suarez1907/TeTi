package com.teti.iptv.domain.catalog

/**
 * Sayfalı katalog sorgusu. Kategori, yıl ve sıralama bağlanmış parametre
 * veya beyaz listedeki ORDER BY ile kurulur; metin birleştirilerek SQL'e yazılmaz.
 */
data class BoundQuery(
    val sql: String,
    val args: List<Any>
)

object CatalogQuery {
    fun vod(
        profileId: Long,
        category: String?,
        year: Int?,
        sort: CatalogSort
    ): BoundQuery = build(
        table = "vod_movies",
        recentColumn = "addedTimestamp",
        profileId = profileId,
        category = category,
        year = year,
        sort = sort
    )

    fun series(
        profileId: Long,
        category: String?,
        year: Int?,
        sort: CatalogSort
    ): BoundQuery = build(
        table = "vod_series",
        recentColumn = "lastModified",
        profileId = profileId,
        category = category,
        year = year,
        sort = sort
    )

    private fun build(
        table: String,
        recentColumn: String,
        profileId: Long,
        category: String?,
        year: Int?,
        sort: CatalogSort
    ): BoundQuery {
        val sql = StringBuilder("SELECT * FROM ")
        sql.append(table)
        sql.append(" WHERE profileId = ?")
        val args = mutableListOf<Any>(profileId)

        val normalized = category?.trim().orEmpty()
        when {
            normalized.isEmpty() || normalized == CatalogSort.CATEGORY_ALL -> Unit
            normalized == CatalogSort.CATEGORY_FAVORITES -> sql.append(" AND isFavorite = 1")
            else -> {
                sql.append(" AND categoryName = ?")
                args.add(normalized)
            }
        }

        if (year != null) {
            sql.append(" AND releaseYear = ?")
            args.add(year)
        }

        sql.append(" ORDER BY ")
        sql.append(sort.orderBy(recentColumn))
        return BoundQuery(sql.toString(), args)
    }
}
