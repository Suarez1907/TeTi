package com.teti.iptv.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

interface XtreamApiService {
    @GET("player_api.php")
    suspend fun getLiveCategories(
        @Query("username") u: String,
        @Query("password") p: String,
        @Query("action") action: String = "get_live_categories"
    ): ResponseBody

    @Streaming
    @GET("player_api.php")
    suspend fun getLiveStreams(
        @Query("username") u: String,
        @Query("password") p: String,
        @Query("action") action: String = "get_live_streams"
    ): ResponseBody

    @GET("player_api.php")
    suspend fun getVodCategories(
        @Query("username") u: String,
        @Query("password") p: String,
        @Query("action") action: String = "get_vod_categories"
    ): ResponseBody

    @Streaming
    @GET("player_api.php")
    suspend fun getVodStreams(
        @Query("username") u: String,
        @Query("password") p: String,
        @Query("action") action: String = "get_vod_streams"
    ): ResponseBody

    @GET("player_api.php")
    suspend fun getSeriesCategories(
        @Query("username") u: String,
        @Query("password") p: String,
        @Query("action") action: String = "get_series_categories"
    ): ResponseBody

    @Streaming
    @GET("player_api.php")
    suspend fun getSeries(
        @Query("username") u: String,
        @Query("password") p: String,
        @Query("action") action: String = "get_series"
    ): ResponseBody

    @GET("player_api.php")
    suspend fun getSeriesInfo(
        @Query("username") u: String,
        @Query("password") p: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: String
    ): ResponseBody
}
