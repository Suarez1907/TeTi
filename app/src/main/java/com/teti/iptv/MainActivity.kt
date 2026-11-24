package com.teti.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.teti.iptv.ui.navigation.MainShell
import com.teti.iptv.ui.navigation.NavArgs
import com.teti.iptv.ui.navigation.Screen
import com.teti.iptv.ui.screen.GuideScreen
import com.teti.iptv.ui.screen.MultiViewScreen
import com.teti.iptv.ui.screen.PlayerScreen
import com.teti.iptv.ui.screen.SeriesDetailScreen
import com.teti.iptv.ui.screen.VodDetailScreen
import com.teti.iptv.ui.theme.TeTiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TeTiTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Screen.Main.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Main.route) {
                        MainShell(navController)
                    }

                    composable(
                        route = Screen.VodDetail.route,
                        arguments = listOf(navArgument("streamId") { type = NavType.StringType })
                    ) { entry ->
                        val streamId = entry.arguments?.getString("streamId").orEmpty()
                        VodDetailScreen(
                            streamId = streamId,
                            onBack = { navController.popBackStack() },
                            onPlay = { url, title ->
                                val id = NavArgs.decodeId(streamId)
                                navController.navigate(
                                    Screen.Player.createRoute(url, title, id, isLive = false)
                                ) { launchSingleTop = true }
                            },
                            onOpenMovie = { id -> navController.navigate(Screen.VodDetail.createRoute(id)) }
                        )
                    }

                    composable(
                        route = Screen.SeriesDetail.route,
                        arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
                    ) { entry ->
                        val seriesId = entry.arguments?.getString("seriesId").orEmpty()
                        SeriesDetailScreen(
                            seriesId = seriesId,
                            onBack = { navController.popBackStack() },
                            onPlayEpisode = { url, title, contentId ->
                                navController.navigate(
                                    Screen.Player.createRoute(url, title, contentId, isLive = false)
                                ) { launchSingleTop = true }
                            },
                            onOpenSeries = { id -> navController.navigate(Screen.SeriesDetail.createRoute(id)) }
                        )
                    }

                    composable(Screen.Guide.route) {
                        GuideScreen(
                            onBack = { navController.popBackStack() },
                            onPlayLive = { channel ->
                                navController.navigate(
                                    Screen.Player.createRoute(channel.streamUrl, channel.name, channel.streamId, isLive = true)
                                ) { launchSingleTop = true }
                            },
                            onPlayCatchup = { url, title, contentId ->
                                navController.navigate(
                                    Screen.Player.createRoute(url, title, contentId, isLive = false)
                                ) { launchSingleTop = true }
                            }
                        )
                    }

                    composable(Screen.MultiView.route) {
                        MultiViewScreen(onBack = { navController.popBackStack() })
                    }

                    composable(
                        route = Screen.Player.route,
                        arguments = listOf(navArgument("payload") { type = NavType.StringType })
                    ) { entry ->
                        val request = NavArgs.decodePlayback(entry.arguments?.getString("payload").orEmpty())
                        PlayerScreen(
                            streamUrl = request?.url.orEmpty(),
                            title = request?.title ?: "Oynatıcı",
                            contentId = request?.contentId.orEmpty(),
                            isLive = request?.isLive == true,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
