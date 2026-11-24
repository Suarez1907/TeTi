package com.teti.iptv.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watch_history",
    indices = [Index(value = ["profileId", "lastWatchedAt"])]
)
data class WatchHistoryEntity(
    @PrimaryKey val contentId: String,
    val profileId: Long,
    val contentType: String, // MOVIE veya EPISODE
    val title: String,
    val posterUrl: String?,
    val positionMs: Long,
    val durationMs: Long,
    val lastWatchedAt: Long,
    /** "Devam et" rafından doğrudan oynatmak için. */
    val streamUrl: String? = null
) {
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    companion object {
        const val TYPE_MOVIE = "MOVIE"
        const val TYPE_EPISODE = "EPISODE"
    }
}
