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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.ui.components.CategoryChipsRow
import com.teti.iptv.ui.theme.DarkRed
import com.teti.iptv.ui.theme.DarkSurface
import com.teti.iptv.ui.viewmodel.LiveTvViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val channels = viewModel.channelsPaging.collectAsLazyPagingItems()

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (activeProfile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Lütfen Ayarlar sekmesinden bir profil ekleyin.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            return
        }

        // Kategori Seçici
        CategoryChipsRow(
            categories = categories,
            selectedCategory = selectedCategory,
            onSelectCategory = { viewModel.selectCategory(it) }
        )

        // Seçili Kanal & EPG Önizleme Barı (TiviMate stili)
        selectedChannel?.let { channel ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = DarkSurface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!channel.logoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = channel.logoUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(40.dp)
                            )
                        } else {
                            Text(
                                text = channel.name.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = channel.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (currentEpg != null) {
                            val start = timeFormat.format(Date(currentEpg!!.startTimeMs))
                            val end = timeFormat.format(Date(currentEpg!!.endTimeMs))
                            Text(
                                text = "$start - $end  ${currentEpg!!.title}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "EPG bilgisi bulunamadı",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = { onPlayChannel(channel) }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Oynat",
                            tint = DarkRed,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // Kanal Listesi
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            items(
                count = channels.itemCount,
                key = { index -> channels[index]?.streamId ?: index }
            ) { index ->
                val channel = channels[index] ?: return@items
                val isSelected = selectedChannel?.streamId == channel.streamId

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) DarkSurface else Color.Transparent)
                        .clickable {
                            viewModel.selectChannel(channel)
                            onPlayChannel(channel)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!channel.logoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = channel.logoUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(30.dp)
                            )
                        } else {
                            Text(
                                text = channel.name.take(2).uppercase(),
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = channel.name,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { viewModel.toggleFavorite(channel) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (channel.isFavorite) DarkRed else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (channels.loadState.append is LoadState.Loading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
