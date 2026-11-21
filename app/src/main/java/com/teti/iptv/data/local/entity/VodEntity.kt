package com.teti.iptv.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "vod_movies",
    indices = [
        Index(value = ["profileId", "categoryName"]),
        Index(value = ["imdbRating"]),
        Index(value = ["addedTimestamp"]),
        Index(value = ["title"])
    ]
)
data class VodEntity(
    @PrimaryKey val streamId: String,
    val profileId: Long,
    val title: String,
    val categoryName: String,
    val streamUrl: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val imdbRating: Float? = null,
    val releaseYear: Int? = null,
    val containerExtension: String? = null,
    val addedTimestamp: Long = 0L,
    val isFavorite: Boolean = false
)
