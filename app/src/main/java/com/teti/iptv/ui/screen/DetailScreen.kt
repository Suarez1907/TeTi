package com.teti.iptv.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.teti.iptv.domain.model.XtreamEpisode
import com.teti.iptv.ui.components.RatingBadge
import com.teti.iptv.ui.theme.DarkRed
import com.teti.iptv.ui.theme.DarkSurface
import com.teti.iptv.ui.viewmodel.DetailViewModel

@Composable
fun VodDetailScreen(
    streamId: String,
    onBack: () -> Unit,
    onPlay: (url: String, title: String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val vod by viewModel.vod.collectAsState()

    LaunchedEffect(streamId) {
        viewModel.loadVod(streamId)
    }

    vod?.let { movie ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AsyncImage(
                    model = movie.backdropUrl ?: movie.posterUrl,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f), MaterialTheme.colorScheme.background)
                            )
                        )
                )

                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                movie.imdbRating?.let {
                    RatingBadge(rating = it)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${movie.categoryName} • ${movie.releaseYear ?: ""}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onPlay(movie.streamUrl, movie.title) },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Oynat", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun SeriesDetailScreen(
    seriesId: String,
    onBack: () -> Unit,
    onPlayEpisode: (url: String, title: String, contentId: String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val series by viewModel.series.collectAsState()
    val seriesInfo by viewModel.seriesInfo.collectAsState()
    val selectedSeason by viewModel.selectedSeasonNumber.collectAsState()

    LaunchedEffect(seriesId) {
        viewModel.loadSeries(seriesId)
    }

    series?.let { s ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AsyncImage(
                    model = s.backdropUrl ?: s.posterUrl,
                    contentDescription = s.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f), MaterialTheme.colorScheme.background)
                            )
                        )
                )

                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                s.imdbRating?.let {
                    RatingBadge(rating = it)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = s.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))
                s.plot?.let {
                    Text(text = it, color = Color.LightGray, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Sezon Seçici
                val seasons = seriesInfo?.seasons ?: emptyList()
                if (seasons.isNotEmpty()) {
                    Text("Sezonlar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(seasons) { season ->
                            val sNum = season.seasonNumber
                            val isSelected = sNum == selectedSeason
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectSeason(sNum) },
                                label = { Text("Sezon $sNum") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DarkRed,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Bölümler
                val episodes = viewModel.getEpisodesForSeason(selectedSeason)
                Text("Bölümler (${episodes.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                episodes.forEach { ep ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val url = viewModel.getEpisodeStreamUrl(ep) ?: return@clickable
                                onPlayEpisode(
                                    url,
                                    "${s.title} S${selectedSeason}E${ep.episodeNum}",
                                    "${seriesId}_${ep.id}"
                                )
                            },
                        color = DarkSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${ep.episodeNum}.",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.width(36.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ep.title.ifBlank { "Bölüm ${ep.episodeNum}" },
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = "Oynat", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
