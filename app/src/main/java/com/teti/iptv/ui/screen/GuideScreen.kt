package com.teti.iptv.ui.screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.data.local.entity.EpgProgramEntity
import com.teti.iptv.domain.catalog.CatalogSort
import com.teti.iptv.domain.epg.CatchupUrl
import com.teti.iptv.ui.components.EmptyHint
import com.teti.iptv.ui.components.GlassIconButton
import com.teti.iptv.ui.components.PillChip
import com.teti.iptv.ui.components.tetiClickable
import com.teti.iptv.ui.theme.TeTiBlack
import com.teti.iptv.ui.theme.TeTiBorder
import com.teti.iptv.ui.theme.TeTiFaint
import com.teti.iptv.ui.theme.TeTiGlass
import com.teti.iptv.ui.theme.TeTiGlassHigh
import com.teti.iptv.ui.theme.TeTiMuted
import com.teti.iptv.ui.theme.TeTiRed
import com.teti.iptv.ui.theme.TeTiText
import com.teti.iptv.ui.theme.TeTiWhite
import com.teti.iptv.ui.viewmodel.GuideViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CHANNEL_COLUMN = 112.dp
private val ROW_HEIGHT = 64.dp
private val DP_PER_MINUTE = 4.dp
private val DAY_WIDTH = DP_PER_MINUTE * (24 * 60)

/**
 * Izgara TV rehberi: solda kanallar, sağda saat çizelgesi. Tüm satırlar tek bir yatay kaydırma
 * durumunu paylaşır; kırmızı çizgi şu anı gösterir. Geçmiş programlar kanal arşiv tutuyorsa
 * oynatılabilir (catch-up).
 */
