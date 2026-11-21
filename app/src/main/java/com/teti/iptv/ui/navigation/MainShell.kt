package com.teti.iptv.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.data.local.entity.SeriesEntity
import com.teti.iptv.data.local.entity.VodEntity
import com.teti.iptv.ui.device.rememberDeviceProfile
import com.teti.iptv.ui.screen.LiveTvScreen
import com.teti.iptv.ui.screen.SearchScreen
import com.teti.iptv.ui.screen.SeriesScreen
import com.teti.iptv.ui.screen.SettingsScreen
import com.teti.iptv.ui.screen.VodScreen
import com.teti.iptv.ui.theme.TeTiAmber
import com.teti.iptv.ui.theme.TeTiAmberDeep
import com.teti.iptv.ui.theme.TeTiInk
import com.teti.iptv.ui.theme.TeTiMuted
import com.teti.iptv.ui.theme.TeTiSurface
import kotlinx.coroutines.launch

private data class TabSpec(val title: String, val icon: ImageVector)

private val tabs = listOf(
    TabSpec("Canlı", Icons.Default.LiveTv),
    TabSpec("Filmler", Icons.Default.Movie),
    TabSpec("Diziler", Icons.Default.Tv),
    TabSpec("Ara", Icons.Default.Search),
    TabSpec("Ayarlar", Icons.Default.Settings)
)

@Composable
fun MainShell(navController: NavHostController) {
    val device = rememberDeviceProfile()
    val pagerState = rememberPagerState(pageCount = { Screen.TAB_COUNT })
    val scope = rememberCoroutineScope()
    val backTarget = Screen.backTab(pagerState.currentPage)

    BackHandler(enabled = backTarget != null) {
        val target = backTarget ?: return@BackHandler
        scope.launch { pagerState.animateScrollToPage(target) }
    }

    fun openPage(index: Int) {
        scope.launch { pagerState.animateScrollToPage(index) }
    }

    Scaffold(
        containerColor = TeTiInk,
        bottomBar = {
            if (!device.useNavigationRail) {
                NavigationBar(containerColor = TeTiSurface) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = pagerState.currentPage == index
                        NavigationBarItem(
                            selected = selected,
                            onClick = { openPage(index) },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TeTiAmber,
                                selectedTextColor = TeTiAmber,
                                indicatorColor = TeTiAmber.copy(alpha = 0.18f),
                                unselectedIconColor = TeTiMuted,
                                unselectedTextColor = TeTiMuted
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(if (device.isTelevision) 12.dp else 0.dp)
        ) {
            if (device.useNavigationRail) {
                NavigationRail(containerColor = TeTiSurface) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = pagerState.currentPage == index
                        NavigationRailItem(
                            selected = selected,
                            onClick = { openPage(index) },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = TeTiAmberDeep,
                                selectedTextColor = TeTiAmber,
                                indicatorColor = TeTiAmber,
                                unselectedIconColor = TeTiMuted,
                                unselectedTextColor = TeTiMuted
                            )
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                userScrollEnabled = device.pagerSwipeEnabled,
                beyondViewportPageCount = 0
            ) { page ->
                when (page) {
                    Screen.TAB_LIVE -> LiveTvScreen(onPlayChannel = { navController.openPlayer(it) })
                    Screen.TAB_VOD -> VodScreen(
                        onMovieClick = { navController.openVod(it) },
                        onPlayMovie = { navController.openMovie(it) }
                    )
                    Screen.TAB_SERIES -> SeriesScreen(onSeriesClick = { navController.openSeries(it) })
                    Screen.TAB_SEARCH -> SearchScreen(
                        onPlayChannel = { navController.openPlayer(it) },
                        onMovieClick = { navController.openVod(it) },
                        onSeriesClick = { navController.openSeries(it) }
                    )
                    Screen.TAB_SETTINGS -> SettingsScreen()
                }
            }
        }
    }
}

private fun NavHostController.openPlayer(channel: ChannelEntity) {
    navigate(
        Screen.Player.createRoute(
            url = channel.streamUrl,
            title = channel.name,
            contentId = channel.streamId,
            isLive = true
        )
    ) { launchSingleTop = true }
}

private fun NavHostController.openMovie(movie: VodEntity) {
    navigate(
        Screen.Player.createRoute(
            url = movie.streamUrl,
            title = movie.title,
            contentId = movie.streamId,
            isLive = false
        )
    ) { launchSingleTop = true }
}

private fun NavHostController.openVod(movie: VodEntity) {
    navigate(Screen.VodDetail.createRoute(movie.streamId)) { launchSingleTop = true }
}

private fun NavHostController.openSeries(series: SeriesEntity) {
    navigate(Screen.SeriesDetail.createRoute(series.seriesId)) { launchSingleTop = true }
}
