package com.teti.iptv.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.teti.iptv.data.local.entity.ChannelEntity
import com.teti.iptv.data.local.entity.EpgProgramEntity
import com.teti.iptv.domain.catalog.CatalogSort
import com.teti.iptv.ui.components.PillChip
import com.teti.iptv.ui.screen.ChannelCard
import com.teti.iptv.ui.theme.TeTiBorder
import com.teti.iptv.ui.theme.TeTiGlass
import com.teti.iptv.ui.theme.TeTiMuted
import com.teti.iptv.ui.theme.TeTiScrim
import com.teti.iptv.ui.theme.TeTiText
import com.teti.iptv.ui.theme.TeTiWhite
import com.teti.iptv.ui.viewmodel.EpgClock
import kotlin.math.roundToInt

/**
 * Parlaklık / ses göstergesi: ince dikey çubuk + ikon + yüzde.
 * [visible] false olduğunda yumuşakça kaybolur (zamanlama çağıranda).
 */
@Composable
fun GestureHud(kind: HudKind, level: Float, visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(350)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(TeTiScrim)
                .border(1.dp, TeTiBorder, RoundedCornerShape(28.dp))
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = hudIcon(kind, level),
                contentDescription = if (kind == HudKind.BRIGHTNESS) "Parlaklık" else "Ses",
                tint = TeTiWhite,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(140.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(level.coerceIn(0f, 1f))
                        .background(TeTiWhite)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "%${(level * 100).roundToInt()}",
                color = TeTiWhite,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun hudIcon(kind: HudKind, level: Float): ImageVector = when (kind) {
    HudKind.BRIGHTNESS -> if (level < 0.4f) Icons.Default.BrightnessLow else Icons.Default.BrightnessHigh
    HudKind.VOLUME -> when {
        level <= 0.01f -> Icons.AutoMirrored.Filled.VolumeOff
        level < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
        else -> Icons.AutoMirrored.Filled.VolumeUp
    }
}

/** Çift tık sarma geri bildirimi: dokunulan yarıda "−10 sn" / "+10 sn" balonu. */
@Composable
fun SeekFeedback(forward: Boolean, visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(100)), exit = fadeOut(tween(300)), modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(TeTiScrim)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!forward) Icon(Icons.Default.FastRewind, contentDescription = null, tint = TeTiWhite)
            Spacer(Modifier.width(6.dp))
            Text(if (forward) "+10 sn" else "−10 sn", color = TeTiWhite, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(6.dp))
            if (forward) Icon(Icons.Default.FastForward, contentDescription = null, tint = TeTiWhite)
        }
    }
}

/**
 * Sağdan kayan yarı saydam hızlı kanal ve kategori çekmecesi. Yayın arkada sürer.
 * Boşluğa dokununca veya sağa kaydırınca kapanır.
 */
@Composable
fun ChannelDrawer(
    visible: Boolean,
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    channels: LazyPagingItems<ChannelEntity>,
    currentChannelId: String?,
    clock: EpgClock,
    loadProgram: suspend (String, Long) -> EpgProgramEntity?,
    onZap: (ChannelEntity) -> Unit,
    onClose: () -> Unit
) {
    // Boşluk: dokununca kapat.
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        )
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        val panelWidth = (maxWidth * 0.86f).coerceAtMost(400.dp)
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(tween(260)) { it } + fadeIn(tween(200)),
            exit = slideOutHorizontally(tween(220)) { it } + fadeOut(tween(180))
        ) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            var dragX by remember { mutableFloatStateOf(0f) }
            Column(
                modifier = Modifier
                    .width(panelWidth)
                    .fillMaxHeight()
                    .background(TeTiGlass.copy(alpha = 0.88f))
                    .border(1.dp, TeTiBorder)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragX = 0f },
                            onDragEnd = {
                                if (GestureMath.isCloseDrawerSwipe(dragX, size.width.toFloat())) onClose()
                            }
                        ) { change, amount ->
                            change.consume()
                            dragX += amount
                        }
                    }
                    // Panel içine dokunmak boşluk dokunuşu sayılmasın.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kanallar", color = TeTiText, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = TeTiMuted)
                    }
                }
                val allCats = listOf(CatalogSort.CATEGORY_ALL, CatalogSort.CATEGORY_FAVORITES) +
                    categories.filter { it != CatalogSort.CATEGORY_ALL && it != CatalogSort.CATEGORY_FAVORITES }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allCats, key = { it }) { cat ->
                        PillChip(
                            text = cat,
                            selected = cat == selectedCategory,
                            onClick = { onSelectCategory(cat) },
                            modifier = if (cat == selectedCategory) Modifier.focusRequester(focus) else Modifier
                        )
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(count = channels.itemCount, key = channels.itemKey { it.streamId }) { index ->
                        val channel = channels[index] ?: return@items
                        ChannelCard(
                            channel = channel,
                            clock = clock,
                            loadProgram = loadProgram,
                            onPlay = { onZap(channel) },
                            onToggleFavorite = null,
                            highlighted = channel.streamId == currentChannelId
                        )
                    }
                }
            }
        }
    }
}
