package com.teti.iptv.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["profileId", "categoryName"]),
        Index(value = ["epgChannelId"]),
        Index(value = ["isFavorite"]),
        Index(value = ["name"])
    ]
)
data class ChannelEntity(
    @PrimaryKey val streamId: String,
    val profileId: Long,
    val name: String,
    val categoryName: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val epgChannelId: String? = null,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val sortOrder: Int = 0
)
