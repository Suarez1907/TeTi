package com.teti.iptv.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.teti.iptv.data.local.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

data class TrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val language: String,
    val label: String,
    val isSelected: Boolean
)

@OptIn(UnstableApi::class)
@Singleton
class TeTiPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val preferences: AppPreferences
) {
    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val audioTracks: StateFlow<List<TrackInfo>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val subtitleTracks: StateFlow<List<TrackInfo>> = _subtitleTracks.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    fun getPlayer(): ExoPlayer {
        return exoPlayer ?: initializePlayer()
    }

    private fun initializePlayer(): ExoPlayer {
        val bufferConfig = preferences.bufferProfile.value
        val useSoftwareDecoder = preferences.useSoftwareDecoder.value
        val userAgent = preferences.userAgent.value

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                bufferConfig.minBufferMs,
                bufferConfig.maxBufferMs,
                bufferConfig.bufferForPlaybackMs,
                bufferConfig.bufferForPlaybackAfterRebufferMs
            )
            .build()

        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(
                if (useSoftwareDecoder) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
            )
        }

        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(userAgent)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        trackSelector = DefaultTrackSelector(context)

        return ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector!!)
            .build().also { player ->
                exoPlayer = player
                player.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        updateAvailableTracks(tracks)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _duration.value = player.duration.coerceAtLeast(0L)
                        }
                    }
                })
            }
    }

    fun play(url: String, startPositionMs: Long = 0L) {
        val player = getPlayer()
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem, startPositionMs)
        player.prepare()
        player.playWhenReady = true
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun selectAudioTrack(trackInfo: TrackInfo) {
        val selector = trackSelector ?: return
        val currentTracks = exoPlayer?.currentTracks ?: return
        val groups = currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        if (trackInfo.groupIndex in groups.indices) {
            val group = groups[trackInfo.groupIndex]
            selector.parameters = selector.buildUponParameters()
                .setOverrideForType(
                    TrackSelectionOverride(group.mediaTrackGroup, trackInfo.trackIndex)
                )
                .build()
        }
    }

    fun selectSubtitleTrack(trackInfo: TrackInfo?) {
        val selector = trackSelector ?: return
        if (trackInfo == null) {
            selector.parameters = selector.buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            return
        }
        val currentTracks = exoPlayer?.currentTracks ?: return
        val groups = currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
        if (trackInfo.groupIndex in groups.indices) {
            val group = groups[trackInfo.groupIndex]
            selector.parameters = selector.buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(
                    TrackSelectionOverride(group.mediaTrackGroup, trackInfo.trackIndex)
                )
                .build()
        }
    }

    private fun updateAvailableTracks(tracks: Tracks) {
        val audio = mutableListOf<TrackInfo>()
        val subtitles = mutableListOf<TrackInfo>()

        var audioGroupIdx = 0
        var subGroupIdx = 0

        for (group in tracks.groups) {
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        audio.add(
                            TrackInfo(
                                groupIndex = audioGroupIdx,
                                trackIndex = i,
                                language = format.language ?: "Bilinmeyen",
                                label = format.label ?: format.language ?: "Ses $i",
                                isSelected = group.isTrackSelected(i)
                            )
                        )
                    }
                    audioGroupIdx++
                }
                C.TRACK_TYPE_TEXT -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        subtitles.add(
                            TrackInfo(
                                groupIndex = subGroupIdx,
                                trackIndex = i,
                                language = format.language ?: "Bilinmeyen",
                                label = format.label ?: format.language ?: "Altyazı $i",
                                isSelected = group.isTrackSelected(i)
                            )
                        )
                    }
                    subGroupIdx++
                }
            }
        }
        _audioTracks.value = audio
        _subtitleTracks.value = subtitles
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        trackSelector = null
        _isPlaying.value = false
    }
}
