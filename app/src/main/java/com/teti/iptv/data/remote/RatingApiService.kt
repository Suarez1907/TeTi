package com.teti.iptv.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * TMDB v3 (birincil) ve OMDb (isteğe bağlı yedek). Kimlik her çağrıda ayrı verilir:
 * v3 anahtarı [apiKey] sorgu parametresiyle, v4 okuma jetonu [authorization] başlığıyla.
 * Null parametreler Retrofit tarafından isteğe eklenmez.
 */
interface RatingApiService {
    @GET
    suspend fun getOmdbByTitle(
        @Url url: String = "https://www.omdbapi.com/",
        @Query("apikey") apiKey: String,
        @Query("t") title: String,
        @Query("y") year: Int? = null
    ): ResponseBody

    @GET
    suspend fun tmdbGet(
        @Url url: String,
        @Header("Authorization") authorization: String?,
        @Query("api_key") apiKey: String?,
        @Query("language") language: String = TMDB_LANGUAGE,
        @Query("query") query: String? = null,
        @Query("year") year: Int? = null,
        @Query("first_air_date_year") firstAirYear: Int? = null,
        @Query("append_to_response") append: String? = null,
        @Query("page") page: Int? = null
    ): ResponseBody

    companion object {
        const val TMDB_BASE = "https://api.themoviedb.org/3/"
        const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/"
        const val TMDB_LANGUAGE = "tr-TR"
    }
}
