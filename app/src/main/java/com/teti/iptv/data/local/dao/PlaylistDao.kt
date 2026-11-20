package com.teti.iptv.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.teti.iptv.data.local.entity.PlaylistProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlist_profiles ORDER BY id ASC")
    fun getAllProfiles(): Flow<List<PlaylistProfileEntity>>

    @Query("SELECT * FROM playlist_profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfile(): Flow<PlaylistProfileEntity?>

    @Query("SELECT * FROM playlist_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfileOnce(): PlaylistProfileEntity?

    @Query("SELECT * FROM playlist_profiles WHERE id = :profileId LIMIT 1")
    suspend fun getProfileById(profileId: Long): PlaylistProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: PlaylistProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: PlaylistProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: PlaylistProfileEntity)

    @Query("UPDATE playlist_profiles SET isActive = 0")
    suspend fun clearAllActive()

    @Query("UPDATE playlist_profiles SET isActive = 1 WHERE id = :profileId")
    suspend fun setActive(profileId: Long)

    @Transaction
    suspend fun setActiveProfile(profileId: Long) {
        clearAllActive()
        setActive(profileId)
    }

    @Query("UPDATE playlist_profiles SET lastSyncedAt = :timestamp WHERE id = :profileId")
    suspend fun updateLastSynced(profileId: Long, timestamp: Long)
}
