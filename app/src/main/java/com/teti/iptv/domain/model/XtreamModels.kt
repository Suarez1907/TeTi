package com.teti.iptv.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XtreamCategory(
    @SerialName("category_id") val categoryId: String = "",
    @SerialName("category_name") val categoryName: String = "",
    @SerialName("parent_id") val parentId: Int = 0
)

@Serializable
data class XtreamEpisode(
    val id: String = "",
    @SerialName("episode_num") val episodeNum: Int = 0,
    val title: String = "",
    @SerialName("container_extension") val containerExtension: String = "mp4",
    val info: XtreamEpisodeInfo? = null
)

@Serializable
data class XtreamEpisodeInfo(
    val plot: String? = null,
    @SerialName("duration_secs") val durationSecs: Long? = null,
    @SerialName("movie_image") val movieImage: String? = null
)

@Serializable
data class XtreamSeriesInfoResponse(
    val seasons: List<XtreamSeason>? = null,
    val episodes: Map<String, List<XtreamEpisode>>? = null
)

@Serializable
data class XtreamSeason(
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("episode_count") val episodeCount: Int? = null,
    val id: Int? = null,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("season_number") val seasonNumber: Int = 1,
    val cover: String? = null
)

@Serializable
data class OmdbResponse(
    @SerialName("Title") val title: String? = null,
    @SerialName("Year") val year: String? = null,
    val imdbRating: String? = null,
    @SerialName("Plot") val plot: String? = null,
    @SerialName("Poster") val poster: String? = null,
    @SerialName("Response") val response: String? = "False"
)

@Serializable
data class TmdbSearchResponse(
    val results: List<TmdbItem>? = null
)

@Serializable
data class TmdbItem(
    val id: Long? = null,
    val title: String? = null,
    val name: String? = null,
    @SerialName("vote_average") val voteAverage: Float? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null
)
