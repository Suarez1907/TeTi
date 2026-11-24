package com.teti.iptv.ui.screen

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.paging.compose.collectAsLazyPagingItems
import com.teti.iptv.ui.components.PillChip
import com.teti.iptv.ui.player.ChannelDrawer
import com.teti.iptv.ui.theme.TeTiBorder
import com.teti.iptv.ui.theme.TeTiGlass
import com.teti.iptv.ui.theme.TeTiMuted
import com.teti.iptv.ui.theme.TeTiScrim
import com.teti.iptv.ui.theme.TeTiText
import com.teti.iptv.ui.theme.TeTiWhite
import com.teti.iptv.ui.viewmodel.EpgClock
import com.teti.iptv.ui.viewmodel.MultiViewViewModel

/** 2 veya 4 kanalı aynı anda izleme. Bir karoya dokunmak sesini açar. */
@OptIn(UnstableApi::class)
@Composable
fun MultiViewScreen(
    onBack: () -> Unit,
    viewModel: MultiViewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val slotCount by viewModel.slotCount.collectAsState()
    val slots by viewModel.slots.collectAsState()
    val audioSlot by viewModel.audioSlot.collectAsState()
    val categories by viewModel.pickerCategories.collectAsState()
    val pickerCategory by viewModel.pickerCategory.collectAsState()
    val pickerChannels = viewModel.pickerChannels.collectAsLazyPagingItems()
    var pickingSlot by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val original = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { activity?.requestedOrientation = original ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    BackHandler(enabled = pickingSlot != null) { pickingSlot = null }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = TeTiWhite)
                }
                Text("Çoklu ekran", color = TeTiText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PillChip("2 ekran", selected = slotCount == 2, onClick = { viewModel.setSlotCount(2) })
                    PillChip("4 ekran", selected = slotCount == 4, onClick = { viewModel.setSlotCount(4) })
                }
                Spacer(Modifier.width(8.dp))
            }
            val rows = if (slotCount == 2) listOf(listOf(0, 1)) else listOf(listOf(0, 1), listOf(2, 3))
            Column(Modifier.weight(1f).padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                rows.forEach { row ->
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { index ->
                            val slot = slots[index]
                            Tile(
                                index = index,
                                channelName = slot.channel?.name,
                                error = slot.error,
                                hasAudio = index == audioSlot,
                                viewModel = viewModel,
                                onSelect = {
                                    if (slot.channel == null) pickingSlot = index else viewModel.selectAudio(index)
                                },
                                onChange = { pickingSlot = index },
                                onClose = { viewModel.clear(index) },
                                modifier = Modifier.weight(1f).fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        ChannelDrawer(
            visible = pickingSlot != null,
            categories = categories,
            selectedCategory = pickerCategory,
            onSelectCategory = viewModel::selectPickerCategory,
            channels = pickerChannels,
            currentChannelId = pickingSlot?.let { slots[it].channel?.streamId },
            clock = EpgClock(System.currentTimeMillis(), 0L, 0),
            loadProgram = viewModel::nowPlaying,
            onZap = { channel ->
                pickingSlot?.let { slot ->
                    viewModel.assign(slot, channel)
                    viewModel.selectAudio(slot)
                }
                pickingSlot = null
            },
            onClose = { pickingSlot = null }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun Tile(
    index: Int,
    channelName: String?,
    error: String?,
    hasAudio: Boolean,
    viewModel: MultiViewViewModel,
    onSelect: () -> Unit,
    onChange: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(TeTiGlass)
            .border(if (hasAudio && channelName != null) 2.dp else 1.dp, if (hasAudio && channelName != null) TeTiWhite else TeTiBorder, shape)
            .clickable(onClick = onSelect)
            .semantics { contentDescription = channelName?.let { "Ekran ${index + 1}: $it" } ?: "Ekran ${index + 1}: boş, kanal ekle" }
    ) {
        if (channelName == null) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Add, contentDescription = null, tint = TeTiMuted, modifier = Modifier.size(36.dp))
                Text("Kanal ekle", color = TeTiMuted, style = MaterialTheme.typography.labelLarge)
            }
            return@Box
        }
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    player = viewModel.playerFor(index)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(TeTiScrim)
                .padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasAudio) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Ses bu ekranda", tint = TeTiWhite, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(channelName, color = TeTiText, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = onChange, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Kanalı değiştir", tint = TeTiWhite, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Ekranı kapat", tint = TeTiWhite, modifier = Modifier.size(18.dp))
            }
        }
        error?.let {
            Text(it, color = TeTiText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Center).padding(8.dp))
        }
    }
}
