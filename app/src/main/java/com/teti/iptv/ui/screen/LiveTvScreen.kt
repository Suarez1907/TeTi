package com.teti.iptv.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.ui.components.CategoryChipsRow
import com.teti.iptv.ui.components.EmptyHint
import com.teti.iptv.ui.components.ScreenHeading
import com.teti.iptv.ui.components.tetiClickable
import com.teti.iptv.ui.theme.TeTiAmber
import com.teti.iptv.ui.theme.TeTiSignal
import com.teti.iptv.ui.theme.TeTiSurface
import com.teti.iptv.ui.theme.TeTiText
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
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val currentEpg by viewModel.currentEpg.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val channels = viewModel.channelsPaging.collectAsLazyPagingItems()
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

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

        selectedChannel?.let { channel ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .tetiClickable(shape = RoundedCornerShape(16.dp)) { onPlayChannel(channel) },
                color = TeTiSurface,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChannelLogo(name = channel.name, logoUrl = channel.logoUrl, size = 52.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(TeTiSignal)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = channel.name,
                                color = TeTiText,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (currentEpg != null) {
                            val start = timeFormat.format(Date(currentEpg!!.startTimeMs))
                            val end = timeFormat.format(Date(currentEpg!!.endTimeMs))
                            Text(
                                text = "$start – $end  ${currentEpg!!.title}",
                                color = TeTiSignal,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text("EPG bilgisi yok", color = Color(0xFF9A958C), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    IconButton(onClick = { onPlayChannel(channel) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Oynat", tint = TeTiAmber, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                items(
                    count = channels.itemCount,
                    key = channels.itemKey { it.streamId }
                ) { index ->
                    val channel = channels[index] ?: return@items
                    val isSelected = selectedChannel?.streamId == channel.streamId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                        .tetiClickable(shape = RoundedCornerShape(14.dp)) {
                            viewModel.selectChannel(channel)
                            onPlayChannel(channel)
                        }
                        .background(if (isSelected) TeTiSurface else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChannelLogo(name = channel.name, logoUrl = channel.logoUrl, size = 44.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = channel.name,
                            color = if (isSelected) TeTiAmber else TeTiText,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.toggleFavorite(channel) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (channel.isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
                                tint = if (channel.isFavorite) TeTiAmber else Color(0xFF9A958C)
                            )
                        }
                    }
                }

                if (channels.loadState.append is LoadState.Loading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TeTiAmber)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelLogo(name: String, logoUrl: String?, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size - 8.dp)
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
