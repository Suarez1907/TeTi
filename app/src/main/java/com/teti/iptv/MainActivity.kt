package com.teti.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.teti.iptv.ui.navigation.Screen
import com.teti.iptv.ui.screen.LiveTvScreen
import com.teti.iptv.ui.screen.PlayerScreen
import com.teti.iptv.ui.screen.SearchScreen
import com.teti.iptv.ui.screen.SeriesDetailScreen
import com.teti.iptv.ui.screen.SeriesScreen
import com.teti.iptv.ui.screen.SettingsScreen
import com.teti.iptv.ui.screen.VodDetailScreen
import com.teti.iptv.ui.screen.VodScreen
import com.teti.iptv.ui.theme.DarkRed
import com.teti.iptv.ui.theme.DarkSurface
import com.teti.iptv.ui.theme.TeTiTheme
import dagger.hilt.android.AndroidEntryPoint

sealed class BottomNavItem(val screen: Screen, val title: String, val icon: ImageVector) {
    data object LiveTv : BottomNavItem(Screen.LiveTv, "Canlı TV", Icons.Default.LiveTv)
    data object Vod : BottomNavItem(Screen.Vod, "Filmler", Icons.Default.Movie)
    data object Series : BottomNavItem(Screen.Series, "Diziler", Icons.Default.Tv)
    data object Search : BottomNavItem(Screen.Search, "Arama", Icons.Default.Search)
    data object Settings : BottomNavItem(Screen.Settings, "Ayarlar", Icons.Default.Settings)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TeTiTheme {
                MainContent()
            }
        }
    }
}

@Composable
fun MainContent() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomItems = listOf(
        BottomNavItem.LiveTv,
        BottomNavItem.Vod,
        BottomNavItem.Series,
        BottomNavItem.Search,
        BottomNavItem.Settings
    )

    val isPlayerScreen = currentRoute?.startsWith("player") == true

    Scaffold(
        bottomBar = {
            if (!isPlayerScreen) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = Color.White
                ) {
                    bottomItems.forEach { item ->
                        val isSelected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DarkRed,
                                selectedTextColor = DarkRed,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.LiveTv.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isPlayerScreen) androidx.compose.foundation.layout.PaddingValues() else innerPadding)
        ) {
            composable(Screen.LiveTv.route) {
                LiveTvScreen(
                    onPlayChannel = { channel ->
                        navController.navigate(
                            Screen.Player.createRoute(
                                url = channel.streamUrl,
                                title = channel.name,
                                contentId = channel.streamId,
                                isLive = true
                            )
                        )
                    }
                )
            }

            composable(Screen.Vod.route) {
                VodScreen(
                    onMovieClick = { movie ->
                        navController.navigate(Screen.VodDetail.createRoute(movie.streamId))
                    },
                    onPlayMovie = { movie ->
                        navController.navigate(
                            Screen.Player.createRoute(
                                url = movie.streamUrl,
                                title = movie.title,
                                contentId = movie.streamId,
                                isLive = false
                            )
                        )
                    }
                )
            }

            composable(Screen.Series.route) {
                SeriesScreen(
                    onSeriesClick = { series ->
                        navController.navigate(Screen.SeriesDetail.createRoute(series.seriesId))
                    }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onPlayChannel = { ch ->
                        navController.navigate(
                            Screen.Player.createRoute(ch.streamUrl, ch.name, ch.streamId, isLive = true)
                        )
                    },
                    onMovieClick = { movie ->
                        navController.navigate(Screen.VodDetail.createRoute(movie.streamId))
                    },
                    onSeriesClick = { series ->
                        navController.navigate(Screen.SeriesDetail.createRoute(series.seriesId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = Screen.VodDetail.route,
                arguments = listOf(navArgument("streamId") { type = NavType.StringType })
            ) { backStackEntry ->
                val streamId = Screen.urlDecode(backStackEntry.arguments?.getString("streamId") ?: "")
                VodDetailScreen(
                    streamId = streamId,
                    onBack = { navController.popBackStack() },
                    onPlay = { url, title ->
                        navController.navigate(Screen.Player.createRoute(url, title, streamId, isLive = false))
                    }
                )
            }

            composable(
                route = Screen.SeriesDetail.route,
                arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
            ) { backStackEntry ->
                val seriesId = Screen.urlDecode(backStackEntry.arguments?.getString("seriesId") ?: "")
                SeriesDetailScreen(
                    seriesId = seriesId,
                    onBack = { navController.popBackStack() },
                    onPlayEpisode = { url, title ->
                        navController.navigate(Screen.Player.createRoute(url, title, seriesId, isLive = false))
                    }
                )
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType; defaultValue = "" },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    navArgument("contentId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("isLive") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val url = Screen.urlDecode(backStackEntry.arguments?.getString("url") ?: "")
                val title = Screen.urlDecode(backStackEntry.arguments?.getString("title") ?: "")
                val contentId = Screen.urlDecode(backStackEntry.arguments?.getString("contentId") ?: "")
                val isLive = backStackEntry.arguments?.getBoolean("isLive") ?: false

                PlayerScreen(
                    streamUrl = url,
                    title = title,
                    contentId = contentId,
                    isLive = isLive,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
