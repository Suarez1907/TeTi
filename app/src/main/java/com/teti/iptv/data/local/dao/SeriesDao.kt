package com.teti.iptv.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.teti.iptv.data.local.entity.SeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesDao {
    @Query("SELECT * FROM vod_series WHERE profileId = :profileId ORDER BY lastModified DESC, title ASC")
    fun getSeriesPaging(profileId: Long): PagingSource<Int, SeriesEntity>

    @Query("SELECT * FROM vod_series WHERE profileId = :profileId AND categoryName = :category ORDER BY lastModified DESC, title ASC")
    fun getSeriesByCategoryPaging(profileId: Long, category: String): PagingSource<Int, SeriesEntity>

    @Query("SELECT * FROM vod_series WHERE profileId = :profileId AND isFavorite = 1 ORDER BY title ASC")
    fun getFavoritesPaging(profileId: Long): PagingSource<Int, SeriesEntity>

    @Query("SELECT * FROM vod_series WHERE profileId = :profileId AND imdbRating IS NOT NULL ORDER BY imdbRating DESC LIMIT :limit")
    fun getTopRated(profileId: Long, limit: Int = 20): Flow<List<SeriesEntity>>

    @Query("SELECT DISTINCT categoryName FROM vod_series WHERE profileId = :profileId ORDER BY categoryName ASC")
    fun getCategories(profileId: Long): Flow<List<String>>

    @Query("SELECT * FROM vod_series WHERE profileId = :profileId AND (title LIKE '%' || :query || '%' OR categoryName LIKE '%' || :query || '%') LIMIT 100")
    fun searchSeries(profileId: Long, query: String): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM vod_series WHERE seriesId = :seriesId LIMIT 1")
    suspend fun getById(seriesId: String): SeriesEntity?

    @Query("SELECT * FROM vod_series WHERE seriesId = :seriesId LIMIT 1")
    fun getByIdFlow(seriesId: String): Flow<SeriesEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(seriesList: List<SeriesEntity>)

    @Query("UPDATE vod_series SET isFavorite = :isFavorite WHERE seriesId = :seriesId")
    suspend fun setFavorite(seriesId: String, isFavorite: Boolean)

    @Query("UPDATE vod_series SET imdbRating = :rating WHERE seriesId = :seriesId")
    suspend fun updateRating(seriesId: String, rating: Float)

    @Query("SELECT seriesId FROM vod_series WHERE profileId = :profileId AND isFavorite = 1")
    suspend fun getFavoriteIds(profileId: Long): List<String>

    @Query("SELECT * FROM vod_series WHERE profileId = :profileId AND imdbRating IS NOT NULL")
    suspend fun getRatedItems(profileId: Long): List<SeriesEntity>

    @Query("DELETE FROM vod_series WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)

    @Transaction
    suspend fun replaceByProfile(profileId: Long, seriesList: List<SeriesEntity>) {
        deleteByProfile(profileId)
        insertAll(seriesList)
    }
}