@Composable
fun GuideScreen(
    onBack: () -> Unit,
    onPlayLive: (ChannelEntity) -> Unit,
    onPlayCatchup: (url: String, title: String, contentId: String) -> Unit,
    viewModel: GuideViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val category by viewModel.category.collectAsState()
    val dayOffset by viewModel.dayOffset.collectAsState()
    val epgVersion by viewModel.epgVersion.collectAsState()
    val channels = viewModel.channels.collectAsLazyPagingItems()
    val dayStart = remember(dayOffset) { viewModel.dayStart(dayOffset) }
    val hScroll = rememberScrollState()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<Pair<ChannelEntity, EpgProgramEntity>?>(null) }
    val now = System.currentTimeMillis()

    // Bugünde şu ana, diğer günlerde 18:00'e kaydır.
    LaunchedEffect(dayStart) {
        val minutes = if (dayOffset == 0) ((now - dayStart) / 60_000L - 30).coerceAtLeast(0) else 18 * 60L
        val px = with(density) { (DP_PER_MINUTE * minutes.toInt()).roundToPx() }
        hScroll.scrollTo(px)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TeTiBlack)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Geri", onBack, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Text("TV Rehberi", color = TeTiText, style = MaterialTheme.typography.headlineSmall)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items((GuideViewModel.MIN_DAY..GuideViewModel.MAX_DAY).toList(), key = { it }) { offset ->
                PillChip(text = dayLabel(offset, viewModel.dayStart(offset)), selected = offset == dayOffset, onClick = { viewModel.selectDay(offset) })
            }
        }
        val allCats = listOf(CatalogSort.CATEGORY_ALL, CatalogSort.CATEGORY_FAVORITES) +
            categories.filter { it != CatalogSort.CATEGORY_ALL && it != CatalogSort.CATEGORY_FAVORITES }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(allCats, key = { it }) { cat ->
                PillChip(text = cat, selected = cat == category, onClick = { viewModel.selectCategory(cat) })
            }
        }

        // Saat cetveli
        Row(Modifier.height(28.dp)) {
            Spacer(Modifier.width(CHANNEL_COLUMN))
            Box(Modifier.horizontalScroll(hScroll).width(DAY_WIDTH).fillMaxHeight()) {
                for (h in 0 until 24) {
                    for (half in 0..1) {
                        val minute = h * 60 + half * 30
                        Text(
                            text = String.format(Locale.ROOT, "%02d:%02d", h, half * 30),
                            color = if (half == 0) TeTiMuted else TeTiFaint,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.offset(x = DP_PER_MINUTE * minute + 4.dp, y = 6.dp)
                        )
                    }
                }
                NowLine(dayStart, now, Modifier.fillMaxHeight())
            }
        }

        if (channels.itemCount == 0) {
            EmptyHint("Rehberde kanal yok. EPG'yi Ayarlar › EPG bölümünden güncelleyebilirsiniz.")
            return
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(count = channels.itemCount, key = channels.itemKey { it.streamId }) { index ->
                val channel = channels[index] ?: return@items
                GuideRow(
                    channel = channel,
                    dayStart = dayStart,
                    now = now,
                    hScroll = hScroll,
                    programsFlow = remember(channel.epgChannelId, dayStart, epgVersion) {
                        channel.epgChannelId?.let { viewModel.programs(it, dayStart) }
                    },
                    onChannelClick = { onPlayLive(channel) },
                    onProgramClick = { program -> selected = channel to program }
                )
            }
        }
    }

    selected?.let { (channel, program) ->
        val airing = now in program.startTimeMs until program.endTimeMs
        val archived = channel.tvArchive && CatchupUrl.isAvailable(channel.archiveDays, program.startTimeMs, program.endTimeMs, now)
        AlertDialog(
            onDismissRequest = { selected = null },
            containerColor = TeTiGlass,
            titleContentColor = TeTiText,
            textContentColor = TeTiText,
            title = { Text(program.title) },
            text = {
                Column {
                    Text(
                        "${channel.name} • ${timeText(program.startTimeMs)} - ${timeText(program.endTimeMs)}",
                        color = TeTiMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    program.description?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 8, overflow = TextOverflow.Ellipsis)
                    }
                    if (!airing && !archived && program.endTimeMs < now) {
                        Spacer(Modifier.height(8.dp))
                        Text("Bu kanal için geçmiş yayın arşivi yok.", color = TeTiFaint, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                when {
                    airing -> TextButton(onClick = { selected = null; onPlayLive(channel) }) {
                        Text("Canlı izle", color = TeTiWhite)
                    }
                    archived -> TextButton(onClick = {
                        scope.launch {
                            val url = viewModel.catchupUrl(channel, program)
                            selected = null
                            if (url != null) {
                                onPlayCatchup(url, "${channel.name} • ${program.title}", "catchup:${channel.streamId}:${program.startTimeMs}")
                            }
                        }
                    }) { Text("Baştan izle", color = TeTiWhite) }
                    else -> Unit
                }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Kapat", color = TeTiMuted) } }
        )
    }
}

@Composable
private fun GuideRow(
    channel: ChannelEntity,
    dayStart: Long,
    now: Long,
    hScroll: ScrollState,
    programsFlow: kotlinx.coroutines.flow.Flow<List<EpgProgramEntity>>?,
    onChannelClick: () -> Unit,
    onProgramClick: (EpgProgramEntity) -> Unit
) {
    val programs by (programsFlow ?: remember { kotlinx.coroutines.flow.flowOf(emptyList()) }).collectAsState(initial = emptyList())
    Row(
        modifier = Modifier
            .height(ROW_HEIGHT)
            .border(width = 0.5.dp, color = TeTiBorder)
    ) {
        Row(
            modifier = Modifier
                .width(CHANNEL_COLUMN)
                .fillMaxHeight()
                .background(TeTiGlass)
                .tetiClickable(shape = RoundedCornerShape(0.dp), onClick = onChannelClick)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChannelLogo(name = channel.name, logoUrl = channel.logoUrl, size = 36.dp)
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    channel.name,
                    color = TeTiText,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel.tvArchive) {
                    Icon(Icons.Default.History, contentDescription = "Arşiv var", tint = TeTiMuted, modifier = Modifier.size(14.dp))
                }
            }
        }
        Box(
            modifier = Modifier
                .horizontalScroll(hScroll)
                .width(DAY_WIDTH)
                .fillMaxHeight()
        ) {
            if (programs.isEmpty()) {
                Text(
                    if (channel.epgChannelId == null) "EPG kimliği yok" else "Program bilgisi yok",
                    color = TeTiFaint,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp)
                )
            }
            programs.forEach { program ->
                val startMin = ((program.startTimeMs - dayStart).coerceAtLeast(0L) / 60_000L).toInt()
                val endMin = ((program.endTimeMs - dayStart).coerceAtMost(GuideViewModel.DAY_MS) / 60_000L).toInt()
                if (endMin > startMin) {
                    ProgramCell(
                        program = program,
                        x = DP_PER_MINUTE * startMin,
                        width = DP_PER_MINUTE * (endMin - startMin),
                        airing = now in program.startTimeMs until program.endTimeMs,
                        past = program.endTimeMs <= now,
                        archived = channel.tvArchive && CatchupUrl.isAvailable(channel.archiveDays, program.startTimeMs, program.endTimeMs, now),
                        onClick = { onProgramClick(program) }
                    )
                }
            }
            NowLine(dayStart, now, Modifier.fillMaxHeight())
        }
    }
}

@Composable
private fun ProgramCell(
    program: EpgProgramEntity,
    x: Dp,
    width: Dp,
    airing: Boolean,
    past: Boolean,
    archived: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .offset(x = x)
            .width(width)
            .fillMaxHeight()
            .padding(2.dp)
            .tetiClickable(shape = shape, minSize = 0.dp, onClick = onClick)
            .background(if (airing) TeTiGlassHigh else TeTiBlack, shape)
            .border(1.dp, if (airing) TeTiWhite.copy(alpha = 0.5f) else TeTiBorder, shape)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .semantics { contentDescription = "${program.title}, ${timeText(program.startTimeMs)}" }
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (archived) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TeTiWhite, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                }
                Text(
                    program.title,
                    color = if (past && !archived) TeTiFaint else TeTiText,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "${timeText(program.startTimeMs)} - ${timeText(program.endTimeMs)}",
                color = TeTiFaint,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NowLine(dayStart: Long, now: Long, modifier: Modifier) {
    if (now < dayStart || now >= dayStart + GuideViewModel.DAY_MS) return
    val minute = ((now - dayStart) / 60_000L).toInt()
    Box(
        modifier = modifier
            .offset(x = DP_PER_MINUTE * minute)
            .width(2.dp)
            .background(TeTiRed)
    )
}

private fun dayLabel(offset: Int, dayStart: Long): String = when (offset) {
    0 -> "Bugün"
    -1 -> "Dün"
    1 -> "Yarın"
    else -> SimpleDateFormat("EEE d", Locale.forLanguageTag("tr-TR")).format(Date(dayStart))
}

private fun timeText(ms: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
