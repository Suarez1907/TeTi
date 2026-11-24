package com.teti.iptv.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import com.teti.iptv.data.local.entity.VodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VodDao {
    @Query("SELECT * FROM vod_movies WHERE profileId = :profileId ORDER BY addedTimestamp DESC, title ASC")
    fun getVodPaging(profileId: Long): PagingSource<Int, VodEntity>

    @Query("SELECT * FROM vod_movies WHERE profileId = :profileId AND categoryName = :category ORDER BY addedTimestamp DESC, title ASC")
    fun getVodByCategoryPaging(profileId: Long, category: String): PagingSource<Int, VodEntity>

    @Query("SELECT * FROM vod_movies WHERE profileId = :profileId AND isFavorite = 1 ORDER BY title ASC")
    fun getFavoritesPaging(profileId: Long): PagingSource<Int, VodEntity>

    @Query("SELECT * FROM vod_movies WHERE profileId = :profileId AND imdbRating IS NOT NULL ORDER BY imdbRating DESC LIMIT :limit")
    fun getTopRated(profileId: Long, limit: Int = 20): Flow<List<VodEntity>>

    @Query("SELECT * FROM vod_movies WHERE profileId = :profileId ORDER BY addedTimestamp DESC LIMIT :limit")
    fun getRecentlyAdded(profileId: Long, limit: Int = 20): Flow<List<VodEntity>>

    @Query("SELECT DISTINCT categoryName FROM vod_movies WHERE profileId = :profileId ORDER BY categoryName ASC")
    fun getCategories(profileId: Long): Flow<List<String>>

    @Query("SELECT DISTINCT releaseYear FROM vod_movies WHERE profileId = :profileId AND releaseYear IS NOT NULL ORDER BY releaseYear DESC")
    fun getReleaseYears(profileId: Long): Flow<List<Int>>

    @RawQuery(observedEntities = [VodEntity::class])
    fun getVodPagingRaw(query: SupportSQLiteQuery): PagingSource<Int, VodEntity>

    @Query(
        """
        SELECT vod_movies.* FROM vod_movies JOIN vod_fts ON vod_movies.rowid = vod_fts.rowid
        WHERE vod_fts MATCH :match AND vod_movies.profileId = :profileId
        LIMIT 100
        """
    )
    fun searchVodFts(profileId: Long, match: String): Flow<List<VodEntity>>

    @Query("SELECT * FROM vod_movies WHERE profileId = :profileId AND (title LIKE '%' || :query || '%' OR categoryName LIKE '%' || :query || '%') LIMIT 100")
    fun searchVod(profileId: Long, query: String): Flow<List<VodEntity>>

    @Query("SELECT * FROM vod_movies WHERE streamId = :streamId LIMIT 1")
    suspend fun getById(streamId: String): VodEntity?

    @Query("SELECT * FROM vod_movies WHERE streamId = :streamId LIMIT 1")
    fun getByIdFlow(streamId: String): Flow<VodEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vodList: List<VodEntity>)

    @Query("UPDATE vod_movies SET isFavorite = :isFavorite WHERE streamId = :streamId")
    suspend fun setFavorite(streamId: String, isFavorite: Boolean)

    @Query("UPDATE vod_movies SET imdbRating = :rating WHERE streamId = :streamId")
    suspend fun updateRating(streamId: String, rating: Float?)

    @Query("SELECT streamId FROM vod_movies WHERE profileId = :profileId AND isFavorite = 1")
    suspend fun getFavoriteIds(profileId: Long): List<String>

    @Query("SELECT * FROM vod_movies WHERE profileId = :profileId AND imdbRating IS NOT NULL")
    suspend fun getRatedItems(profileId: Long): List<VodEntity>

    @Query("DELETE FROM vod_movies WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)

    @Transaction
    suspend fun replaceByProfile(profileId: Long, vodList: List<VodEntity>) {
        deleteByProfile(profileId)
        insertAll(vodList)
    }
}
