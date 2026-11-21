package com.teti.iptv.ui.navigation

sealed class Screen(val route: String) {
    data object Main : Screen("main")

    data object VodDetail : Screen("vod_detail/{streamId}") {
        fun createRoute(streamId: String): String = "vod_detail/${NavArgs.encodeId(streamId)}"
    }

    data object SeriesDetail : Screen("series_detail/{seriesId}") {
        fun createRoute(seriesId: String): String = "series_detail/${NavArgs.encodeId(seriesId)}"
    }

    data object Player : Screen("player/{payload}") {
        fun createRoute(url: String, title: String, contentId: String, isLive: Boolean = false): String {
            return "player/${NavArgs.encodePlayback(url, title, contentId, isLive)}"
        }
    }

    companion object {
        const val TAB_LIVE = 0
        const val TAB_VOD = 1
        const val TAB_SERIES = 2
        const val TAB_SEARCH = 3
        const val TAB_SETTINGS = 4
        const val TAB_COUNT = 5

        /** Kök sekmede geri, uygulamadan çıkmadan önce ilk sekmeye döner. */
        fun backTab(current: Int): Int? = if (current > TAB_LIVE) TAB_LIVE else null
    }
}
