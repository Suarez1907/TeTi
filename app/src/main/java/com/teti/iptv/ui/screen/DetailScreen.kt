package com.teti.iptv.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.teti.iptv.data.local.entity.TmdbMetadataEntity
import com.teti.iptv.domain.model.XtreamEpisode
import com.teti.iptv.ui.components.GlassIconButton
import com.teti.iptv.ui.components.GlassSurface
import com.teti.iptv.ui.components.PillChip
import com.teti.iptv.ui.components.PrimaryActionButton
import com.teti.iptv.ui.components.formatRating
import com.teti.iptv.ui.components.tetiClickable
import com.teti.iptv.ui.navigation.NavArgs
import com.teti.iptv.ui.theme.TeTiBlack
import com.teti.iptv.ui.theme.TeTiGlass
import com.teti.iptv.ui.theme.TeTiGold
import com.teti.iptv.ui.theme.TeTiMuted
import com.teti.iptv.ui.theme.TeTiText
import com.teti.iptv.ui.theme.TeTiWhite
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
    val meta by viewModel.vodMeta.collectAsState()
    val status by viewModel.vodStatus.collectAsState()
    val metaLoading by viewModel.metaLoading.collectAsState()

    LaunchedEffect(decodedId) { viewModel.loadVod(decodedId) }

    val movie = vod
    DetailScaffold(
        backdropUrl = meta?.backdropUrl ?: movie?.backdropUrl ?: meta?.posterUrl ?: movie?.posterUrl,
        onBack = onBack
    ) {
        when {
            movie == null && status == DetailViewModel.DetailStatus.Loading -> LoadingBlock()
            movie == null -> Text("Film bulunamadı.", color = TeTiMuted, style = MaterialTheme.typography.bodyLarge)
            else -> {
                TitleBlock(
                    title = movie.title,
                    year = meta?.releaseYear ?: movie.releaseYear,
                    runtimeMinutes = meta?.runtimeMinutes,
                    rating = meta?.voteAverage ?: movie.imdbRating,
                    genres = meta?.genres ?: movie.categoryName.takeIf { it.isNotBlank() }
                )
                Spacer(Modifier.height(16.dp))
                ActionRow(
                    playLabel = "Oynat",
                    isFavorite = movie.isFavorite,
                    onPlay = { onPlay(movie.streamUrl, movie.title) },
                    onToggleFavorite = viewModel::toggleVodFavorite
                )
                Spacer(Modifier.height(20.dp))
                MetadataBlock(meta = meta, fallbackPlot = null, loading = metaLoading)
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
    val meta by viewModel.seriesMeta.collectAsState()
    val status by viewModel.seriesStatus.collectAsState()
    val metaLoading by viewModel.metaLoading.collectAsState()
    val seriesInfo by viewModel.seriesInfo.collectAsState()
    val selectedSeason by viewModel.selectedSeasonNumber.collectAsState()

    LaunchedEffect(decodedId) { viewModel.loadSeries(decodedId) }

    val show = series
    DetailScaffold(
        backdropUrl = meta?.backdropUrl ?: show?.backdropUrl ?: meta?.posterUrl ?: show?.posterUrl,
        onBack = onBack
    ) {
        when {
            show == null && status == DetailViewModel.DetailStatus.Loading -> LoadingBlock()
            show == null -> Text("Dizi bulunamadı.", color = TeTiMuted, style = MaterialTheme.typography.bodyLarge)
            else -> {
                val seasons = viewModel.seasonNumbers(seriesInfo)
                val episodes = viewModel.getEpisodesForSeason(selectedSeason)
                fun play(ep: XtreamEpisode) {
                    val url = viewModel.getEpisodeStreamUrl(ep) ?: return
                    onPlayEpisode(url, "${show.title} S${selectedSeason}B${ep.episodeNum}", "${decodedId}_${ep.id}")
                }

                TitleBlock(
                    title = show.title,
                    year = meta?.releaseYear ?: show.releaseYear,
                    runtimeMinutes = meta?.runtimeMinutes,
                    rating = meta?.voteAverage ?: show.imdbRating,
                    genres = meta?.genres ?: show.categoryName.takeIf { it.isNotBlank() },
                    extra = seasons.size.takeIf { it > 0 }?.let { "$it Sezon" }
                )
                Spacer(Modifier.height(16.dp))
                val first = episodes.firstOrNull()
                ActionRow(
                    playLabel = if (first != null) "S$selectedSeason B${first.episodeNum} Oynat" else "Oynat",
                    playEnabled = first != null,
                    isFavorite = show.isFavorite,
                    onPlay = { first?.let(::play) },
                    onToggleFavorite = viewModel::toggleSeriesFavorite
                )
                Spacer(Modifier.height(20.dp))
                MetadataBlock(meta = meta, fallbackPlot = show.plot, loading = metaLoading)

                if (seasons.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Text("Sezonlar", color = TeTiText, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(seasons, key = { it }) { sNum ->
                            PillChip(
                                text = "Sezon $sNum",
                                selected = sNum == selectedSeason,
                                onClick = { viewModel.selectSeason(sNum) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Bölümler (${episodes.size})", color = TeTiText, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (seriesInfo == null && status == DetailViewModel.DetailStatus.Ready) {
                    Text("Bölümler yükleniyor…", color = TeTiMuted, style = MaterialTheme.typography.bodyMedium)
                }
                episodes.forEach { ep -> EpisodeRow(ep, onClick = { play(ep) }) }
            }
        }
    }
}

/** Tam genişlikte backdrop + aşağı doğru siyaha eriyen gradient; içerik üstüne biner. */
@Composable
private fun DetailScaffold(
    backdropUrl: String?,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(TeTiBlack)
    ) {
        val wide = maxWidth >= 700.dp
        val backdropHeight = if (wide) maxHeight * 0.62f else (maxWidth * 0.62f).coerceAtLeast(260.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(backdropHeight)
            ) {
                if (!backdropUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = backdropUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(TeTiGlass))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.45f),
                                0.25f to Color.Transparent,
                                0.6f to TeTiBlack.copy(alpha = 0.55f),
                                1f to TeTiBlack
                            )
                        )
                )
            }
            Column(
                modifier = Modifier
                    .offset(y = (-56).dp)
                    .padding(horizontal = 16.dp)
                    .then(if (wide) Modifier.widthIn(max = 760.dp) else Modifier.fillMaxWidth())
            ) {
                content()
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
        GlassIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Geri",
            onClick = onBack,
            size = 44.dp,
            modifier = Modifier
                .statusBarsPadding()
                .padding(12.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleBlock(
    title: String,
    year: Int?,
    runtimeMinutes: Int?,
    rating: Float?,
    genres: String?,
    extra: String? = null
) {
    Text(
        text = title,
        color = TeTiText,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(Modifier.height(8.dp))
    // Yıl • Süre • ★ 7.8 • Türler
    val parts = buildList<@Composable () -> Unit> {
        year?.let { add { MetaText(it.toString()) } }
        runtimeMinutes?.takeIf { it > 0 }?.let { add { MetaText(formatRuntime(it)) } }
        extra?.let { add { MetaText(it) } }
        rating?.takeIf { it > 0f }?.let { r ->
            add {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = "TMDB puanı", tint = TeTiGold, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(formatRating(r), color = TeTiText, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        genres?.let { add { MetaText(it, maxLines = 1) } }
    }
    // Dar ekranda türler alt satıra kayar, kırpılmaz.
    FlowRow(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        parts.forEachIndexed { index, part ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (index > 0) MetaText("  •  ")
                part()
            }
        }
    }
}

@Composable
private fun MetaText(text: String, maxLines: Int = 1) {
    Text(
        text = text,
        color = TeTiMuted,
        style = MaterialTheme.typography.labelLarge,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ActionRow(
    playLabel: String,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    playEnabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PrimaryActionButton(
            text = playLabel,
            icon = Icons.Default.PlayArrow,
            onClick = onPlay,
            enabled = playEnabled,
            modifier = Modifier.weight(1f)
        )
        GlassIconButton(
            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
            onClick = onToggleFavorite,
            tint = TeTiWhite,
            active = isFavorite
        )
    }
}

@Composable
private fun MetadataBlock(meta: TmdbMetadataEntity?, fallbackPlot: String?, loading: Boolean) {
    val plot = meta?.overview ?: fallbackPlot
    var expanded by remember(plot) { mutableStateOf(false) }
    when {
        plot != null -> Text(
            text = plot,
            color = TeTiText.copy(alpha = 0.86f),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = if (expanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .animateContentSize()
                .tetiClickable(shape = RoundedCornerShape(8.dp), minSize = 0.dp) { expanded = !expanded }
        )
        loading -> Text("Özet TMDB'den yükleniyor…", color = TeTiMuted, style = MaterialTheme.typography.bodyMedium)
        else -> Text("Bu içerik için özet bulunamadı.", color = TeTiMuted, style = MaterialTheme.typography.bodyMedium)
    }
    meta?.cast?.let {
        Spacer(Modifier.height(14.dp))
        CreditLine("Oyuncular", it)
    }
    meta?.director?.let {
        Spacer(Modifier.height(6.dp))
        CreditLine("Yönetmen", it)
    }
}

@Composable
private fun CreditLine(label: String, value: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = TeTiMuted)) { append("$label: ") }
            withStyle(SpanStyle(color = TeTiText)) { append(value) }
        },
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun EpisodeRow(ep: XtreamEpisode, onClick: () -> Unit) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .tetiClickable(shape = RoundedCornerShape(16.dp), onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(112.dp)
                    .height(63.dp)
                    .background(TeTiBlack, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                val thumb = ep.info?.movieImage
                if (!thumb.isNullOrBlank()) {
                    AsyncImage(
                        model = thumb,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TeTiBlack, RoundedCornerShape(10.dp))
                    )
                }
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TeTiWhite)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${ep.episodeNum}. ${ep.title.ifBlank { "Bölüm ${ep.episodeNum}" }}",
                    color = TeTiText,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                ep.info?.durationSecs?.takeIf { it > 0 }?.let {
                    Text(formatRuntime((it / 60).toInt()), color = TeTiMuted, style = MaterialTheme.typography.bodySmall)
                }
                ep.info?.plot?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = TeTiMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun LoadingBlock() {
    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = TeTiWhite, strokeWidth = 2.dp)
    }
}

internal fun formatRuntime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}s ${m}dk"
        h > 0 -> "${h}s"
        else -> "${m}dk"
    }
}
