package com.teti.iptv.ui.screen

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.paging.compose.collectAsLazyPagingItems
import com.teti.iptv.player.TrackInfo
import com.teti.iptv.ui.components.ThinProgress
import com.teti.iptv.ui.player.ChannelDrawer
import com.teti.iptv.ui.player.GestureHud
import com.teti.iptv.ui.player.GestureMath
import com.teti.iptv.ui.player.HudKind
import com.teti.iptv.ui.player.SeekFeedback
import com.teti.iptv.ui.player.playerDragGestures
import com.teti.iptv.ui.player.rememberBrightnessController
import com.teti.iptv.ui.player.rememberVolumeController
import com.teti.iptv.ui.theme.TeTiGlass
import com.teti.iptv.ui.theme.TeTiMuted
import com.teti.iptv.ui.theme.TeTiRed
import com.teti.iptv.ui.theme.TeTiText
import com.teti.iptv.ui.theme.TeTiWhite
import com.teti.iptv.ui.viewmodel.PlaybackItem
import com.teti.iptv.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tam ekran oynatıcı (TeTiPlayerScreen). Tek elle kullanım için hareketler:
 *  - ortadan sola kaydırma (canlı TV): hızlı kanal çekmecesi
 *  - sol yarıda dikey: parlaklık, sağ yarıda dikey: ses (HUD 1 sn sonra kaybolur)
 *  - çift tık (VOD/dizi): sol yarı −10 sn, sağ yarı +10 sn
 * Yatay ve dikey hareketler eksen kilidiyle ayrılır ([GestureMath.decideAxis]).
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    streamUrl: String,
    title: String,
    contentId: String,
    isLive: Boolean,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isPlaying by viewModel.isPlaying.collectAsState()
    val audioTracks by viewModel.audioTracks.collectAsState()
    val subtitleTracks by viewModel.subtitleTracks.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val playbackError by viewModel.playbackError.collectAsState()
    val current by viewModel.current.collectAsState()
    val program by viewModel.currentProgram.collectAsState()
    val clock by viewModel.epgClock.collectAsState()

    val item = current ?: PlaybackItem(streamUrl, title, contentId, isLive)
    val live = item.isLive

    var showControls by remember { mutableStateOf(true) }
    var showTrackDialog by remember { mutableStateOf(false) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    val playerFocus = remember { FocusRequester() }

    // HUD (parlaklık/ses) durumu: her değişimde sayaç artar, 1 sn hareketsizlikte gizlenir.
    val brightness = rememberBrightnessController()
    val volume = rememberVolumeController()
    var hudKind by remember { mutableStateOf(HudKind.VOLUME) }
    var hudLevel by remember { mutableFloatStateOf(0f) }
    var hudVisible by remember { mutableStateOf(false) }
    var hudTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(hudTick) {
        if (hudTick == 0) return@LaunchedEffect
        delay(1000)
        hudVisible = false
    }

    // Çift tık sarma geri bildirimi.
    var seekForward by remember { mutableStateOf(true) }
    var seekVisible by remember { mutableStateOf(false) }
    var seekTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(seekTick) {
        if (seekTick == 0) return@LaunchedEffect
        delay(650)
        seekVisible = false
    }

    LaunchedEffect(Unit) {
        viewModel.open(PlaybackItem(streamUrl, title, contentId, isLive))
        runCatching { playerFocus.requestFocus() }
    }

    // Tam ekranda yatay moda geç
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Kontrolleri 4 saniye sonra otomatik gizle
    LaunchedEffect(showControls, isPlaying, drawerOpen) {
        if (showControls && isPlaying && !drawerOpen) {
            delay(4000)
            showControls = false
        }
    }

    BackHandler(enabled = drawerOpen) { drawerOpen = false }

    fun seekWithFeedback(forward: Boolean) {
        viewModel.seekBy(if (forward) 10_000 else -10_000)
        seekForward = forward
        seekVisible = true
        seekTick++
    }

    fun openDrawer() {
        if (!live) return
        drawerOpen = true
        showControls = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerFocus)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || drawerOpen) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.MediaPlayPause, Key.Spacebar -> {
                        if (!showControls) {
                            viewModel.togglePlayPause()
                            showControls = true
                            true
                        } else {
                            false
                        }
                    }
                    Key.Menu, Key.Guide -> {
                        if (live) openDrawer()
                        live
                    }
                    Key.MediaRewind -> {
                        if (!live) seekWithFeedback(forward = false)
                        showControls = true
                        true
                    }
                    Key.MediaFastForward -> {
                        if (!live) seekWithFeedback(forward = true)
                        showControls = true
                        true
                    }
                    Key.DirectionLeft -> {
                        if (showControls) return@onPreviewKeyEvent false
                        if (!live) seekWithFeedback(forward = false)
                        showControls = !live
                        true
                    }
                    Key.DirectionRight -> {
                        if (showControls) return@onPreviewKeyEvent false
                        if (live) openDrawer() else seekWithFeedback(forward = true)
                        true
                    }
                    Key.DirectionUp, Key.DirectionDown -> {
                        if (showControls) return@onPreviewKeyEvent false
                        showControls = true
                        true
                    }
                    else -> false
                }
            }
            .focusable()
            .playerDragGestures(
                enabled = !drawerOpen,
                readLevel = { kind ->
                    if (kind == HudKind.BRIGHTNESS) brightness.read() else volume.read()
                },
                onVertical = { kind, level ->
                    if (kind == HudKind.BRIGHTNESS) brightness.set(level) else volume.set(level)
                    hudKind = kind
                    hudLevel = level
                    hudVisible = true
                    hudTick++
                },
                onVerticalEnd = { volume.endGesture() },
                onSwipeLeft = { startX, dx, width ->
                    if (live && GestureMath.isOpenDrawerSwipe(startX, dx, width)) openDrawer()
                },
                onSwipeRight = { _, _ -> }
            )
            .pointerInput(live, drawerOpen) {
                if (drawerOpen) return@pointerInput
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    // Canlı yayında sarma yok; çift tık beklemeden tek tık anında çalışsın.
                    onDoubleTap = if (live) null else { offset ->
                        seekWithFeedback(forward = offset.x > size.width / 2f)
                    }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.getPlayer()
                    useController = false
                    keepScreenOn = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (playbackError != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(18.dp))
                    .background(TeTiGlass.copy(alpha = 0.92f))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(playbackError.orEmpty(), color = TeTiText)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = viewModel::retry) { Text("Tekrar Dene", color = TeTiWhite) }
                    TextButton(onClick = onBack) { Text("Geri", color = TeTiMuted) }
                    if (live) TextButton(onClick = { openDrawer() }) { Text("Kanallar", color = TeTiWhite) }
                }
            }
        }

        AnimatedVisibility(
            visible = showControls && playbackError == null && !drawerOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.7f),
                            0.3f to Color.Transparent,
                            0.7f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.75f)
                        )
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                // Üst çubuk
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (live) {
                                LiveBadge()
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        program?.let { p ->
                            Text(
                                text = "${clockText(p.startTimeMs)} - ${clockText(p.endTimeMs)}  ${p.title}",
                                color = TeTiText.copy(alpha = 0.75f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (live) {
                        IconButton(onClick = { openDrawer() }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Kanal listesi", tint = Color.White)
                        }
                    }
                    IconButton(onClick = { showTrackDialog = true }) {
                        Icon(Icons.Default.Subtitles, contentDescription = "Ses ve Altyazı", tint = Color.White)
                    }
                }

                // Orta oynat / duraklat
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!live) {
                        IconButton(onClick = { seekWithFeedback(forward = false) }, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Default.Replay10, contentDescription = "10 sn geri", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                    }
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.35f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                            tint = Color.White,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                    if (!live) {
                        IconButton(onClick = { seekWithFeedback(forward = true) }, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Default.Forward10, contentDescription = "10 sn ileri", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                    }
                }

                // Alt: VOD ilerleme çubuğu / canlıda program ilerlemesi
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    if (!live) {
                        val dur = duration.coerceAtLeast(1L)
                        Slider(
                            value = (currentPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f),
                            onValueChange = { frac -> viewModel.seekTo((frac * dur).toLong()) },
                            colors = SliderDefaults.colors(
                                thumbColor = TeTiWhite,
                                activeTrackColor = TeTiRed,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            )
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatDuration(currentPosition), color = Color.White, fontSize = 12.sp)
                            Text(formatDuration(dur), color = Color.LightGray, fontSize = 12.sp)
                        }
                    } else {
                        program?.let { p ->
                            ThinProgress(fraction = programProgress(p, clock.nowMs))
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = "Kanal listesi için sola kaydırın",
                            color = TeTiText.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // Çift tık sarma balonları
        SeekFeedback(
            forward = false,
            visible = seekVisible && !seekForward,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 48.dp)
        )
        SeekFeedback(
            forward = true,
            visible = seekVisible && seekForward,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp)
        )

        // Parlaklık HUD'u solda, ses HUD'u sağda
        GestureHud(
            kind = hudKind,
            level = hudLevel,
            visible = hudVisible,
            modifier = Modifier
                .align(if (hudKind == HudKind.BRIGHTNESS) Alignment.CenterStart else Alignment.CenterEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 32.dp)
        )

        if (live) {
            val categories by viewModel.drawerCategories.collectAsState()
            val drawerCategory by viewModel.drawerCategory.collectAsState()
            val drawerChannels = viewModel.drawerChannels.collectAsLazyPagingItems()
            ChannelDrawer(
                visible = drawerOpen,
                categories = categories,
                selectedCategory = drawerCategory,
                onSelectCategory = viewModel::selectDrawerCategory,
                channels = drawerChannels,
                currentChannelId = item.contentId,
                clock = clock,
                loadProgram = viewModel::nowPlaying,
                onZap = { channel -> viewModel.zapTo(channel) },
                onClose = {
                    drawerOpen = false
                    runCatching { playerFocus.requestFocus() }
                }
            )
        }
    }

    if (showTrackDialog) {
        TrackSelectionDialog(
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            onSelectAudio = { viewModel.selectAudio(it) },
            onSelectSubtitle = { viewModel.selectSubtitle(it) },
            onDismiss = { showTrackDialog = false }
        )
    }
}

