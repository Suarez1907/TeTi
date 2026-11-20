package com.teti.iptv.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.teti.iptv.data.local.entity.VodEntity
import com.teti.iptv.ui.components.CategoryChipsRow
import com.teti.iptv.ui.components.PosterCard
import com.teti.iptv.ui.components.RatingBadge
import com.teti.iptv.ui.theme.DarkRed
import com.teti.iptv.ui.theme.DarkSurface
import com.teti.iptv.ui.viewmodel.VodViewModel

@Composable
fun VodScreen(
    onMovieClick: (VodEntity) -> Unit,
    onPlayMovie: (VodEntity) -> Unit,
    viewModel: VodViewModel = hiltViewModel()
) {
    val activeProfile by viewModel.activeProfile.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val topRated by viewModel.topRated.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val vodPaging = viewModel.vodPaging.collectAsLazyPagingItems()

    val heroMovie = topRated.firstOrNull() ?: recentlyAdded.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (activeProfile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Lütfen Ayarlar sekmesinden bir profil ekleyin.", color = Color.Gray)
            }
            return
        }

        CategoryChipsRow(
            categories = categories,
            selectedCategory = selectedCategory,
            onSelectCategory = { viewModel.selectCategory(it) }
        )

        if (selectedCategory == "Tümü" || selectedCategory == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Netflix Tarzı Hero Banner
                heroMovie?.let { hero ->
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clickable { onMovieClick(hero) }
                        ) {
                            AsyncImage(
                                model = hero.backdropUrl ?: hero.posterUrl,
                                contentDescription = hero.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Karartma gradyanı
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.5f),
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                            )

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                hero.imdbRating?.let {
                                    RatingBadge(rating = it)
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                Text(
                                    text = hero.title,
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { onPlayMovie(hero) },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkRed),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Oynat", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { onMovieClick(hero) },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Detay", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // En Çok Oy Alanlar Carousel
                if (topRated.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                text = "En Beğenilen Filmler",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(topRated, key = { it.streamId }) { movie ->
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

                // Son Eklenenler Carousel
                if (recentlyAdded.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                text = "Son Eklenenler",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(recentlyAdded, key = { it.streamId }) { movie ->
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
            }
        } else {
            // Kategoriye Göre Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    count = vodPaging.itemCount,
                    key = { index -> vodPaging[index]?.streamId ?: index }
                ) { index ->
                    val movie = vodPaging[index] ?: return@items
                    PosterCard(
                        title = movie.title,
                        posterUrl = movie.posterUrl,
                        rating = movie.imdbRating,
                        onClick = { onMovieClick(movie) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
