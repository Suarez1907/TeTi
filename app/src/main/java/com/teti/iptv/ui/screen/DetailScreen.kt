package com.teti.iptv.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.teti.iptv.ui.components.RatingBadge
import com.teti.iptv.ui.components.tetiClickable
import com.teti.iptv.ui.navigation.NavArgs
import com.teti.iptv.ui.theme.TeTiAmber
import com.teti.iptv.ui.theme.TeTiAmberDeep
import com.teti.iptv.ui.theme.TeTiMuted
import com.teti.iptv.ui.theme.TeTiSurface
import com.teti.iptv.ui.theme.TeTiText
import com.teti.iptv.ui.viewmodel.DetailViewModel

@Composable
fun VodDetailScreen(
    streamId: String,
    onBack: () -> Unit,
    onPlay: (url: String, title: String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val decodedId = NavArgs.decodeId(streamId)
    val vod by viewModel.vod.collectAsState()
    val status by viewModel.vodStatus.collectAsState()

    LaunchedEffect(decodedId) {
        viewModel.loadVod(decodedId)
    }

    val movie = vod
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (movie != null) {
                Button(
                    onClick = { onPlay(movie.streamUrl, movie.title) },
                    colors = ButtonDefaults.buttonColors(containerColor = TeTiAmber, contentColor = TeTiAmberDeep),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Oynat", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            DetailHeader(
                imageUrl = movie?.backdropUrl ?: movie?.posterUrl,
                title = movie?.title ?: "Film",
                onBack = onBack
            )
            when {
                status == DetailViewModel.DetailStatus.Loading -> {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TeTiAmber)
                    }
                }
                movie == null -> {
                    Text(
                        "Film bulunamadı.",
                        color = TeTiMuted,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    Column(modifier = Modifier.padding(16.dp)) {
                        movie.imdbRating?.let {
                            RatingBadge(rating = it)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(movie.title, color = TeTiText, style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = listOfNotNull(
                                movie.categoryName.takeIf { it.isNotBlank() },
                                movie.releaseYear?.toString()
                            ).joinToString(" · "),
                            color = TeTiMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
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
    val decodedId = NavArgs.decodeId(seriesId)
    val series by viewModel.series.collectAsState()
    val status by viewModel.seriesStatus.collectAsState()
    val seriesInfo by viewModel.seriesInfo.collectAsState()
    val selectedSeason by viewModel.selectedSeasonNumber.collectAsState()

    LaunchedEffect(decodedId) {
        viewModel.loadSeries(decodedId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
    ) {
        val show = series
        DetailHeader(
            imageUrl = show?.backdropUrl ?: show?.posterUrl,
            title = show?.title ?: "Dizi",
            onBack = onBack
        )
        when {
            status == DetailViewModel.DetailStatus.Loading -> {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TeTiAmber)
                }
            }
            show == null -> {
                Text(
                    "Dizi bulunamadı.",
                    color = TeTiMuted,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            else -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    show.imdbRating?.let {
                        RatingBadge(rating = it)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(show.title, color = TeTiText, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    show.plot?.let {
                        Text(it, color = TeTiMuted, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    val seasons = seriesInfo?.seasons ?: emptyList()
                    if (seasons.isNotEmpty()) {
                        Text("Sezonlar", color = TeTiText, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(seasons, key = { it.seasonNumber }) { season ->
                                val sNum = season.seasonNumber
                                FilterChip(
                                    selected = sNum == selectedSeason,
                                    onClick = { viewModel.selectSeason(sNum) },
                                    label = { Text("Sezon $sNum") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TeTiAmber,
                                        selectedLabelColor = TeTiAmberDeep,
                                        labelColor = TeTiText
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    val episodes = viewModel.getEpisodesForSeason(selectedSeason)
                    Text(
                        "Bölümler (${episodes.size})",
                        color = TeTiText,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    episodes.forEach { ep ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .tetiClickable {
                                    val url = viewModel.getEpisodeStreamUrl(ep) ?: return@tetiClickable
                                    onPlayEpisode(
                                        url,
                                        "${show.title} S${selectedSeason}E${ep.episodeNum}",
                                        "${decodedId}_${ep.id}"
                                    )
                                },
                            color = TeTiSurface,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${ep.episodeNum}",
                                    color = TeTiAmber,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.width(40.dp)
                                )
                                Text(
                                    text = ep.title.ifBlank { "Bölüm ${ep.episodeNum}" },
                                    color = TeTiText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.PlayArrow, contentDescription = "Oynat", tint = TeTiText)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(imageUrl: String?, title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize().background(TeTiSurface))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, MaterialTheme.colorScheme.background)
                    )
                )
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
        }
    }
}
