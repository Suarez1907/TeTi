package com.teti.iptv.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object LiveTv : Screen("live_tv")
    data object Vod : Screen("vod")
    data object Series : Screen("series")
    data object Search : Screen("search")
    data object Settings : Screen("settings")

    data object VodDetail : Screen("vod_detail/{streamId}") {
        fun createRoute(streamId: String): String = "vod_detail/${urlEncode(streamId)}"
    }

    data object SeriesDetail : Screen("series_detail/{seriesId}") {
        fun createRoute(seriesId: String): String = "series_detail/${urlEncode(seriesId)}"
    }

    data object Player : Screen("player?url={url}&title={title}&contentId={contentId}&isLive={isLive}") {
        fun createRoute(url: String, title: String, contentId: String, isLive: Boolean = false): String {
            return "player?url=${urlEncode(url)}&title=${urlEncode(title)}&contentId=${urlEncode(contentId)}&isLive=$isLive"
        }
    }

    companion object {
        fun urlEncode(value: String): String = Uri.encode(value)
    }
}
