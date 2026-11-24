package com.teti.iptv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.teti.iptv.data.local.entity.TmdbMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TmdbMetadataDao {
    @Query("SELECT * FROM tmdb_metadata WHERE contentKey = :key LIMIT 1")
    fun observe(key: String): Flow<TmdbMetadataEntity?>

    @Query("SELECT * FROM tmdb_metadata WHERE contentKey = :key LIMIT 1")
    suspend fun get(key: String): TmdbMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TmdbMetadataEntity)

    @Query("DELETE FROM tmdb_metadata")
    suspend fun clearAll()

    @Query("UPDATE vod_movies SET imdbRating = NULL")
    suspend fun clearMovieRatings()

    @Query("UPDATE vod_series SET imdbRating = NULL")
    suspend fun clearSeriesRatings()

    /** Senkron sonrası: film puanlarını yalnızca TMDB önbelleğinden geri yükler. */
    @Query(
        """
        UPDATE vod_movies SET imdbRating = (
            SELECT voteAverage FROM tmdb_metadata
            WHERE contentKey = 'movie:' || vod_movies.streamId AND voteAverage > 0
        )
        WHERE profileId = :profileId
        """
    )
    suspend fun applyMovieRatings(profileId: Long)

    @Query(
        """
        UPDATE vod_series SET imdbRating = (
            SELECT voteAverage FROM tmdb_metadata
            WHERE contentKey = 'tv:' || vod_series.seriesId AND voteAverage > 0
        )
        WHERE profileId = :profileId
        """
    )
    suspend fun applySeriesRatings(profileId: Long)

    /** TMDB'de henüz aranmamış filmler (zenginleştirme kuyruğu). */
    @Query(
        """
        SELECT streamId AS id, title FROM vod_movies
        WHERE profileId = :profileId
          AND NOT EXISTS (SELECT 1 FROM tmdb_metadata m WHERE m.contentKey = 'movie:' || vod_movies.streamId)
        ORDER BY addedTimestamp DESC
        LIMIT :limit
        """
    )
    suspend fun pendingMovies(profileId: Long, limit: Int): List<PendingTitle>

    @Query(
        """
        SELECT seriesId AS id, title FROM vod_series
        WHERE profileId = :profileId
          AND NOT EXISTS (SELECT 1 FROM tmdb_metadata m WHERE m.contentKey = 'tv:' || vod_series.seriesId)
        ORDER BY lastModified DESC
        LIMIT :limit
        """
    )
    suspend fun pendingSeries(profileId: Long, limit: Int): List<PendingTitle>

    @Query(
        """
        SELECT
          (SELECT COUNT(*) FROM vod_movies WHERE profileId = :profileId) +
          (SELECT COUNT(*) FROM vod_series WHERE profileId = :profileId)
        """
    )
    suspend fun catalogSize(profileId: Long): Int
}

@Dao
interface TmdbMatchDao {
    /** TMDB kimlikleri verilen filmlerden katalogda bulunanlar. */
    @Query(
        """
        SELECT vod_movies.* FROM vod_movies
        JOIN tmdb_metadata m ON m.contentKey = 'movie:' || vod_movies.streamId
        WHERE vod_movies.profileId = :profileId AND m.tmdbId IN (:tmdbIds)
        """
    )
    suspend fun moviesByTmdbIds(profileId: Long, tmdbIds: List<Long>): List<com.teti.iptv.data.local.entity.VodEntity>

    @Query(
        """
        SELECT vod_series.* FROM vod_series
        JOIN tmdb_metadata m ON m.contentKey = 'tv:' || vod_series.seriesId
        WHERE vod_series.profileId = :profileId AND m.tmdbId IN (:tmdbIds)
        """
    )
    suspend fun seriesByTmdbIds(profileId: Long, tmdbIds: List<Long>): List<com.teti.iptv.data.local.entity.SeriesEntity>

    @Query("SELECT contentKey, tmdbId FROM tmdb_metadata WHERE contentKey IN (:keys) AND tmdbId IS NOT NULL")
    suspend fun tmdbIdsFor(keys: List<String>): List<KeyTmdbId>
}

data class KeyTmdbId(val contentKey: String, val tmdbId: Long)

data class PendingTitle(val id: String, val title: String)
