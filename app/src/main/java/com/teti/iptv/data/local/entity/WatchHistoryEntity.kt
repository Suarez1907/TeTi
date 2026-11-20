package com.teti.iptv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val contentId: String,
    val profileId: Long,
    val contentType: String, // "MOVIE" veya "EPISODE"
    val title: String,
    val posterUrl: String?,
    val positionMs: Long,
    val durationMs: Long,
    val lastWatchedAt: Long
)
