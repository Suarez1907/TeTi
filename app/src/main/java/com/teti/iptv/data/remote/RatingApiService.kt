package com.teti.iptv.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface RatingApiService {
    @GET
    suspend fun getOmdbByTitle(
        @Url url: String = "https://www.omdbapi.com/",
        @Query("apikey") apiKey: String,
        @Query("t") title: String,
        @Query("y") year: Int? = null
    ): ResponseBody

    @GET
    suspend fun searchTmdbMovie(
        @Url url: String = "https://api.themoviedb.org/3/search/movie",
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("year") year: Int? = null
    ): ResponseBody

    @GET
    suspend fun searchTmdbTv(
        @Url url: String = "https://api.themoviedb.org/3/search/tv",
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("first_air_date_year") year: Int? = null
    ): ResponseBody
}
