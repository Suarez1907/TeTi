package com.teti.iptv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TMDB'den çekilen meta veri önbelleği. Katalog senkronizasyonunda film/dizi satırları
 * silinip yeniden yazıldığı için ayrı tabloda tutulur ve senkron sonrası geri uygulanır.
 *
 * [contentKey] "movie:<streamId>" veya "tv:<seriesId>" biçimindedir.
 * [tmdbId] null ise TMDB'de eşleşme bulunamadı demektir (tekrar aranmaz).
 */
@Entity(tableName = "tmdb_metadata")
data class TmdbMetadataEntity(
    @PrimaryKey val contentKey: String,
    val tmdbId: Long? = null,
    val voteAverage: Float? = null,
    val overview: String? = null,
    val backdropUrl: String? = null,
    val posterUrl: String? = null,
    val releaseDate: String? = null,
    val runtimeMinutes: Int? = null,
    val genres: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val detailsLoaded: Boolean = false,
    val fetchedAt: Long = 0L
) {
    val releaseYear: Int? get() = releaseDate?.take(4)?.toIntOrNull()

    companion object {
        fun movieKey(streamId: String) = "movie:$streamId"
        fun tvKey(seriesId: String) = "tv:$seriesId"
    }
}