@Composable
private fun LiveBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(TeTiRed)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("CANLI", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

private fun clockText(timeMs: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMs))

@Composable
fun TrackSelectionDialog(
    audioTracks: List<TrackInfo>,
    subtitleTracks: List<TrackInfo>,
    onSelectAudio: (TrackInfo) -> Unit,
    onSelectSubtitle: (TrackInfo?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TeTiGlass,
        titleContentColor = TeTiText,
        textContentColor = TeTiText,
        title = { Text("Ses ve Altyazı Ayarları") },
        text = {
            Column {
                Text("Ses Parçaları", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                audioTracks.forEach { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAudio(track) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(track.label, color = if (track.isSelected) TeTiWhite else TeTiMuted, modifier = Modifier.weight(1f))
                        if (track.isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = TeTiWhite)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Altyazılar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSubtitle(null) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kapalı", color = if (subtitleTracks.none { it.isSelected }) TeTiWhite else TeTiMuted, modifier = Modifier.weight(1f))
                    if (subtitleTracks.none { it.isSelected }) Icon(Icons.Default.Check, contentDescription = null, tint = TeTiWhite)
                }

                subtitleTracks.forEach { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSubtitle(track) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(track.label, color = if (track.isSelected) TeTiWhite else TeTiMuted, modifier = Modifier.weight(1f))
                        if (track.isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = TeTiWhite)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tamam", color = TeTiWhite) }
        }
    )
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
