package com.teti.iptv.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.teti.iptv.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE profileId = :profileId AND isHidden = 0 ORDER BY sortOrder ASC, name ASC")
    fun getChannelsPaging(profileId: Long): PagingSource<Int, ChannelEntity>

    @Query("SELECT * FROM channels WHERE profileId = :profileId AND categoryName = :category AND isHidden = 0 ORDER BY sortOrder ASC, name ASC")
    fun getChannelsByCategoryPaging(profileId: Long, category: String): PagingSource<Int, ChannelEntity>

    @Query("SELECT * FROM channels WHERE profileId = :profileId AND isFavorite = 1 AND isHidden = 0 ORDER BY sortOrder ASC, name ASC")
    fun getFavoriteChannelsPaging(profileId: Long): PagingSource<Int, ChannelEntity>

    @Query("SELECT DISTINCT categoryName FROM channels WHERE profileId = :profileId ORDER BY categoryName ASC")
    fun getCategories(profileId: Long): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE profileId = :profileId AND (name LIKE '%' || :query || '%' OR categoryName LIKE '%' || :query || '%') AND isHidden = 0 LIMIT 100")
    fun searchChannels(profileId: Long, query: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE streamId = :streamId LIMIT 1")
    suspend fun getById(streamId: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE streamId = :streamId")
    suspend fun setFavorite(streamId: String, isFavorite: Boolean)

    @Query("UPDATE channels SET isHidden = 1 WHERE streamId = :streamId")
    suspend fun restoreHidden(streamId: String)

    @Query("SELECT streamId FROM channels WHERE profileId = :profileId AND isFavorite = 1")
    suspend fun getFavoriteIds(profileId: Long): List<String>

    @Query("SELECT streamId FROM channels WHERE profileId = :profileId AND isHidden = 1")
    suspend fun getHiddenIds(profileId: Long): List<String>

    @Query("DELETE FROM channels WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)

    @Transaction
    suspend fun replaceByProfile(profileId: Long, channels: List<ChannelEntity>) {
        deleteByProfile(profileId)
        insertAll(channels)
    }

    @Query("SELECT COUNT(*) FROM channels WHERE profileId = :profileId")
    suspend fun countByProfile(profileId: Long): Int
}
