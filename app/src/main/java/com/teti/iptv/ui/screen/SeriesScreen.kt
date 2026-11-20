package com.teti.iptv.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.teti.iptv.data.local.entity.SeriesEntity
import com.teti.iptv.ui.components.CategoryChipsRow
import com.teti.iptv.ui.components.PosterCard
import com.teti.iptv.ui.viewmodel.SeriesViewModel

@Composable
fun SeriesScreen(
    onSeriesClick: (SeriesEntity) -> Unit,
    viewModel: SeriesViewModel = hiltViewModel()
) {
    val activeProfile by viewModel.activeProfile.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val topRated by viewModel.topRated.collectAsState()
    val seriesPaging = viewModel.seriesPaging.collectAsLazyPagingItems()

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
                if (topRated.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                text = "En Popüler Diziler",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(topRated, key = { it.seriesId }) { series ->
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
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    count = seriesPaging.itemCount,
                    key = { index -> seriesPaging[index]?.seriesId ?: index }
                ) { index ->
                    val series = seriesPaging[index] ?: return@items
                    PosterCard(
                        title = series.title,
                        posterUrl = series.posterUrl,
                        rating = series.imdbRating,
                        onClick = { onSeriesClick(series) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
