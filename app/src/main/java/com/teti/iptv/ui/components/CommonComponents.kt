package com.teti.iptv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.teti.iptv.domain.catalog.CatalogSort
import com.teti.iptv.ui.theme.TeTiBorder
import com.teti.iptv.ui.theme.TeTiGlass
import com.teti.iptv.ui.theme.TeTiGold
import com.teti.iptv.ui.theme.TeTiMuted
import com.teti.iptv.ui.theme.TeTiOnWhite
import com.teti.iptv.ui.theme.TeTiRed
import com.teti.iptv.ui.theme.TeTiText
import com.teti.iptv.ui.theme.TeTiWhite
import java.util.Locale

/** İnce kenarlıklı, minimal kategori chip'leri (pill). Seçili olan beyaz dolgulu. */
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(allCats, key = { it }) { cat ->
            PillChip(
                text = cat,
                selected = (selectedCategory ?: CatalogSort.CATEGORY_ALL) == cat,
                onClick = { onSelectCategory(cat) }
            )
        }
    }
}

@Composable
fun PillChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .heightIn(min = 36.dp)
            .tetiClickable(shape = shape, minSize = 36.dp, onClick = onClick)
            .background(if (selected) TeTiWhite else Color.Transparent, shape)
            .border(1.dp, if (selected) TeTiWhite else TeTiBorder, shape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) TeTiOnWhite else TeTiText,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

/** Cam efektli yüzey: koyu yarı opak dolgu + ince kenarlık. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    alpha: Float = 1f,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = TeTiGlass.copy(alpha = alpha),
        shape = shape,
        border = BorderStroke(1.dp, TeTiBorder),
        content = content
    )
}

/** Beyaz zemin + siyah metin ana aksiyon butonu. */
@Composable
fun PrimaryActionButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TeTiWhite,
            contentColor = TeTiOnWhite,
            disabledContainerColor = TeTiWhite.copy(alpha = 0.4f),
            disabledContentColor = TeTiOnWhite.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/** Cam efektli kare ikon butonu (favori, sıralama vb.). */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TeTiText,
    size: Dp = 52.dp,
    active: Boolean = false
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(size)
            .tetiClickable(shape = shape, minSize = size, onClick = onClick)
            .background(if (active) TeTiWhite.copy(alpha = 0.12f) else TeTiGlass, shape)
            .border(1.dp, TeTiBorder, shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun RatingBadge(
    rating: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = "TMDB puanı ${formatRating(rating)}" },
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
                tint = TeTiGold,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = formatRating(rating),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

fun formatRating(rating: Float): String = String.format(Locale.US, "%.1f", rating)

/** Canlı yayın göstergesi (kırmızı nokta). */
@Composable
fun LiveDot(modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(TeTiRed)
    )
}

/** İnce kırmızı ilerleme çubuğu (EPG ve izleme ilerlemesi). */
@Composable
fun ThinProgress(fraction: Float, modifier: Modifier = Modifier, height: Dp = 3.dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(TeTiBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .background(TeTiRed)
        )
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
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier.width(128.dp))
            .tetiClickable(shape = RoundedCornerShape(12.dp), onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(TeTiGlass)
                .border(1.dp, TeTiBorder, RoundedCornerShape(12.dp))
        ) {
            if (!posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = posterUrl,
                    // Başlık aşağıda metin olarak var; TalkBack iki kez okumasın.
                    contentDescription = null,
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
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
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

/** "Devam et" kartı: yatay görsel, başlık ve izlenen kısmı gösteren kırmızı çubuk. */
@Composable
fun ContinueCard(
    title: String,
    imageUrl: String?,
    progress: Float,
    remainingText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(200.dp)
            .tetiClickable(shape = RoundedCornerShape(12.dp), onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(TeTiGlass)
                .border(1.dp, TeTiBorder, RoundedCornerShape(12.dp))
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TeTiWhite)
            }
            ThinProgress(
                fraction = progress,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, color = TeTiText, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        remainingText?.let { Text(it, color = TeTiMuted, style = MaterialTheme.typography.labelMedium) }
    }
}

fun remainingLabel(positionMs: Long, durationMs: Long): String? {
    if (durationMs <= 0) return null
    val minutes = ((durationMs - positionMs) / 60_000L).coerceAtLeast(1L)
    return "$minutes dk kaldı"
}
