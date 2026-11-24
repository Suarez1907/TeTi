package com.teti.iptv.ui.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.teti.iptv.data.local.entity.VodEntity
import com.teti.iptv.domain.catalog.CatalogSort
import com.teti.iptv.ui.components.CatalogHeaderActions
import com.teti.iptv.ui.components.CategoryChipsRow
import com.teti.iptv.ui.components.ContinueCard
import com.teti.iptv.ui.components.remainingLabel
import com.teti.iptv.data.local.entity.WatchHistoryEntity
import com.teti.iptv.ui.components.EmptyHint
import com.teti.iptv.ui.components.PosterCard
import com.teti.iptv.ui.components.RatingBadge
import com.teti.iptv.ui.components.ScreenHeading
import com.teti.iptv.ui.components.SectionTitle
import com.teti.iptv.ui.components.tetiClickable
import com.teti.iptv.ui.device.rememberDeviceProfile
import com.teti.iptv.ui.theme.TeTiOnWhite
import com.teti.iptv.ui.theme.TeTiWhite
import com.teti.iptv.ui.theme.TeTiBlack
import com.teti.iptv.ui.theme.TeTiBorder
import com.teti.iptv.ui.theme.TeTiGlass
import com.teti.iptv.ui.theme.TeTiMuted
import com.teti.iptv.ui.theme.TeTiText
import com.teti.iptv.ui.viewmodel.VodViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VodScreen(
    onMovieClick: (VodEntity) -> Unit,
    onPlayMovie: (VodEntity) -> Unit,
    onResume: (WatchHistoryEntity) -> Unit,
    viewModel: VodViewModel = hiltViewModel()
) {
    val activeProfile by viewModel.activeProfile.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val years by viewModel.years.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val topRated by viewModel.topRated.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val trending by viewModel.trending.collectAsState()
    val vodPaging = viewModel.vodPaging.collectAsLazyPagingItems()
    val device = rememberDeviceProfile()
    val discover = selectedCategory == CatalogSort.CATEGORY_ALL &&
        selectedYear == null &&
        sort == CatalogSort.RECENTLY_ADDED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (activeProfile == null) {
            ScreenHeading(title = "Filmler", subtitle = "Katalog")
            EmptyHint("Lütfen Ayarlar sekmesinden bir profil ekleyin.")
            return
        }

        ScreenHeading(
            title = "Filmler",
            subtitle = if (discover) "Keşfet ve tüm katalog" else listOfNotNull(
                sort.label(),
                selectedYear?.toString(),
                selectedCategory
            ).joinToString(" · ")
        ) {
            CatalogHeaderActions(
                sort = sort,
                onSortChange = viewModel::selectSort,
                years = years,
                selectedYear = selectedYear,
                onYearChange = viewModel::selectYear
            )
        }
        CategoryChipsRow(
            categories = categories,
            selectedCategory = selectedCategory,
            onSelectCategory = viewModel::selectCategory
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (device.widthDp >= 700) 150.dp else 112.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (discover) {
                    val hero = topRated.firstOrNull() ?: recentlyAdded.firstOrNull()
                    if (hero != null) {
                        item(span = { GridItemSpan(maxLineSpan) }, contentType = "hero") {
                            MovieHero(
                                movie = hero,
                                onPlay = { onPlayMovie(hero) },
                                onInfo = { onMovieClick(hero) }
                            )
                        }
                    }
                    if (continueWatching.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, contentType = "continue") {
                            Column {
                                SectionTitle("Kaldığın yerden devam et")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(continueWatching, key = { it.contentId }) { entry ->
                                        ContinueCard(
                                            title = entry.title,
                                            imageUrl = entry.posterUrl,
                                            progress = entry.progress,
                                            remainingText = remainingLabel(entry.positionMs, entry.durationMs),
                                            onClick = { onResume(entry) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (trending.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, contentType = "row") {
                            Column {
                                SectionTitle("Bu hafta trend")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(trending, key = { "trend-" + it.streamId }) { item ->
                                        PosterCard(
                                            title = item.title,
                                            posterUrl = item.posterUrl,
                                            rating = item.imdbRating,
                                            onClick = { onMovieClick(item) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (topRated.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, contentType = "row") {
                            Column {
                            SectionTitle("En beğenilenler")
                            LazyRow(
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
                    if (recentlyAdded.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, contentType = "row") {
                            Column {
                            SectionTitle("Son eklenenler")
                            LazyRow(
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
                    item(span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                        SectionTitle("Tüm filmler")
                    }
                }

                items(
                    count = vodPaging.itemCount,
                    key = vodPaging.itemKey { it.streamId },
                    contentType = { "poster" }
                ) { index ->
                    val movie = vodPaging[index]
                    if (movie == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(TeTiGlass)
                        )
                    } else {
                        PosterCard(
                            title = movie.title,
                            posterUrl = movie.posterUrl,
                            rating = movie.imdbRating,
                            onClick = { onMovieClick(movie) },
                            fillWidth = true
                        )
                    }
                }

                if (vodPaging.loadState.refresh is LoadState.NotLoading && vodPaging.itemCount == 0) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyHint(if (discover) "Katalog boş. Aşağı çekerek senkronize edebilirsiniz." else "Bu filtreyle eşleşen film yok.")
                    }
                }
            }
        }

    }
}

@Composable
private fun MovieHero(
    movie: VodEntity,
    onPlay: () -> Unit,
    onInfo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(18.dp))
            .tetiClickable(shape = RoundedCornerShape(18.dp), onClick = onInfo)
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
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f), TeTiBlack)
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            movie.imdbRating?.let {
                RatingBadge(rating = it)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = movie.title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            movie.releaseYear?.let {
                Text(text = it.toString(), color = TeTiMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TeTiWhite, contentColor = TeTiOnWhite),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Oynat", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onInfo,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TeTiBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = TeTiGlass.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TeTiText)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Detay", color = TeTiText)
                }
            }
        }
    }
}
