package com.teti.iptv.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.data.local.entity.EpgProgramEntity
import com.teti.iptv.ui.components.CategoryChipsRow
import com.teti.iptv.ui.components.EmptyHint
import com.teti.iptv.ui.components.GlassSurface
import com.teti.iptv.ui.components.LiveDot
import com.teti.iptv.ui.components.ScreenHeading
import com.teti.iptv.ui.components.ThinProgress
import com.teti.iptv.ui.components.tetiClickable
import com.teti.iptv.ui.theme.TeTiBlack
import com.teti.iptv.ui.theme.TeTiBorder
import com.teti.iptv.ui.theme.TeTiFaint
import com.teti.iptv.ui.theme.TeTiMuted
import com.teti.iptv.ui.theme.TeTiText
import com.teti.iptv.ui.theme.TeTiWhite
import com.teti.iptv.ui.viewmodel.EpgClock
import com.teti.iptv.ui.viewmodel.LiveTvViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTvScreen(
    onPlayChannel: (ChannelEntity) -> Unit,
    viewModel: LiveTvViewModel = hiltViewModel()
) {
    val activeProfile by viewModel.activeProfile.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val clock by viewModel.epgClock.collectAsState()
    val channels = viewModel.channelsPaging.collectAsLazyPagingItems()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeading(title = "Canlı TV", subtitle = activeProfile?.name ?: "Profil bekleniyor")
        if (activeProfile == null) {
            EmptyHint("Lütfen Ayarlar sekmesinden bir profil ekleyin.")
            return
        }

        CategoryChipsRow(
            categories = categories,
            selectedCategory = selectedCategory,
            onSelectCategory = { viewModel.selectCategory(it) }
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = channels.itemCount,
                    key = channels.itemKey { it.streamId },
                    contentType = { "channel" }
                ) { index ->
                    val channel = channels[index] ?: return@items
                    ChannelCard(
                        channel = channel,
                        clock = clock,
                        loadProgram = viewModel::nowPlaying,
                        onPlay = { onPlayChannel(channel) },
                        onToggleFavorite = { viewModel.toggleFavorite(channel) }
                    )
                }

                if (channels.loadState.append is LoadState.Loading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TeTiWhite, strokeWidth = 2.dp)
                        }
                    }
                }
                if (channels.loadState.refresh is LoadState.NotLoading && channels.itemCount == 0) {
                    item { EmptyHint("Bu kategoride kanal yok. Aşağı çekerek senkronize edebilirsiniz.") }
                }
            }
        }
    }
}

/**
 * Kanal kartı: logo, kanal adı + canlı gösterge, o anki program, saat aralığı ve
 * programın ne kadarının geçtiğini gösteren ince kırmızı çubuk.
 */
@Composable
fun ChannelCard(
    channel: ChannelEntity,
    clock: EpgClock,
    loadProgram: suspend (epgChannelId: String, nowMs: Long) -> EpgProgramEntity?,
    onPlay: () -> Unit,
    onToggleFavorite: (() -> Unit)?,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    val program = rememberNowPlaying(channel.epgChannelId, clock, loadProgram)
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .tetiClickable(shape = RoundedCornerShape(16.dp), onClick = onPlay)
            .then(if (highlighted) Modifier.border(1.dp, TeTiWhite, RoundedCornerShape(16.dp)) else Modifier),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChannelLogo(name = channel.name, logoUrl = channel.logoUrl, size = 52.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (program != null) {
                        LiveDot(size = 6.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = channel.name,
                        color = TeTiText,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                if (program != null) {
                    Text(
                        text = program.title,
                        color = TeTiText.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${formatClock(program.startTimeMs)} - ${formatClock(program.endTimeMs)}",
                            color = TeTiMuted,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ThinProgress(
                            fraction = programProgress(program, clock.nowMs),
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Text(
                        text = channel.categoryName.ifBlank { "Program bilgisi yok" },
                        color = TeTiFaint,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (onToggleFavorite != null) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (channel.isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
                        tint = if (channel.isFavorite) TeTiWhite else TeTiFaint
                    )
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
fun rememberNowPlaying(
    epgChannelId: String?,
    clock: EpgClock,
    loadProgram: suspend (String, Long) -> EpgProgramEntity?
): EpgProgramEntity? {
    // Dakika değişmedikçe ve EPG/kaydırma aynı kaldıkça yeniden sorgulamaz.
    val minute = clock.nowMs / 60_000
    val program by produceState<EpgProgramEntity?>(null, epgChannelId, minute, clock.epgUpdatedAt, clock.offsetHours) {
        value = epgChannelId?.let { loadProgram(it, clock.nowMs) }
    }
    return program
}

fun programProgress(program: EpgProgramEntity, nowMs: Long): Float {
    val total = (program.endTimeMs - program.startTimeMs).coerceAtLeast(1L)
    return ((nowMs - program.startTimeMs).toFloat() / total).coerceIn(0f, 1f)
}

private fun formatClock(timeMs: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMs))

@Composable
fun ChannelLogo(name: String, logoUrl: String?, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(TeTiBlack)
            .border(1.dp, TeTiBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size - 12.dp)
            )
        } else {
            Text(
                text = name.take(2).uppercase(Locale.getDefault()),
                color = TeTiText,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
