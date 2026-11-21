package com.teti.iptv.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.data.local.entity.SeriesEntity
import com.teti.iptv.data.local.entity.VodEntity
import com.teti.iptv.ui.components.EmptyHint
import com.teti.iptv.ui.components.PosterCard
import com.teti.iptv.ui.components.ScreenHeading
import com.teti.iptv.ui.components.SectionTitle
import com.teti.iptv.ui.components.tetiClickable
import com.teti.iptv.ui.theme.TeTiMuted
import com.teti.iptv.ui.theme.TeTiGlass
import com.teti.iptv.ui.theme.TeTiText
import com.teti.iptv.ui.viewmodel.SearchViewModel

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
    ) {
        ScreenHeading(title = "Arama", subtitle = "Kanal, film veya dizi")
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.onQueryChange(it) },
            placeholder = { Text("En az 2 harf yazın", color = TeTiMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TeTiMuted) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Temizle", tint = TeTiMuted)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TeTiText),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = TeTiGlass,
                unfocusedContainerColor = TeTiGlass,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(64.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (query.length < 2) {
            EmptyHint("Aramak için en az 2 harf yazın")
        } else if (channels.isEmpty() && vods.isEmpty() && seriesList.isEmpty()) {
            EmptyHint("Sonuç bulunamadı")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (channels.isNotEmpty()) {
                    item { SectionTitle("Canlı kanallar (${channels.size})", modifier = Modifier.padding(horizontal = 16.dp)) }
                    items(channels, key = { it.streamId }) { ch ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .tetiClickable(shape = RoundedCornerShape(14.dp)) { onPlayChannel(ch) }
                                .background(TeTiGlass)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!ch.logoUrl.isNullOrBlank()) {
                                    AsyncImage(model = ch.logoUrl, contentDescription = null, modifier = Modifier.size(36.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(ch.name, color = TeTiText, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                if (vods.isNotEmpty()) {
                    item {
                        Column {
                            SectionTitle("Filmler (${vods.size})", modifier = Modifier.padding(horizontal = 16.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(vods, key = { it.streamId }) { movie ->
                                    PosterCard(
                                        title = movie.title,
                                        posterUrl = movie.posterUrl,
                                        rating = movie.imdbRating,
                                        onClick = { onMovieClick(movie) }
                                    )
                                }
                            }
                        }
                    }
                }
                if (seriesList.isNotEmpty()) {
                    item {
                        Column {
                            SectionTitle("Diziler (${seriesList.size})", modifier = Modifier.padding(horizontal = 16.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(seriesList, key = { it.seriesId }) { series ->
                                    PosterCard(
                                        title = series.title,
                                        posterUrl = series.posterUrl,
                                        rating = series.imdbRating,
                                        onClick = { onSeriesClick(series) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
