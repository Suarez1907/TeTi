package com.teti.iptv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.teti.iptv.data.local.entity.EpgProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpgDao {
    @Query("SELECT * FROM epg_programs WHERE epgChannelId = :epgChannelId AND startTimeMs <= :nowMs AND endTimeMs >= :nowMs LIMIT 1")
    fun getCurrentProgram(epgChannelId: String, nowMs: Long): Flow<EpgProgramEntity?>

    @Query("SELECT * FROM epg_programs WHERE epgChannelId = :epgChannelId AND startTimeMs <= :nowMs AND endTimeMs >= :nowMs LIMIT 1")
    suspend fun getCurrentProgramOnce(epgChannelId: String, nowMs: Long): EpgProgramEntity?

    @Query("SELECT * FROM epg_programs WHERE epgChannelId = :epgChannelId AND endTimeMs >= :fromMs AND startTimeMs <= :toMs ORDER BY startTimeMs ASC")
    fun getProgramsForChannel(epgChannelId: String, fromMs: Long, toMs: Long): Flow<List<EpgProgramEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<EpgProgramEntity>)

    @Query("DELETE FROM epg_programs WHERE endTimeMs < :beforeMs")
    suspend fun deleteOldPrograms(beforeMs: Long)

    @Query("DELETE FROM epg_programs")
    suspend fun clearAll()
}
