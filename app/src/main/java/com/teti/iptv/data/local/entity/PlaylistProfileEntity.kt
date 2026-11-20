package com.teti.iptv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PlaylistType { XTREAM, M3U_URL, M3U_LOCAL }

@Entity(tableName = "playlist_profiles")
data class PlaylistProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: PlaylistType,
    val serverUrl: String,
    val username: String? = null,
    val password: String? = null,
    val epgUrl: String? = null,
    val customUserAgent: String = "TeTi-IPTV/1.0",
    val isActive: Boolean = false,
    val lastSyncedAt: Long = 0L
)
