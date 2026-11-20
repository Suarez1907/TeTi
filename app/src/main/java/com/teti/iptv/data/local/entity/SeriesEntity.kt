package com.teti.iptv.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vod_series",
    indices = [
        Index(value = ["profileId", "categoryName"]),
        Index(value = ["imdbRating"]),
        Index(value = ["title"])
    ]
)
data class SeriesEntity(
    @PrimaryKey val seriesId: String,
    val profileId: Long,
    val title: String,
    val categoryName: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val plot: String? = null,
    val imdbRating: Float? = null,
    val releaseYear: Int? = null,
    val lastModified: Long = 0L,
    val isFavorite: Boolean = false
)
