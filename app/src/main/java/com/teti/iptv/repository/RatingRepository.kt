package com.teti.iptv.repository

import com.teti.iptv.data.local.AppPreferences
import com.teti.iptv.data.local.dao.SeriesDao
import com.teti.iptv.data.local.dao.VodDao
import com.teti.iptv.data.remote.RatingApiService
import com.teti.iptv.data.remote.parser.TitleCleaner
import com.teti.iptv.domain.model.OmdbResponse
import com.teti.iptv.domain.model.TmdbSearchResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RatingRepository @Inject constructor(
    private val ratingApiService: RatingApiService,
    private val preferences: AppPreferences,
    private val vodDao: VodDao,
    private val seriesDao: SeriesDao,
    private val json: Json
) {
    suspend fun enrichVodRating(streamId: String, rawTitle: String) = withContext(Dispatchers.IO) {
        val cleaned = TitleCleaner.clean(rawTitle)
        val omdbKey = preferences.omdbApiKey.value
        val tmdbKey = preferences.tmdbApiKey.value

        var rating: Float? = null

        if (omdbKey.isNotBlank()) {
            rating = fetchOmdbRating(omdbKey, cleaned.title, cleaned.year)
        }

        if (rating == null && tmdbKey.isNotBlank()) {
            rating = fetchTmdbMovieRating(tmdbKey, cleaned.title, cleaned.year)
        }

        if (rating != null && rating > 0f) {
            vodDao.updateRating(streamId, rating)
        }
    }

    suspend fun enrichSeriesRating(seriesId: String, rawTitle: String) = withContext(Dispatchers.IO) {
        val cleaned = TitleCleaner.clean(rawTitle)
        val omdbKey = preferences.omdbApiKey.value
        val tmdbKey = preferences.tmdbApiKey.value

        var rating: Float? = null

        if (omdbKey.isNotBlank()) {
            rating = fetchOmdbRating(omdbKey, cleaned.title, cleaned.year)
        }

        if (rating == null && tmdbKey.isNotBlank()) {
            rating = fetchTmdbTvRating(tmdbKey, cleaned.title, cleaned.year)
        }

        if (rating != null && rating > 0f) {
            seriesDao.updateRating(seriesId, rating)
        }
    }

    private suspend fun fetchOmdbRating(apiKey: String, title: String, year: Int?): Float? {
        return runCatching {
            val response = ratingApiService.getOmdbByTitle(apiKey = apiKey, title = title, year = year)
            val model = json.decodeFromString<OmdbResponse>(response.string())
            if (model.Response.equals("True", ignoreCase = true)) {
                model.imdbRating?.toFloatOrNull()
            } else null
        }.getOrNull()
    }

    private suspend fun fetchTmdbMovieRating(apiKey: String, title: String, year: Int?): Float? {
        return runCatching {
            val response = ratingApiService.searchTmdbMovie(apiKey = apiKey, query = title, year = year)
            val model = json.decodeFromString<TmdbSearchResponse>(response.string())
            model.results?.firstOrNull()?.voteAverage
        }.getOrNull()
    }

    private suspend fun fetchTmdbTvRating(apiKey: String, title: String, year: Int?): Float? {
        return runCatching {
            val response = ratingApiService.searchTmdbTv(apiKey = apiKey, query = title, year = year)
            val model = json.decodeFromString<TmdbSearchResponse>(response.string())
            model.results?.firstOrNull()?.voteAverage
        }.getOrNull()
    }
}
