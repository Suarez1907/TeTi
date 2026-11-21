package com.teti.iptv.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.teti.iptv.domain.catalog.CatalogSort
import com.teti.iptv.ui.theme.DarkGold
import com.teti.iptv.ui.theme.DarkSurface
import com.teti.iptv.ui.theme.TeTiAmber
import com.teti.iptv.ui.theme.TeTiMuted
import com.teti.iptv.ui.theme.TeTiSurfaceHigh
import com.teti.iptv.ui.theme.TeTiText
import java.util.Locale

@Composable
fun CategoryChipsRow(
    categories: List<String>,
    selectedCategory: String?,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allCats = listOf(CatalogSort.CATEGORY_ALL, CatalogSort.CATEGORY_FAVORITES) +
        categories.filter { it != CatalogSort.CATEGORY_ALL && it != CatalogSort.CATEGORY_FAVORITES }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(allCats, key = { it }) { cat ->
            val isSelected = (selectedCategory ?: CatalogSort.CATEGORY_ALL) == cat
            FilterChip(
                selected = isSelected,
                onClick = { onSelectCategory(cat) },
                label = {
                    Text(
                        cat,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TeTiAmber,
                    selectedLabelColor = Color(0xFF1A1006),
                    containerColor = TeTiSurfaceHigh,
                    labelColor = TeTiText
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun RatingBadge(
    rating: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.72f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = DarkGold,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = String.format(Locale.US, "%.1f", rating),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun PosterCard(
    title: String,
    posterUrl: String?,
    rating: Float? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false
) {
    Column(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier.width(132.dp))
            .tetiClickable(shape = RoundedCornerShape(14.dp), onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurface)
        ) {
            if (!posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = title.take(1).uppercase(Locale.getDefault()),
                    color = TeTiMuted,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            if (rating != null && rating > 0f) {
                RatingBadge(
                    rating = rating,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            color = TeTiText,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = TeTiText,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = TeTiMuted,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(24.dp)
        )
    }
}
