package com.teti.iptv.ui.screen

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.teti.iptv.player.TrackInfo
import com.teti.iptv.ui.theme.DarkRed
import com.teti.iptv.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import java.util.Locale

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

    var showControls by remember { mutableStateOf(true) }
    var showTrackDialog by remember { mutableStateOf(false) }
    val playerFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { playerFocus.requestFocus() }
    }

    // Tam ekranda yatay moda geç
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(streamUrl) {
        viewModel.startPlayback(
            url = streamUrl,
            contentId = contentId,
            title = title,
            isLive = isLive
        )
    }

    // Kontrolleri 4 saniye sonra otomatik gizle
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    fun seekBy(deltaMs: Long) {
        val player = viewModel.getPlayer()
        val upper = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
        val target = (player.currentPosition + deltaMs).coerceIn(0L, upper)
        viewModel.seekTo(target)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerFocus)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
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
                    Key.MediaRewind -> {
                        if (!isLive) seekBy(-10_000)
                        showControls = true
                        true
                    }
                    Key.MediaFastForward -> {
                        if (!isLive) seekBy(10_000)
                        showControls = true
                        true
                    }
                    Key.DirectionLeft -> {
                        if (showControls) return@onPreviewKeyEvent false
                        if (!isLive) seekBy(-10_000)
                        showControls = true
                        true
                    }
                    Key.DirectionRight -> {
                        if (showControls) return@onPreviewKeyEvent false
                        if (!isLive) seekBy(10_000)
                        showControls = true
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
            .clickable { showControls = !showControls }
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

        // Overlay Kontroller
        if (playbackError != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(playbackError.orEmpty(), color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = viewModel::retry) {
                    Text("Tekrar Dene")
                }
                TextButton(onClick = onBack) { Text("Geri") }
            }
        } else if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                // Üst Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = { showTrackDialog = true }) {
                        Icon(Icons.Default.Subtitles, contentDescription = "Ses ve Altyazı", tint = Color.White)
                    }
                }

                // Orta Oynat / Duraklat Butonları
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isLive) {
                        IconButton(
                            onClick = { seekBy(-10_000) },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Default.Replay10, contentDescription = "10sn Geri", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }

                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    if (!isLive) {
                        IconButton(
                            onClick = { seekBy(10_000) },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Default.Forward10, contentDescription = "10sn İleri", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                }

                // Alt İlerleme Çubuğu (Canlı yayın harici)
                if (!isLive) {
                    val pos = currentPosition
                    val dur = duration.coerceAtLeast(1L)

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatDuration(pos), color = Color.White, fontSize = 12.sp)
                            Text(formatDuration(dur), color = Color.LightGray, fontSize = 12.sp)
                        }

                        Slider(
                            value = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f),
                            onValueChange = { frac ->
                                viewModel.seekTo((frac * dur).toLong())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = DarkRed,
                                activeTrackColor = DarkRed,
                                inactiveTrackColor = Color.Gray
                            )
                        )
                    }
                }
            }
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
fun TrackSelectionDialog(
    audioTracks: List<TrackInfo>,
    subtitleTracks: List<TrackInfo>,
    onSelectAudio: (TrackInfo) -> Unit,
    onSelectSubtitle: (TrackInfo?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                        Text(track.label, color = if (track.isSelected) DarkRed else Color.White, modifier = Modifier.weight(1f))
                        if (track.isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = DarkRed)
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
                    Text("Kapalı", color = if (subtitleTracks.none { it.isSelected }) DarkRed else Color.White, modifier = Modifier.weight(1f))
                    if (subtitleTracks.none { it.isSelected }) Icon(Icons.Default.Check, contentDescription = null, tint = DarkRed)
                }

                subtitleTracks.forEach { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSubtitle(track) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(track.label, color = if (track.isSelected) DarkRed else Color.White, modifier = Modifier.weight(1f))
                        if (track.isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = DarkRed)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tamam") }
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
