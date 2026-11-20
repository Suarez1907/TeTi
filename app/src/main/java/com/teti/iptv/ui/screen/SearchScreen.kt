package com.teti.iptv.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.data.local.entity.SeriesEntity
import com.teti.iptv.data.local.entity.VodEntity
import com.teti.iptv.ui.components.PosterCard
import com.teti.iptv.ui.theme.DarkSurface
import com.teti.iptv.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onPlayChannel: (ChannelEntity) -> Unit,
    onMovieClick: (VodEntity) -> Unit,
    onSeriesClick: (SeriesEntity) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val channels by viewModel.channelResults.collectAsState(initial = emptyList())
    val vods by viewModel.vodResults.collectAsState(initial = emptyList())
    val seriesList by viewModel.seriesResults.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.onQueryChange(it) },
            placeholder = { Text("Kanal, film veya dizi ara...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Temizle", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = DarkSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (query.length < 2) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aramak için en az 2 harf yazın", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Kanallar
                if (channels.isNotEmpty()) {
                    item {
                        Text("Canlı Kanallar (${channels.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    items(channels, key = { it.streamId }) { ch ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface)
                                .clickable { onPlayChannel(ch) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                                if (!ch.logoUrl.isNullOrBlank()) {
                                    AsyncImage(model = ch.logoUrl, contentDescription = null, modifier = Modifier.size(28.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(ch.name, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }

                // Filmler
                if (vods.isNotEmpty()) {
                    item {
                        Text("Filmler (${vods.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(vods, key = { it.streamId }) { m ->
                                PosterCard(title = m.title, posterUrl = m.posterUrl, rating = m.imdbRating, onClick = { onMovieClick(m) })
                            }
                        }
                    }
                }

                // Diziler
                if (seriesList.isNotEmpty()) {
                    item {
                        Text("Diziler (${seriesList.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(seriesList, key = { it.seriesId }) { s ->
                                PosterCard(title = s.title, posterUrl = s.posterUrl, rating = s.imdbRating, onClick = { onSeriesClick(s) })
                            }
                        }
                    }
                }
            }
        }
    }
}
