package com.teti.iptv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.teti.iptv.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history WHERE profileId = :profileId ORDER BY lastWatchedAt DESC LIMIT :limit")
    fun getRecentHistory(profileId: Long, limit: Int = 20): Flow<List<WatchHistoryEntity>>

    @Query("SELECT positionMs FROM watch_history WHERE contentId = :contentId LIMIT 1")
    suspend fun getPosition(contentId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE contentId = :contentId")
    suspend fun delete(contentId: String)
}
